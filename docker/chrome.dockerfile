# Stage 1: Extract the bundled chromedriver from the selenium image.
# Using the same base as the runtime ensures the chromedriver version always matches Chrome.
FROM selenium/standalone-chrome:latest AS chromedriver_source
RUN cp "$(which chromedriver)" /tmp/chromedriver

# Stage 2: Patch chromedriver with undetected-chromedriver
FROM python:3.14.3-slim-trixie AS chromedriver_patcher

COPY --from=chromedriver_source /tmp/chromedriver /tmp/chromedriver

# Copy Python requirements
COPY ./docker/config/requirements.txt .
RUN python3 -m pip install --upgrade pip && python3 -m pip install -r ./requirements.txt

COPY ./docker/scripts/patch_chromedriver.py /usr/local/bin/patch_chromedriver.py
RUN chmod +x /usr/local/bin/patch_chromedriver.py && \
    python3 /usr/local/bin/patch_chromedriver.py /tmp/chromedriver

# Runtime: official Selenium standalone-chrome image with patched chromedriver and fonts
FROM selenium/standalone-chrome:latest

USER root

# Install required applications
# Note: selenium/standalone-chrome is Ubuntu-based; package versions differ from Debian and are not pinned here
# - fonts-arphic-ukai (Chinese fonts)
# - fonts-ipafont (Japanese fonts)
# - numlockx (set numlock state in the virtual display at startup)
RUN apt-get update && \
    apt-get install -yqq --no-install-recommends \
        fonts-arphic-ukai \
        fonts-ipafont \
        numlockx \
    && \
    apt-get autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Ensure /tmp/.X11-unix has sticky+world-writable permissions so Xvfb can create sockets
# and the app container's non-root user can connect to them via the shared x11_socket volume
RUN mkdir -p /tmp/.X11-unix && chmod 1777 /tmp/.X11-unix

# Replace the bundled chromedriver with the patched version
COPY --from=chromedriver_patcher /tmp/chromedriver /usr/local/bin/chromedriver

USER seluser
