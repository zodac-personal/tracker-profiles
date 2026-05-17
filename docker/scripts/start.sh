#!/bin/sh
# ------------------------------------------------------------------------------
# Script Name:     start.sh
#
# Description:     Runs the tracker-profiles web server. The server exposes a UI
#                  on port 8080 and delegates screenshot execution to a remote
#                  Selenium browser configured via SELENIUM_REMOTE_URL.
#
# Usage:           ./start.sh
#
# Requirements:
#   - Java installed and available on the system PATH
#   - `tracker-profiles.jar` available at /app/tracker-profiles.jar
#   - SELENIUM_REMOTE_URL pointing to a running Selenium Grid or standalone node
#
# Environment Variables:
#   - JAVA_ADDITIONAL_OPTS: Additional JVM options appended after whichever options are in effect
#   - JAVA_OPTS:            Replaces all default JVM options (defaults are used if unset or empty)
#
# Exit Codes:
#   - 0: Success (server stopped cleanly)
#   - 1: Java application signalled failure
# ------------------------------------------------------------------------------

set -eu

DEFAULT_JAVA_OPTS="-Xms128m -Xmx512m \
  -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders \
  -XX:+UseG1GC -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:SharedArchiveFile=/app/app.jsa -Xshare:auto \
  -Djava.util.logging.config.file=/app/logging.properties"

# SC2086: intentional word splitting to pass JVM flags as separate arguments
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-${DEFAULT_JAVA_OPTS}} ${JAVA_ADDITIONAL_OPTS:-} \
  -jar /app/tracker-profiles.jar
