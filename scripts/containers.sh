#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
engine=${CONTAINER_ENGINE:-docker}
case "$engine" in docker|podman) ;; *) echo 'CONTAINER_ENGINE must be docker or podman'; exit 1;; esac
# Keep Git Bash from rewriting container paths in command arguments.
export MSYS_NO_PATHCONV=1
exec "$engine" compose -f compose.yaml "$@"
