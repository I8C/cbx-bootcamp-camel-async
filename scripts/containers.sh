#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -eq 0 || "$1" == --help || "$1" == -h ]]; then
  echo 'Usage: bash scripts/containers.sh <compose-command> [options]'
  echo 'Examples: up -d | stop | down | version | pull | ps | logs --tail 20 artemis'
  echo 'Uses Docker by default. For Podman: export CONTAINER_ENGINE=podman'
  exit 0
fi
engine=${CONTAINER_ENGINE:-docker}
case "$engine" in docker|podman) ;; *) echo 'CONTAINER_ENGINE must be docker or podman'; exit 1;; esac
# Keep Git Bash from rewriting container paths in command arguments.
export MSYS_NO_PATHCONV=1
exec "$engine" compose -f compose.yaml "$@"
