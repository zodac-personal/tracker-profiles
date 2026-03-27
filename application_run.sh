#!/bin/sh
# Launches the docker-compose stack.
# If TRACKER_EXECUTION_ORDER contains 'MANUAL' (case-insensitive), the chrome container is started
# first and numlock is set in the virtual display. A background process waits on a FIFO; the Java
# app writes to it just before the first MANUAL tracker runs, which triggers VNC to open then.
# The VNC browser window is automatically closed when the stack exits.

set -eu

COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"
VNC_URL="http://localhost:7900/?autoconnect=1"

# Use the env var if set, otherwise fall back to the application default
ORDER="${TRACKER_EXECUTION_ORDER:-HEADLESS,MANUAL}"

POLLER_PID=""
SIGNAL_PIPE="/tmp/tracker-vnc-signal"
BROWSER_DATA_DIR=""

# Resolve the host Chrome binary once at startup
CHROME_CMD=""
for _cmd in google-chrome chromium chromium-browser; do
    if command -v "${_cmd}" > /dev/null 2>&1; then
        CHROME_CMD="${_cmd}"
        break
    fi
done

cleanup() {
    if [ -n "${BROWSER_DATA_DIR}" ]; then
        pkill -f "${BROWSER_DATA_DIR}" 2>/dev/null || true
        sleep 1
        rm -rf "${BROWSER_DATA_DIR}"
    fi
    if [ -n "${POLLER_PID}" ]; then
        kill "${POLLER_PID}" 2>/dev/null || true
    fi
    rm -f "${SIGNAL_PIPE}"
    docker compose -f "${COMPOSE_FILE}" down
}
trap cleanup EXIT

launch_chrome() {
    "${CHROME_CMD}" --new-window --user-data-dir="${BROWSER_DATA_DIR}" \
        --no-first-run --no-default-browser-check --start-maximized \
        "${VNC_URL}" > /dev/null 2>&1 &
}

open_vnc() {
    if [ -n "${CHROME_CMD}" ]; then
        launch_chrome
    elif command -v firefox > /dev/null 2>&1; then
        firefox --new-window --profile "${BROWSER_DATA_DIR}" "${VNC_URL}" > /dev/null 2>&1 &
    elif command -v open > /dev/null 2>&1; then
        # macOS: synchronous, no PID to track
        open -na "Google Chrome" --args --new-window --user-data-dir="${BROWSER_DATA_DIR}" \
            --no-first-run --no-default-browser-check --start-maximized \
            "${VNC_URL}" > /dev/null 2>&1 || open "${VNC_URL}"
    elif command -v cmd.exe > /dev/null 2>&1; then
        # Windows (Git Bash / WSL): synchronous, no PID to track
        cmd.exe /c start "${VNC_URL}"
    else
        printf 'Could not auto-open browser. Open manually: %s\n' "${VNC_URL}"
    fi
}

# Build all images upfront so VNC does not open while the app image is still building
docker compose -f "${COMPOSE_FILE}" build

if echo "${ORDER}" | grep -qi "manual"; then
    # Create a FIFO so the Java app can signal the host when a MANUAL tracker is about to run
    rm -f "${SIGNAL_PIPE}"
    mkfifo "${SIGNAL_PIPE}"

    # Create temp browser data dir here so cleanup() can pkill by path after the subshell exits
    BROWSER_DATA_DIR="$(mktemp -d)"

    # Background: block on the FIFO and open VNC only when Java writes to it
    ( read -r _ < "${SIGNAL_PIPE}" && open_vnc ) &
    POLLER_PID=$!

    # Start chrome in isolation first so numlock can be set before the app connects
    docker compose -f "${COMPOSE_FILE}" up -d tracker-profiles-chrome

    # Wait for the healthcheck to pass
    printf 'Waiting for Chrome to be ready'
    CHROME_ID="$(docker compose -f "${COMPOSE_FILE}" ps -q tracker-profiles-chrome)"
    until [ "$(docker inspect --format='{{.State.Health.Status}}' "${CHROME_ID}")" = "healthy" ]; do
        printf '.'
        sleep 2
    done
    printf '\r\033[K'

    # Set numlock ON in the virtual display so it matches the expected state inside VNC
    docker exec "${CHROME_ID}" numlockx on 2>/dev/null || true
fi

# Start the full stack. If chrome is already running it is reused; tracker-profiles starts fresh
docker compose -f "${COMPOSE_FILE}" up --exit-code-from tracker-profiles
