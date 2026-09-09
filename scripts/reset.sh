#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo 'Reset deletes only this workshop broker/database data and XA logs.'
echo 'First stop App A, B1 and B2 with Ctrl+C in their terminals.'
read -r -p 'Type RESET after stopping all three applications: ' answer
[[ "$answer" == RESET ]] || exit 1
bash scripts/containers.sh down -v
# Refuse symlinks that could lead outside this checkout before deleting logs.
root=$(pwd -P)
for path in .runtime/tx-b1 .runtime/tx-b2 .runtime/test-tx-b1 .runtime/test-tx-b2; do
  resolved=$(realpath -m "$path")
  case "$resolved" in
    "$root"/.runtime/tx-b1|"$root"/.runtime/tx-b2|"$root"/.runtime/test-tx-b1|"$root"/.runtime/test-tx-b2) rm -rf -- "$resolved" ;;
    *) echo "Unsafe log path: $resolved"; exit 1 ;;
  esac
done
bash scripts/containers.sh up -d
echo 'Wait for the broker to start, then restart the apps. Both stocks start at 100.'
