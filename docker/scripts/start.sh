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
#   - tracker-profiles.jar available at /app/tracker-profiles.jar
#   - SELENIUM_REMOTE_URL pointing to a running Selenium Grid or standalone node
#
# Exit Codes:
#   - 0: Success (server stopped cleanly)
#   - 1: Java application signalled failure
# ------------------------------------------------------------------------------

set -eu

exec java \
  -Xms"${JAVA_XMS:-128m}" -Xmx"${JAVA_XMX:-512m}" \
  -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders \
  -XX:+UseG1GC -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45 \
  -Djava.util.logging.config.file=/app/logging.properties \
  -jar /app/tracker-profiles.jar
