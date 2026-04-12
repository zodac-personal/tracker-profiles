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
                latest_version=$(get_pypi_version "${package}")
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
        exit 1
    fi

    section_count=$(grep -c "${DEBIAN_START_MARKER}" "${dockerfile}")

    echo
    echo "🔍 Fetching latest dockerfile Debian package versions..."
    # Pull latest debian image
    docker pull "debian:${DEBIAN_DOCKER_IMAGE_VERSION}-slim" >/dev/null

    get_debian_version() {
        docker run --rm "debian:${DEBIAN_DOCKER_IMAGE_VERSION}-slim" sh -c "apt-get update -qq 2>/dev/null && apt-cache policy ${1}" | awk '/Candidate:/ { print $2 }'
    }

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
            exit 1
        fi

        unset debian_versions
        declare -A debian_versions

        for package in "${package_names[@]}"; do
            version=$(get_debian_version "${package}")
            if [[ -z "${version}" ]]; then
                echo "❌ Failed to get version for: ${package}"
                exit 1
            fi
            debian_versions["${package}"]="${version}"
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

    get_ubuntu_version() {
        docker run --rm "ubuntu:${UBUNTU_DOCKER_IMAGE_VERSION}" sh -c "apt-get update -qq 2>/dev/null && apt-cache policy ${1}" | awk '/Candidate:/ { print $2 }'
    }

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
            exit 1
        fi

        unset ubuntu_versions
        declare -A ubuntu_versions

        for package in "${package_names[@]}"; do
            version=$(get_ubuntu_version "${package}")
            if [[ -z "${version}" ]]; then
                echo "❌ Failed to get version for: ${package}"
                exit 1
            fi
            ubuntu_versions["${package}"]="${version}"
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

update_pom_versions() {
    local JAVA_DOCKER_IMAGE="maven:3.9.12-eclipse-temurin-25-alpine"

    docker pull "${JAVA_DOCKER_IMAGE}" >/dev/null &&
        docker run --rm -t \
            -v "${PWD}":/app \
            -v "${HOME}/.m2":/root/.m2 \
            -w /app \
            "${JAVA_DOCKER_IMAGE}" \
            mvn versions:update-properties

    echo "✅ pom.xml updated successfully with latest Maven packages"
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

update_debian_packages "${dockerfile}"
update_ubuntu_packages "${dockerfile}"
update_requirements "${requirements}"
update_pom_versions
