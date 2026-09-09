#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
case "${1:-}" in
  a) app=app-a ;;
  b1) app=app-b; export INSTANCE=b1 HTTP_PORT=8081 ;;
  b2) app=app-b; export INSTANCE=b2 HTTP_PORT=8082 ;;
  *) echo 'Usage: bash scripts/run.sh a|b1|b2'; exit 1 ;;
esac
if [[ "$app" == app-b ]]; then
  mkdir -p ".runtime/tx-$INSTANCE"
  export TX_DIRECTORY=".runtime/tx-$INSTANCE"
fi
exec java -jar "$app/target/quarkus-app/quarkus-run.jar"
