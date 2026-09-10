#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -eq 0 || "$1" == --help || "$1" == -h ]]; then
  echo 'Usage: bash scripts/containers.sh <compose-command> [options]'
  echo 'Examples: up -d | stop | down | version | pull | ps | logs --tail 20 artemis'
  echo 'Uses Docker Desktop.'
  exit 0
fi
# Keep Git Bash from rewriting container paths in command arguments.
export MSYS_NO_PATHCONV=1
exec docker compose -f compose.yaml "$@"
