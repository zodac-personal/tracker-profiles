#!/bin/bash
# ------------------------------------------------------------------------------
# Script Name:     lint_and_tests.sh
#
# Description:     Lints and tests the project using Docker.
#
# Usage:           ./lint_and_tests.sh
#
# Requirements:
#   - Docker must be installed and available on the system PATH
#
# Behavior:
#   - Runs hadolint to lint the Dockerfile (config: ci/docker/.hadolint.yaml)
#   - Runs markdownlint-cli2 to lint all Markdown files (config: ci/doc/.markdownlint.json)
#   - Runs mvn to execute Java lints and tests
#
# Exit Codes:
#   - 0: All linting and tests passed successfully
#   - Non-zero: One or more linting errors or test failures occurred
# ------------------------------------------------------------------------------

set -uo pipefail

HADOLINT_DOCKER_IMAGE="hadolint/hadolint:v2.14.0-alpine"
JDK_DOCKER_IMAGE="eclipse-temurin:26_35-jdk"
JAVA_BUILD_IMAGE="local/tracker-profiles-builder:latest"
MARKDOWNLINT_DOCKER_IMAGE="davidanson/markdownlint-cli2:v0.22.0"
MAVEN_DOCKER_IMAGE="maven:3.9.14"

overall_exit_code=0

echo
echo "🐳 Running Dockerfile lint using [${HADOLINT_DOCKER_IMAGE}]"
docker pull "${HADOLINT_DOCKER_IMAGE}" >/dev/null
docker run --rm \
    -v "${PWD}":/app \
    -w /app \
    "${HADOLINT_DOCKER_IMAGE}" \
    hadolint --config ci/docker/.hadolint.yaml docker/Dockerfile \
    || { echo "❌ Dockerfile lint failed"; overall_exit_code=1; }

echo
echo "🐳 Running Markdown lint using [${MARKDOWNLINT_DOCKER_IMAGE}]"
docker pull "${MARKDOWNLINT_DOCKER_IMAGE}" >/dev/null
docker run --rm \
    -v "${PWD}":/app \
    -w /app \
    "${MARKDOWNLINT_DOCKER_IMAGE}" \
    --config ci/doc/.markdownlint.json \
    "**/*.md" "!RELEASE_NOTES.md" "!tracker-profiles-screenshots/target/**" \
    || { echo "❌ Markdown lint failed"; overall_exit_code=1; }

echo
echo "🐳 Running Java lints and tests using [${MAVEN_DOCKER_IMAGE}] + [${JDK_DOCKER_IMAGE}]"
docker pull "${MAVEN_DOCKER_IMAGE}" >/dev/null
docker pull "${JDK_DOCKER_IMAGE}" >/dev/null
docker build -t "${JAVA_BUILD_IMAGE}" - <<EOF
FROM ${MAVEN_DOCKER_IMAGE} AS maven_base
FROM ${JDK_DOCKER_IMAGE}
COPY --from=maven_base /usr/share/maven /usr/share/maven
ENV M2_HOME="/usr/share/maven"
ENV MAVEN_HOME="/usr/share/maven"
ENV PATH="\${M2_HOME}/bin:\${PATH}"
EOF
docker run --rm -t \
    -u "$(id -u):$(id -g)" \
    -v "${PWD}":/app \
    -v "${HOME}/.m2":/var/maven/.m2 \
    -w /app \
    --entrypoint mvn \
    "${JAVA_BUILD_IMAGE}" \
    -Duser.home=/var/maven clean verify -Dall \
    || { echo "❌ Java lints and tests failed"; overall_exit_code=1; }

if [[ "${overall_exit_code}" -ne 0 ]]; then
    echo
    echo "❌ One or more steps failed"
    exit 1
fi
