#!/usr/bin/env bash
set -euo pipefail

VERSION="${1}"
PREV_TAG="${2}"

: "${CHANGELOG_CONTENT:?CHANGELOG_CONTENT is required}"

{
  echo "body<<EOF"

  # Prepend RELEASE_NOTES if present and non-empty
  if [ -s RELEASE_NOTES ]; then
    echo "Including RELEASE_NOTES content..." >&2
    cat RELEASE_NOTES
    echo ""
    echo "---"
    echo ""
  fi

  cat <<EOF2
Docker image pushed to Docker Hub:
[docker pull zodac/tracker-profiles:${VERSION}](https://hub.docker.com/r/zodac/tracker-profiles/tags)

## Changes since ${PREV_TAG}:

${CHANGELOG_CONTENT}
EOF2

  echo "EOF"
} >> "${GITHUB_OUTPUT}"
