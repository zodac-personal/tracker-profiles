#!/bin/bash

echo "Running full build (tests + lints) before push"
if ! mvn clean install -Dall; then
  echo "Pre-push build failed, push aborted"
  exit 1
fi

exit 0
