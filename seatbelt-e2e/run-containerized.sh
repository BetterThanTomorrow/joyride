#!/bin/bash

# Containerized integration test runner, the default for `npm run integration-test`
# Runs `launch.js --local` on a virtual display, passing on the arguments.
# The tests run against out/, which the host's watcher keeps current. The container doesn't compile.

set -e

if ! command -v docker > /dev/null; then
    echo "Docker not found. To run the integration tests on this machine instead: npm run integration-test -- --local" >&2
    exit 1
fi

cd "$(dirname "$0")"
JOYRIDE_REPO="$(cd .. && pwd)"
export JOYRIDE_REPO

# A debug build installs the registry home's bb scripts from a vscode-mcp checkout next to
# this one, when there is one, so mount it where the extension looks.
MOUNTS=()
if [ -d "$JOYRIDE_REPO/../vscode-mcp" ]; then
    VSCODE_MCP_REPO="$(cd "$JOYRIDE_REPO/../vscode-mcp" && pwd)"
    MOUNTS=(-v "$VSCODE_MCP_REPO:$VSCODE_MCP_REPO:ro")
fi

docker compose run --rm "${MOUNTS[@]}" integration-test \
    xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24" \
    node seatbelt-e2e/launch.js --local "$@"
