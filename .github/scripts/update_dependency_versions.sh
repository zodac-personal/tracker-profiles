#!/bin/bash
# ------------------------------------------------------------------------------
# Script Name:     update_dependency_versions.sh
#
# Description:     Updates version-pinned package declarations in both a Dockerfile
#                  and a Python requirements.txt file. Fetches the latest versions
#                  of Python, Debian, Ubuntu, and Python pip packages, then rewrites the
#                  Dockerfile and requirements files with those values.
#
# Usage:           ./update_dependency_versions.sh <path_to_Dockerfile> <path_to_requirements.txt>
#
# Requirements:
#   - bash, awk, grep, jq, curl
#   - Dockerfile must contain specific marker comments for package installs:
#       # BEGIN DEBIAN PACKAGES / # END DEBIAN PACKAGES
#       # BEGIN UBUNTU PACKAGES / # END UBUNTU PACKAGES
#   - requirements.txt should contain lines in the format: `package>=version` or `package==version`
#   - Internet access to fetch latest versions from PyPI and apt
#
# Behavior:
#   - For the Dockerfile:
#       - Updates Debian packages (defined in the Dockerfile) with the latest candidate versions from apt
#       - Updates Ubuntu packages (defined in the Dockerfile) with the latest candidate versions from apt
#       - Rewrites the install blocks in the Dockerfile between respective markers
#   - For requirements.txt:
#       - Updates packages defined with `>=` to their latest PyPI versions
#       - Leaves `==` pinned packages untouched
#       - Overwrites the original requirements.txt with the updated values
#   - For pom.xml:
#       - Runs `mvn versions:update-properties`
#
# Exit Codes:
#   - 0: Success
#   - 1: Failure due to missing markers or failed version fetches
# ------------------------------------------------------------------------------

set -euo pipefail

DEBIAN_DOCKER_IMAGE_VERSION="13.3"
UBUNTU_DOCKER_IMAGE_VERSION="24.04"

get_pypi_version() {
    version=$(curl -fsSL "https://pypi.org/pypi/${1}/json" | jq -r '.info.version // empty')
    if [[ -z "$version" ]]; then
        echo "⚠️ Could not fetch PyPI version for ${1}" >&2
        return 1
    fi
    echo "$version"
}

update_requirements() {
    for requirements in "$@"; do
        name=$(basename "${requirements}")

        echo
        echo "🔍 Fetching ${name} versions from PyPI..."
        while IFS= read -r line; do
            # Match 'package>=version'
            if [[ $line =~ ^([a-zA-Z0-9._-]+)'>='([0-9a-zA-Z._-]+)$ ]]; then
                package="${BASH_REMATCH[1]}"
                if ! latest_version=$(get_pypi_version "${package}"); then
                    echo "  ⚠️ Skipping ${package} (keeping existing version)"
                    echo "${line}" >>"${requirements}.tmp"
                    continue
                fi
                echo "  ${package}=${latest_version}"
                echo "${package}>=${latest_version}" >>"${requirements}.tmp"

            # Match 'package==version'
            elif [[ $line =~ ^([a-zA-Z0-9._-]+)'=='([0-9a-zA-Z._-]+)$ ]]; then
                package="${BASH_REMATCH[1]}"
                pinned_version="${BASH_REMATCH[2]}"
                echo "  ${package}=${pinned_version} (pinned)"
                echo "${line}" >>"${requirements}.tmp"
            else
                echo "${line}" >>"${requirements}.tmp"
            fi
        done <"${requirements}"

        # Overwrite the original file
        mv "${requirements}.tmp" "${requirements}"
        echo "✅ ${requirements#./} updated successfully with latest packages"
    done
}

