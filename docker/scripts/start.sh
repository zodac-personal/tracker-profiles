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
#   - Browser (Chromium or Firefox) installed
#   - Java installed and available on the system PATH
#   - `tracker-profiles.jar` available at /app/tracker-profiles.jar
#   - X display server running and accessible at DISPLAY=:0
#
# Behavior:
#   - Starts the web browser
#   - Executes the Java JAR file
#   - Outputs a colored success or error message based on Java's exit code
#   - Tracks and prints total execution time in a natural format
#   - On SIGINT (Ctrl+C), gracefully terminates the browser and Java PID (if started)
#
# Exit Codes:
#   - 0: Success
#   - 1: Java application signaled failure (e.g., screenshots not captured)
#   - 130: Script terminated via SIGINT (manual interruption)
# ------------------------------------------------------------------------------

set -eu

main() {
    start_time=$(date +%s)

    chromium --display=:0 >/dev/null 2>&1 &
    BROWSER_PID=$!

    java \
      -Xms"${JAVA_XMS:-128m}" -Xmx"${JAVA_XMX:-512m}" \
      -XX:+UseG1GC -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45 \
      -jar /app/tracker-profiles.jar &
    JAVA_PID=$!

    if wait "${JAVA_PID}"; then
        printf '\033[32mScreenshots complete in %s\033[0m\n' "$(get_execution_time "${start_time}")"
    else
        printf '\033[31mTook screenshots with errors in %s, review logs\033[0m\n' "$(get_execution_time "${start_time}")"
        exit 1
    fi
}

get_execution_time() {
    start_time="${1}"
    end_time=$(date +%s)
    elapsed_time=$((end_time - start_time))
    _convert_to_natural_time "${elapsed_time}"
}

_convert_to_natural_time() {
    elapsed_time="${1}"
    if [ "${elapsed_time}" -lt 60 ]; then
        echo "${elapsed_time}s"
    elif [ "${elapsed_time}" -lt 3600 ]; then
        elapsed_m=$((elapsed_time / 60))
        elapsed_s=$((elapsed_time % 60))
        printf "%dm:%02ds\n" "${elapsed_m}" "${elapsed_s}"
    else
        elapsed_h=$((elapsed_time / 3600))
        elapsed_m=$(((elapsed_time % 3600) / 60))
        elapsed_s=$((elapsed_time % 60))
        printf "%dh:%02dm:%02ds\n" "${elapsed_h}" "${elapsed_m}" "${elapsed_s}"
    fi
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
