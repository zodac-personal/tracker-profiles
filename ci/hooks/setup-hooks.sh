#!/bin/bash
# Commit message format
cp ./ci/hooks/commit-msg.sh .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg

# Pre-push build validation
cp ./ci/hooks/pre-push.sh .git/hooks/pre-push
chmod +x .git/hooks/pre-push

echo "Git hooks installed!"
