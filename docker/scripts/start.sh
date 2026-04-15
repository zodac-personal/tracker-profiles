#!/bin/sh
# ------------------------------------------------------------------------------
# Script Name:     start.sh
#
# Description:     Launches a headless instance of a web browser and runs a Java
#                  application (`tracker-profiles.jar`) that performs screenshot
#                  capture.
#
# Usage:           ./start.sh
#
# Requirements:
#   - Chromium Browser installed
#   - Java installed and available on the system PATH
#   - `tracker-profiles.jar` available at /app/tracker-profiles.jar
#   - X display server running and accessible at DISPLAY=:0
#
# Behavior:
#   - Starts the web browser
#   - Executes the Java JAR file
#   - On SIGINT (Ctrl+C), gracefully terminates the browser and Java PID (if started)
#
# Exit Codes:
#   - 0: Success
#   - 1: Java application signaled failure (e.g., screenshots not captured)
#   - 130: Script terminated via SIGINT (manual interruption)
# ------------------------------------------------------------------------------

set -eu

main() {
    chromium --display=:0 >/dev/null 2>&1 &
    BROWSER_PID=$!

    java \
      -Xms"${JAVA_XMS:-128m}" -Xmx"${JAVA_XMX:-512m}" \
      -XX:+UseG1GC -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45 \
      -jar /app/tracker-profiles.jar &
    JAVA_PID=$!

    wait "${JAVA_PID}"
}

cleanup() {
    printf '\n\033[33mCleaning up...\033[0m\n'

    # Stop Java process
    if [ -n "${JAVA_PID:-}" ]; then
        kill -TERM "${JAVA_PID}" 2>/dev/null || true
        wait "${JAVA_PID}" 2>/dev/null || true
    fi

    # Stop browser
    if [ -n "${BROWSER_PID:-}" ]; then
        kill -TERM "${BROWSER_PID:-}" 2>/dev/null || true
        wait "${BROWSER_PID:-}" 2>/dev/null || true
    fi

    exit 130
}

trap cleanup INT
main
