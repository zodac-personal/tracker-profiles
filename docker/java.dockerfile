# Stage 1: Build the Java artefact
FROM maven:3.9.12-eclipse-temurin-25-alpine AS maven_builder

# Set the working directory
WORKDIR /app

# Copy the pom.xml files and build dependencies (to cache them)
COPY pom.xml .
COPY tracker-profiles-screenshots/pom.xml ./tracker-profiles-screenshots/pom.xml
RUN mvn dependency:go-offline

# Copy source code and build the project, with dependencies cached
COPY tracker-profiles-screenshots/src ./tracker-profiles-screenshots/src
RUN mvn clean install

# Stage 2: Create minimal JDK
FROM eclipse-temurin:25.0.2_10-jdk AS java_builder

# Copy JAR so we can extract jdeps dependencies
COPY --from=maven_builder /app/tracker-profiles-screenshots/target/tracker-profiles-screenshots-*.jar /tmp/tracker-profiles.jar

RUN apt-get update && apt-get install -yqq --no-install-recommends binutils && \
        jlink --compress=zip-9 \
        --no-header-files \
        --no-man-pages \
        --strip-debug \
        --add-modules $(jdeps --multi-release BASE --print-module-deps --ignore-missing-deps /tmp/tracker-profiles.jar) \
        --output "/opt/jdk" && \
    strip -p --strip-unneeded "/opt/jdk/lib/server/libjvm.so" && \
    find /opt/jdk/bin -type f -exec strip -p --strip-unneeded {} \; || true

# Runtime
FROM debian:13.3-slim AS runtime
# Set the working directory
WORKDIR /app

# Install required applications using 'tracker-profiles/.github/scripts/update_dependency_versions.sh'
# - Fonts for Java Swing rendering for ther UI dialogs
#   - fontconfig
#   - fonts-dejavu-core
# - X11 libraries for Java AWT on Linux
#   - libx11-6
#   - libxext6
#   - libxi6
#   - libxrender1
#   - libxtst6
# BEGIN DEBIAN PACKAGES
RUN apt-get update && \
    apt-get install -yqq --no-install-recommends \
        fontconfig="2.15.0-2.3" \
        libx11-6="2:1.8.12-1" \
        libxext6="2:1.3.4-1+b3" \
        libxi6="2:1.8.2-1" \
        libxrender1="1:0.9.12-1" \
        libxtst6="2:1.2.5-1" \
    && \
    apt-get autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
# END DEBIAN PACKAGES

# Copy Google Chrome extensions
COPY ./docker/browser_extensions/ublock_origin_lite_ddkjiahejlhfcafbddmgiahcphecmpfh_2026_2_22.crx /app/ublock_origin_lite.crx

# Copy JDK & JAR
COPY --from=java_builder /opt/jdk /opt/jdk
ENV JAVA_HOME="/opt/jdk"
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=maven_builder /app/tracker-profiles-screenshots/target/tracker-profiles-screenshots-*.jar tracker-profiles.jar

# Copy application script
COPY ./docker/scripts/start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Configure nonroot user
ENV PUID=1000
ENV PGID=1000
# Create user and group
RUN groupadd -g ${PGID} nonroot && \
    useradd -u ${PUID} -g nonroot -d /tmp/chrome-home -m nonroot
# Ensure the home directory has proper ownership
RUN chown -R nonroot:nonroot /tmp/chrome-home
# Set environment and user
ENV HOME=/tmp/chrome-home
USER nonroot
VOLUME /tmp/chrome-home

# Start application
CMD ["/app/start.sh"]
# Debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]
