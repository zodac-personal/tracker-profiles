#!/bin/bash
# ------------------------------------------------------------------------------
# Script Name:     java_lint_and_tests.sh
#
# Description:     Lints and tests the Java code using Docker.
#
# Usage:           ./java_lint_and_tests.sh
#
# Requirements:
#   - Docker must be installed and available on the system PATH
#   - If run in GitHub Actions, the `CI=true` environment variable must be set
#
# Behavior:
#   - 'mvn clean install -Dall' to execute lints and tests
#
# Exit Codes:
#   - 0: All linting and tests passed successfully
#   - Non-zero: One or more linting errors or test failures occurred
# ------------------------------------------------------------------------------

set -euo pipefail

JAVA_DOCKER_IMAGE="maven:3.9.12-eclipse-temurin-25-alpine"

echo
echo "🐳 Running Java build using [${JAVA_DOCKER_IMAGE}]"

docker pull "${JAVA_DOCKER_IMAGE}" >/dev/null && \
docker run --rm -t \
    -v "${PWD}":/app \
    -v "${HOME}/.m2":/root/.m2 \
    -w /app \
    "${JAVA_DOCKER_IMAGE}" \
    mvn verify -Dall
