#!/bin/bash

echo "Running full build with tests and lints"
if ! .github/scripts/lint_and_tests.sh; then
  echo "Pre-commit build failed, commit aborted"
  exit 1
fi

exit 0
