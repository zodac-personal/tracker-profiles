#!/bin/bash
# ------------------------------------------------------------------------------
# Script Name:     lint_and_tests.sh
#
# Description:     Lints and tests the project using Docker.
#
# Usage:           ./lint_and_tests.sh [steps]
#
#                  [steps] is an optional comma-separated list of steps to run.
#                  If omitted, all steps are run.
#
#                  Valid steps:
#                    docker      - Lint the Dockerfile with hadolint
#                    javascript  - Lint JavaScript files with eslint
#                    markdown    - Lint Markdown files with markdownlint-cli2
#                    java        - Run Java lints and tests with Maven
#
#                  Examples:
#                    ./lint_and_tests.sh
#                    ./lint_and_tests.sh docker
#                    ./lint_and_tests.sh docker,javascript
#
# Requirements:
#   - Docker must be installed and available on the system PATH
#
# Exit Codes:
#   - 0: All linting and tests passed successfully
#   - Non-zero: One or more linting errors or test failures occurred
# ------------------------------------------------------------------------------

# TODO: Only run a lint if there has been a change from master? Might need to tag a KGB?

set -uo pipefail

trap 'echo; echo "❌ Interrupted"; exit 130' INT

ESLINT_BUILD_IMAGE="local/tracker-profiles-eslint:latest"
ESLINT_NODE_IMAGE="node:25.9.0-alpine"
HADOLINT_DOCKER_IMAGE="hadolint/hadolint:v2.14.0-alpine"
JAVA_BUILD_IMAGE="local/tracker-profiles-builder:latest"
JDK_DOCKER_IMAGE="eclipse-temurin:26_35-jdk"
MARKDOWNLINT_DOCKER_IMAGE="davidanson/markdownlint-cli2:v0.22.1"
MAVEN_DOCKER_IMAGE="maven:3.9.15"

VALID_STEPS=("docker" "javascript" "markdown" "java")

overall_exit_code=0

run_docker() {
    echo
    echo "Running Dockerfile lint using [${HADOLINT_DOCKER_IMAGE}]"
    docker pull "${HADOLINT_DOCKER_IMAGE}" >/dev/null
    if output=$(docker run --rm \
        -v "${PWD}":/app \
        -w /app \
        "${HADOLINT_DOCKER_IMAGE}" \
        hadolint --config ci/docker/.hadolint.yaml docker/Dockerfile 2>&1); then
        echo "✅ Dockerfile lint passed"
    else
        echo "${output}"
        echo "❌ Dockerfile lint failed"
        overall_exit_code=1
    fi
}

run_javascript() {
    echo
    echo "Running JavaScript lint using [${ESLINT_NODE_IMAGE}]"
    docker pull "${ESLINT_NODE_IMAGE}" >/dev/null
    build_output=$(
        docker build -t "${ESLINT_BUILD_IMAGE}" - 2>&1 <<EOF
FROM ${ESLINT_NODE_IMAGE}
RUN npm install -g eslint@9
EOF
    )
    # shellcheck disable=SC2181
    if [[ $? -ne 0 ]]; then
        echo "❌ JavaScript image build failed"
        echo "${build_output}"
        overall_exit_code=1
        return
    fi
    if output=$(docker run --rm \
        -v "${PWD}":/app \
        -w /app \
        "${ESLINT_BUILD_IMAGE}" \
        eslint --config ci/javascript/eslint.config.cjs \
        "tracker-profiles-screenshots/src/main/resources/net/zodac/tracker/redaction/*.js" 2>&1); then
        echo "✅ JavaScript lint passed"
    else
        echo "${output}"
        echo "❌ JavaScript lint failed"
        overall_exit_code=1
    fi
}

run_markdown() {
    echo
    echo "Running Markdown lint using [${MARKDOWNLINT_DOCKER_IMAGE}]"
    docker pull "${MARKDOWNLINT_DOCKER_IMAGE}" >/dev/null
    if output=$(docker run --rm \
        -v "${PWD}":/app \
        -w /app \
        "${MARKDOWNLINT_DOCKER_IMAGE}" \
        --config ci/doc/.markdownlint.json \
        "**/*.md" "!RELEASE_NOTES.md" "!tracker-profiles-screenshots/target/**" 2>&1); then
        echo "✅ Markdown lint passed"
    else
        echo "${output}"
        echo "❌ Markdown lint failed"
        overall_exit_code=1
    fi
}

run_java() {
    echo
    echo "Running Java lints and tests using [${MAVEN_DOCKER_IMAGE}] + [${JDK_DOCKER_IMAGE}]"
    docker pull "${MAVEN_DOCKER_IMAGE}" >/dev/null
    docker pull "${JDK_DOCKER_IMAGE}" >/dev/null
    build_output=$(
        docker build -t "${JAVA_BUILD_IMAGE}" - 2>&1 <<EOF
FROM ${MAVEN_DOCKER_IMAGE} AS maven_base
FROM ${JDK_DOCKER_IMAGE}
COPY --from=maven_base /usr/share/maven /usr/share/maven
ENV M2_HOME="/usr/share/maven"
ENV MAVEN_HOME="/usr/share/maven"
ENV PATH="\${M2_HOME}/bin:\${PATH}"
EOF
    )
    # shellcheck disable=SC2181
    if [[ $? -ne 0 ]]; then
        echo "❌ Java image build failed"
        echo "${build_output}"
        overall_exit_code=1
        return
    fi
    if output=$(docker run --rm \
        -u "$(id -u):$(id -g)" \
        -v "${PWD}":/app \
        -v "${HOME}/.m2":/var/maven/.m2 \
        -w /app \
        --entrypoint mvn \
        "${JAVA_BUILD_IMAGE}" \
        -Duser.home=/var/maven clean verify -Dall 2>&1); then
        echo "✅ Java lints and tests passed"
    else
        echo "${output}"
        echo "❌ Java lints and tests failed"
        overall_exit_code=1
    fi
}

# Parse and validate steps
if [[ $# -eq 0 ]]; then
    steps=("${VALID_STEPS[@]}")
else
    IFS=',' read -ra steps <<<"${1}"
    for step in "${steps[@]}"; do
        pattern=" ${step} "
        if [[ ! " ${VALID_STEPS[*]} " =~ $pattern ]]; then
            echo "❌ Unknown step: '${step}'. Valid steps: $(
                IFS=', '
                echo "${VALID_STEPS[*]}"
            )"
            exit 1
        fi
    done
fi

# Execute steps
for step in "${steps[@]}"; do
    case "${step}" in
    docker) run_docker ;;
    javascript) run_javascript ;;
    markdown) run_markdown ;;
    java) run_java ;;
    esac
done

if [[ "${overall_exit_code}" -ne 0 ]]; then
    echo
    echo "❌ One or more steps failed"
    exit 1
fi
