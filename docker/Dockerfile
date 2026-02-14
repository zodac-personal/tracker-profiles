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

# Stage 2: Install Python
FROM python:3.14.3-slim AS python_builder

# BEGIN PYTHON PACKAGES
RUN python3 -m ensurepip && \
    pip install \
        pip=="26.0.1" \
        wheel=="0.46.3"
# END PYTHON PACKAGES

# Copy Python requirements
COPY ./python/requirements.txt .

# Set up Python virtual environment and install dependencies
RUN python3 -m venv /app/venv && \
    /app/venv/bin/pip install --upgrade pip && \
    /app/venv/bin/pip install -r ./requirements.txt

# Stage 3: Install java
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

# Stage 4: Prepare scripts
FROM debian:13.3-slim AS scripts_builder
COPY ./docker/scripts/start.sh /start.sh
RUN chmod +x /start.sh

# Stage 5: Shell binaries
FROM busybox:1.37.0-musl AS shell_builder

# Runtime
FROM debian:13.3-slim
# Set the working directory
WORKDIR /app

# Install required applications using 'tracker-profiles/.github/scripts/update_dependency_versions.sh'
# - ca-certificates (SSL)
# - chromium (web browser)
# BEGIN DEBIAN PACKAGES
RUN apt-get update && \
    apt-get install -yqq --no-install-recommends \
        ca-certificates="20250419" \
        chromium="144.0.7559.109-2~deb13u1" \
    && \
    apt-get autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
# END DEBIAN PACKAGES

# Downloading the chromedriver for the installed version of chromium and installing it in the Docker image
# Doing this to avoid undetected-chromedriver from downloading a version itself, which means older version aren't able to be used once obsolete
RUN apt-get update && apt-get install -yqq --no-install-recommends curl unzip && \
    mkdir -p /usr/local/chromium && \
    curl -fL \
      "https://storage.googleapis.com/chrome-for-testing-public/$(chromium --version | awk '{print $2}')/linux64/chromedriver-linux64.zip" \
      -o /tmp/chromedriver.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/chromium && rm -f /tmp/chromedriver.zip && \
    apt-get purge -yqq curl unzip && apt-get autoremove -yqq --purge && apt-get clean && rm -rf /var/lib/apt/lists/* && \
    chmod -R 0777 /usr/local/chromium

# Copy Python installation
COPY --from=python_builder /usr/local /usr/local
COPY --from=python_builder /app/venv /app/venv
COPY ./python/selenium_manager/ ./selenium_manager/
ENV PATH="/usr/local/bin:${PATH}"
ENV PATH="/app/venv/bin:/usr/local/bin:${PATH}"

# Copy JDK & JAR
COPY --from=java_builder /opt/jdk /opt/jdk
ENV JAVA_HOME="/opt/jdk"
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=maven_builder /app/tracker-profiles-screenshots/target/tracker-profiles-screenshots-*.jar tracker-profiles.jar

# Copy scripts
COPY --from=scripts_builder /start.sh /app/start.sh
COPY --from=shell_builder /bin/busybox /bin/wget

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=10s \
            --retries=3 \
            CMD [ "wget", "--no-verbose", "--tries=1", "--spider", "http://127.0.0.1:5000/ping" ]

# Define non-root user UID and GID
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

# Suppress 'dbind-WARNING' error logs
ENV NO_AT_BRIDGE=1

# Start application
CMD ["/app/start.sh"]
# Debugging
#ENTRYPOINT ["tail", "-f", "/dev/null"]
