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

docker compose run --rm integration-test \
    xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24" \
    node seatbelt-e2e/launch.js --local "$@"
