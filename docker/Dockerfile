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

# Stage 3: Patch chromedriver
FROM python:3.14.3-slim-trixie AS chromedriver_builder

# Install required applications using 'tracker-profiles/.github/scripts/update_dependency_versions.sh'
# - ca-certificates (SSL)
# - chromium (web browser)
# BEGIN DEBIAN PACKAGES
RUN apt-get update && \
    apt-get install -yqq --no-install-recommends \
        ca-certificates="20250419" \
        chromium="146.0.7680.177-1~deb13u1" \
    && \
    apt-get autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
# END DEBIAN PACKAGES

# Copy Python requirements
COPY ./docker/config/requirements.txt .
RUN python3 -m pip install --upgrade pip && python3 -m pip install -r ./requirements.txt

# Download latest chromedriver for chromium verison then patch with undetected-chromedriver
COPY ./docker/scripts/patch_chromedriver.py /usr/local/bin/patch_chromedriver.py
RUN apt-get update && apt-get install -yqq --no-install-recommends curl unzip && \
    mkdir -p /usr/local/chromium && \
    chromium_major_version=$(chromium --version | awk '{print $2}' | cut -d. -f1) && \
    chromedriver_version=$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${chromium_major_version}") && \
    curl -fL "https://storage.googleapis.com/chrome-for-testing-public/${chromedriver_version}/linux64/chromedriver-linux64.zip" -o /tmp/chromedriver.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/chromium && rm -f /tmp/chromedriver.zip && \
    apt-get purge -yqq curl unzip && apt-get autoremove -yqq --purge && apt-get clean && rm -rf /var/lib/apt/lists/* && \
    chmod -R 0755 /usr/local/chromium && \
    chmod +x /usr/local/bin/patch_chromedriver.py && \
    python3 /usr/local/bin/patch_chromedriver.py

# Runtime
FROM debian:13.3-slim AS runtime
# Set the working directory
WORKDIR /app

# Suppress 'dbind-WARNING' error logs
ENV NO_AT_BRIDGE=1

# Install required applications using 'tracker-profiles/.github/scripts/update_dependency_versions.sh'
# - ca-certificates (SSL)
# - chromium (web browser)
# - fonts-arphic-ukai (Chinese fonts)
# - fonts-ipafont (Japanese fonts)
# - wmctrl (force browser to be active window)
# BEGIN DEBIAN PACKAGES
RUN apt-get update && \
    apt-get install -yqq --no-install-recommends \
        ca-certificates="20250419" \
        chromium="146.0.7680.177-1~deb13u1" \
        fonts-arphic-ukai="0.2.20080216.2-5" \
        fonts-ipafont="00303-23" \
        wmctrl="1.07+git20240228.1105759-1" \
    && \
    apt-get autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
# END DEBIAN PACKAGES

# Copy Google Chrome extensions
COPY ./docker/browser_extensions/ublock_origin_lite_ddkjiahejlhfcafbddmgiahcphecmpfh_2026_2_22.crx /app/ublock_origin_lite.crx

# Copy patched chromedriver
COPY --from=chromedriver_builder /usr/local/chromium/chromedriver-linux64/chromedriver /usr/local/chromium/chromedriver-linux64/chromedriver

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