update_debian_packages() {
    dockerfile="${1}"

    DEBIAN_START_MARKER="# BEGIN DEBIAN PACKAGES"
    DEBIAN_END_MARKER="# END DEBIAN PACKAGES"

    if ! grep -q "${DEBIAN_START_MARKER}" "${dockerfile}" || ! grep -q "${DEBIAN_END_MARKER}" "${dockerfile}"; then
        echo "❌ Could not find Debian marker lines in ${dockerfile}"
        return 1
    fi

    section_count=$(grep -c "${DEBIAN_START_MARKER}" "${dockerfile}")

    echo
    echo "🔍 Fetching latest dockerfile Debian package versions..."
    # Pull latest debian image
    docker pull "debian:${DEBIAN_DOCKER_IMAGE_VERSION}-slim" >/dev/null

    for ((section = 1; section <= section_count; section++)); do
        echo "[Section ${section}/${section_count}]"

        # Extract only the Nth section's package block
        package_block=$(awk -v start="${DEBIAN_START_MARKER}" -v end="${DEBIAN_END_MARKER}" -v n="${section}" '
            $0 ~ start { count++; if (count == n) in_block = 1 }
            in_block
            $0 ~ end && in_block { in_block = 0 }
        ' "${dockerfile}")

        # Extract the package names before '=' using regex
        mapfile -t package_names < <(echo "${package_block}" | grep -oP '^\s*[a-z0-9.+-]+(?==)' | sed 's/^[[:space:]]*//')

        if [[ "${#package_names[@]}" -eq 0 ]]; then
            echo "❌ No package names found in section ${section}"
            rm -f debian_block.txt "${dockerfile}.tmp"
            return 1
        fi

        unset debian_versions
        declare -A debian_versions

        # Single container: update apt once, then query all packages together
        versions_raw=$(docker run --rm "debian:${DEBIAN_DOCKER_IMAGE_VERSION}-slim" sh -c \
            "apt-get update -qq 2>/dev/null && apt-cache policy ${package_names[*]}")

        while IFS='=' read -r pkg ver; do
            [[ -n "${pkg}" && -n "${ver}" ]] && debian_versions["${pkg}"]="${ver}"
        done < <(echo "${versions_raw}" | awk '
            /^[a-z0-9]/ { pkg=$1; sub(/:$/, "", pkg) }
            /Candidate:/  { print pkg "=" $2 }
        ')

        for package in "${package_names[@]}"; do
            version="${debian_versions["${package}"]:-}"
            if [[ -z "${version}" ]]; then
                echo "❌ Failed to get version for: ${package}"
                rm -f debian_block.txt "${dockerfile}.tmp"
                return 1
            fi
            echo "  ${package}=${version}"
        done

        # Build the updated install block for this section
        {
            echo "${DEBIAN_START_MARKER}"
            echo "RUN apt-get update && \\"
            echo "    apt-get install -yqq --no-install-recommends \\"
            for package in "${package_names[@]}"; do
                echo "        ${package}=\"${debian_versions[${package}]}\" \\"
            done
            echo "    && \\"
            echo "    apt-get autoremove && \\"
            echo "    apt-get clean && \\"
            echo "    rm -rf /var/lib/apt/lists/*"
            echo "${DEBIAN_END_MARKER}"
        } >debian_block.txt

        # Replace the Nth occurrence of the block with the new one
        awk -v start_marker="${DEBIAN_START_MARKER}" \
            -v end_marker="${DEBIAN_END_MARKER}" \
            -v target_section="${section}" '
        BEGIN {
            while ((getline line < "debian_block.txt") > 0) {
                block = block line ORS
            }
            close("debian_block.txt")
            sub(/\n$/, "", block)
        }
        $0 ~ start_marker {
            section_count++
            if (section_count == target_section) {
                print block
                in_target = 1
                next
            }
        }
        $0 ~ end_marker && in_target { in_target = 0; next }
        !in_target { print }
        ' "${dockerfile}" >"${dockerfile}.tmp"

        mv "${dockerfile}.tmp" "${dockerfile}"
    done

    rm -f debian_block.txt

    echo "✅ ${dockerfile#./} updated successfully with latest Debian packages"
}

update_ubuntu_packages() {
    dockerfile="${1}"

    UBUNTU_START_MARKER="# BEGIN UBUNTU PACKAGES"
    UBUNTU_END_MARKER="# END UBUNTU PACKAGES"

    if ! grep -q "${UBUNTU_START_MARKER}" "${dockerfile}" || ! grep -q "${UBUNTU_END_MARKER}" "${dockerfile}"; then
        echo "⚠️ No Ubuntu marker lines found in ${dockerfile}, skipping"
        return 0
    fi

    section_count=$(grep -c "${UBUNTU_START_MARKER}" "${dockerfile}")

    echo
    echo "🔍 Fetching latest dockerfile Ubuntu package versions..."
    # Pull latest ubuntu image
    docker pull "ubuntu:${UBUNTU_DOCKER_IMAGE_VERSION}" >/dev/null

    for ((section = 1; section <= section_count; section++)); do
        echo "[Section ${section}/${section_count}]"

        # Extract only the Nth section's package block
        package_block=$(awk -v start="${UBUNTU_START_MARKER}" -v end="${UBUNTU_END_MARKER}" -v n="${section}" '
            $0 ~ start { count++; if (count == n) in_block = 1 }
            in_block
            $0 ~ end && in_block { in_block = 0 }
        ' "${dockerfile}")

        # Extract the package names before '=' using regex
        mapfile -t package_names < <(echo "${package_block}" | grep -oP '^\s*[a-z0-9.+-]+(?==)' | sed 's/^[[:space:]]*//')

        if [[ "${#package_names[@]}" -eq 0 ]]; then
            echo "❌ No package names found in section ${section}"
            rm -f ubuntu_block.txt "${dockerfile}.tmp"
            return 1
        fi

        unset ubuntu_versions
        declare -A ubuntu_versions

        # Single container: update apt once, then query all packages together
        versions_raw=$(docker run --rm "ubuntu:${UBUNTU_DOCKER_IMAGE_VERSION}" sh -c \
            "apt-get update -qq 2>/dev/null && apt-cache policy ${package_names[*]}")

        while IFS='=' read -r pkg ver; do
            [[ -n "${pkg}" && -n "${ver}" ]] && ubuntu_versions["${pkg}"]="${ver}"
        done < <(echo "${versions_raw}" | awk '
            /^[a-z0-9]/ { pkg=$1; sub(/:$/, "", pkg) }
            /Candidate:/  { print pkg "=" $2 }
        ')

        for package in "${package_names[@]}"; do
            version="${ubuntu_versions["${package}"]:-}"
            if [[ -z "${version}" ]]; then
                echo "❌ Failed to get version for: ${package}"
                rm -f ubuntu_block.txt "${dockerfile}.tmp"
                return 1
            fi
            echo "  ${package}=${version}"
        done

        # Build the updated install block for this section
        {
            echo "${UBUNTU_START_MARKER}"
            echo "RUN apt-get update && \\"
            echo "    apt-get install -yqq --no-install-recommends \\"
            for package in "${package_names[@]}"; do
                echo "        ${package}=\"${ubuntu_versions[${package}]}\" \\"
            done
            echo "    && \\"
            echo "    apt-get autoremove && \\"
            echo "    apt-get clean && \\"
            echo "    rm -rf /var/lib/apt/lists/*"
            echo "${UBUNTU_END_MARKER}"
        } >ubuntu_block.txt

        # Replace the Nth occurrence of the block with the new one
        awk -v start_marker="${UBUNTU_START_MARKER}" \
            -v end_marker="${UBUNTU_END_MARKER}" \
            -v target_section="${section}" '
        BEGIN {
            while ((getline line < "ubuntu_block.txt") > 0) {
                block = block line ORS
            }
            close("ubuntu_block.txt")
            sub(/\n$/, "", block)
        }
        $0 ~ start_marker {
            section_count++
            if (section_count == target_section) {
                print block
                in_target = 1
                next
            }
        }
        $0 ~ end_marker && in_target { in_target = 0; next }
        !in_target { print }
        ' "${dockerfile}" >"${dockerfile}.tmp"

        mv "${dockerfile}.tmp" "${dockerfile}"
    done

    rm -f ubuntu_block.txt

    echo "✅ ${dockerfile#./} updated successfully with latest Ubuntu packages"
}

update_java_version() {
    local dockerfile="${1}"
    local workflows_dir=".github/workflows"
    local lint_script=".github/scripts/lint_and_tests.sh"
    local pom_xml="./pom.xml"

    echo
    echo "🔍 Fetching latest Java version..."

    local available_releases
    available_releases=$(curl -fsSL "https://api.adoptium.net/v3/info/available_releases")
    local latest_major
    latest_major=$(echo "${available_releases}" | jq -r '.most_recent_feature_release // empty')
    if [[ -z "${latest_major}" ]]; then
        echo "⚠️ Could not fetch latest Java version from Adoptium" >&2
        return 1
    fi

    echo "  Latest Java major version: ${latest_major}"
    echo "  Fetching full release details for Java ${latest_major}..."

    local release_info
    release_info=$(curl -fsSL \
        "https://api.adoptium.net/v3/assets/latest/${latest_major}/hotspot?architecture=x64&image_type=jdk&os=linux")
    local release_name
    release_name=$(echo "${release_info}" | jq -r '.[0].release_name // empty')
    if [[ -z "${release_name}" ]]; then
        echo "⚠️ Could not fetch release details for Java ${latest_major} from Adoptium" >&2
        return 1
    fi

    # Convert "jdk-26.0.0+7" → "26.0.0_7-jdk"
    local docker_tag
    docker_tag="$(echo "${release_name#jdk-}" | sed 's/+/_/')-jdk"

    echo "  Docker tag: eclipse-temurin:${docker_tag}"
    echo "  Verifying Docker image eclipse-temurin:${docker_tag} exists on Docker Hub..."

    local http_status
    http_status=$(curl -fsSL -o /dev/null -w "%{http_code}" \
        "https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/${docker_tag}")
    if [[ "${http_status}" != "200" ]]; then
        echo "⚠️ Docker image eclipse-temurin:${docker_tag} not found on Docker Hub (HTTP ${http_status}), skipping update"
        return 0
    fi
    echo "  ✅ Docker image eclipse-temurin:${docker_tag} confirmed on Docker Hub"

    # Dockerfile (both FROM lines — jdk_builder and java_app_builder)
    sed -i "s|FROM eclipse-temurin:[^ ]*-jdk|FROM eclipse-temurin:${docker_tag}|g" "${dockerfile}"

    # lint_and_tests.sh
    sed -i "s|JDK_DOCKER_IMAGE=\"eclipse-temurin:[^\"]*-jdk\"|JDK_DOCKER_IMAGE=\"eclipse-temurin:${docker_tag}\"|" "${lint_script}"

    # pom.xml (major version only)
    sed -i "s|<java-release>[0-9]*</java-release>|<java-release>${latest_major}</java-release>|" "${pom_xml}"

    # All workflow files (major version only)
    if [[ -d "${workflows_dir}" ]]; then
        for workflow in "${workflows_dir}"/*.yml; do
            if grep -q "java-version:" "${workflow}"; then
                sed -i "s|java-version: '[0-9]*'|java-version: '${latest_major}'|g" "${workflow}"
            fi
        done
    fi

    echo "✅ Java updated to ${latest_major} (${docker_tag}) in pom.xml, Dockerfile, lint_and_tests.sh, and workflows"
}

update_pom_versions() {
    mvn versions:update-properties
    echo "✅ pom.xml updated successfully with latest Maven packages"
}

update_maven_version() {
    local dockerfile="${1}"
    local workflows_dir=".github/workflows"
    local lint_script=".github/scripts/lint_and_tests.sh"
    local pom_xml="./pom.xml"

    echo
    echo "🔍 Fetching latest Maven version..."

    local curl_args=(-fsSL)
    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
        curl_args+=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
    fi

    local latest_version
    latest_version=$(curl "${curl_args[@]}" "https://api.github.com/repos/apache/maven/releases/latest" | jq -r '.tag_name // empty')
    if [[ -z "${latest_version}" ]]; then
        echo "⚠️ Could not fetch latest Maven version from GitHub" >&2
        return 1
    fi
    latest_version="${latest_version#maven-}"

    echo "  Latest Maven version: ${latest_version}"
    echo "  Verifying Docker image maven:${latest_version} exists on Docker Hub..."

    local http_status
    http_status=$(curl -fsSL -o /dev/null -w "%{http_code}" \
        "https://hub.docker.com/v2/repositories/library/maven/tags/${latest_version}")
    if [[ "${http_status}" != "200" ]]; then
        echo "⚠️ Docker image maven:${latest_version} not found on Docker Hub (HTTP ${http_status}), skipping update"
        return 0
    fi
    echo "  ✅ Docker image maven:${latest_version} confirmed on Docker Hub"

    # pom.xml
    sed -i "s|<maven-release>[^<]*</maven-release>|<maven-release>${latest_version}</maven-release>|" "${pom_xml}"

    # Dockerfile
    sed -i "s|FROM maven:[^ ]* AS maven_base|FROM maven:${latest_version} AS maven_base|" "${dockerfile}"

    # lint_and_tests.sh
    sed -i "s|MAVEN_DOCKER_IMAGE=\"maven:[^\"]*\"|MAVEN_DOCKER_IMAGE=\"maven:${latest_version}\"|" "${lint_script}"

    # All workflow files
    if [[ -d "${workflows_dir}" ]]; then
        for workflow in "${workflows_dir}"/*.yml; do
            if grep -q "maven-version:" "${workflow}"; then
                sed -i "s|maven-version: '[0-9.]*'|maven-version: '${latest_version}'|g" "${workflow}"
            fi
        done
    fi

    echo "✅ Maven updated to ${latest_version} in pom.xml, Dockerfile, lint_and_tests.sh, and workflows"
}

get_github_action_version() {
    local action="${1}"
    local curl_args=(-fsSL)

    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
        curl_args+=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
    fi

    local version
    version=$(curl "${curl_args[@]}" "https://api.github.com/repos/${action}/releases/latest" | jq -r '.tag_name // empty')
    if [[ -z "${version}" ]]; then
        echo "⚠️ Could not fetch GitHub release version for ${action}" >&2
        return 1
    fi
    echo "${version}"
}

update_github_actions() {
    local workflows_dir=".github/workflows"

    if [[ ! -d "${workflows_dir}" ]]; then
        echo "⚠️ No workflows directory found at ${workflows_dir}, skipping"
        return 0
    fi

    echo
    echo "🔍 Fetching latest GitHub Action versions..."

    # Collect unique 'owner/repo@version' references from all workflow files
    mapfile -t action_refs < <(
        grep -rh 'uses:' "${workflows_dir}"/*.yml \
            | grep -oP 'uses:\s+\K[a-zA-Z0-9._-]+/[a-zA-Z0-9._-]+@\S+' \
            | sort -u
    )

    if [[ "${#action_refs[@]}" -eq 0 ]]; then
        echo "  No action references found in ${workflows_dir}"
        return 0
    fi

    for ref in "${action_refs[@]}"; do
        action="${ref%@*}"
        current_version="${ref#*@}"

        if ! latest_version=$(get_github_action_version "${action}"); then
            continue
        fi

        if [[ "${current_version}" == "${latest_version}" ]]; then
            echo "  ${action}=${latest_version} (already up-to-date)"
        else
            echo "  ${action}: ${current_version} → ${latest_version}"
            for workflow in "${workflows_dir}"/*.yml; do
                sed -i "s|${action}@${current_version}|${action}@${latest_version}|g" "${workflow}"
            done
        fi
    done

    echo "✅ ${workflows_dir} updated successfully with latest GitHub Actions"
}

# Default paths assume the script is being run from the root of the project
dockerfile="${1:-./docker/Dockerfile}"
requirements="${2:-./docker/config/requirements.txt}"

if [[ ! -f "${dockerfile}" ]]; then
    echo "❌ Dockerfile not found: ${dockerfile}"
    exit 1
fi
if [[ ! -f "${requirements}" ]]; then
    echo "❌ Python requirements.txt not found: ${requirements}"
    exit 1
fi

update_debian_packages "${dockerfile}"      || echo "⚠️ Debian packages update failed, continuing..."
update_ubuntu_packages "${dockerfile}"      || echo "⚠️ Ubuntu packages update failed, continuing..."
update_requirements "${requirements}"       || echo "⚠️ Python requirements update failed, continuing..."
update_pom_versions                         || echo "⚠️ Maven versions update failed, continuing..."
update_maven_version "${dockerfile}"        || echo "⚠️ Maven version update failed, continuing..."
update_java_version "${dockerfile}"         || echo "⚠️ Java version update failed, continuing..."
update_github_actions                       || echo "⚠️ GitHub Actions update failed, continuing..."
