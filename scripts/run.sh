#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ $# -eq 1 ]] || { echo 'Usage: bash scripts/run.sh a|b1|b2'; exit 1; }
case "${1:-}" in
  a) app=app-a ;;
  b1) app=app-b; export INSTANCE=b1 HTTP_PORT=8081 ;;
  b2) app=app-b; export INSTANCE=b2 HTTP_PORT=8082 ;;
  *) echo 'Usage: bash scripts/run.sh a|b1|b2'; exit 1 ;;
esac
jar="$app/target/quarkus-app/quarkus-run.jar"
if [[ ! -f "$jar" || ! -d "$app/target/quarkus-app/lib" ]]; then
  echo 'No packaged application. First run from the workshop folder: bash mvnw clean package'
  exit 1
fi
if [[ -n "$(find "$app/src" "$app/pom.xml" pom.xml .mvn -newer "$jar" -print -quit)" ]]; then
  echo 'Sources or configuration changed. Rebuild first: bash mvnw clean package'
  exit 1
fi
if [[ "$app" == app-b ]]; then
  mkdir -p ".runtime/tx-$INSTANCE"
  export TX_DIRECTORY=".runtime/tx-$INSTANCE"
fi
exec java -jar "$jar"
