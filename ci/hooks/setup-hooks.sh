#!/bin/bash
# Commit message format
cp ./ci/hooks/commit-msg.sh .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg

echo "Git hooks installed!"
