#!/bin/bash

echo "Running full build with tests and lints"
if ! mvn clean install -Dall; then
  echo "Pre-commit build failed, commit aborted"
  exit 1
fi

exit 0
