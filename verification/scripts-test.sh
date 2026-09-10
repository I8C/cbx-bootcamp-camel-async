#!/usr/bin/env bash
# Script checks use a disposable copy and fake commands, never the real containers.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tools
root=$(pwd -P)
testdir=$(mktemp -d "$root/.tools/script-test.XXXXXX")
trap 'case "$testdir" in "$root"/.tools/script-test.*) rm -rf -- "$testdir" ;; esac' EXIT
mkdir -p "$testdir/bin" "$testdir/scripts" "$testdir/.mvn"
cp scripts/*.sh "$testdir/scripts/"
cp -R checkpoints "$testdir/checkpoints"
touch "$testdir/pom.xml"
for app in app-a app-b; do
  mkdir -p "$testdir/$app/src/main/java/workshop" "$testdir/$app/src/main/resources"
  touch "$testdir/$app/pom.xml"
done
export CAPTURE="$testdir/args"
cat > "$testdir/bin/docker" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$@" > "$CAPTURE"
SH
cat > "$testdir/bin/java" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$@" "${INSTANCE:-}" "${HTTP_PORT:-}" "${TX_DIRECTORY:-}" > "$CAPTURE"
SH
chmod +x "$testdir/bin/"*
export PATH="$testdir/bin:$PATH"
cd "$testdir"

for command in up stop down version; do
  bash scripts/containers.sh "$command"
  printf '%s\n' compose -f compose.yaml "$command" > expected
  diff -u expected "$CAPTURE"
done
bash scripts/containers.sh up -d
printf '%s\n' compose -f compose.yaml up -d > expected
diff -u expected "$CAPTURE"
bash scripts/containers.sh logs --since '1 hour ago' artemis
printf '%s\n' compose -f compose.yaml logs --since '1 hour ago' artemis > expected
diff -u expected "$CAPTURE"
bash scripts/containers.sh > help
grep -q 'Usage:' help

for step in starter queue topic; do
  printf 'YES\n' | bash scripts/checkpoint.sh "$step"
  cmp "checkpoints/$step/SendRoute.java.txt" app-a/src/main/java/workshop/SendRoute.java
  cmp "checkpoints/$step/StockRoute.java.txt" app-b/src/main/java/workshop/StockRoute.java
  cmp "checkpoints/$step/app-a.properties" app-a/src/main/resources/application.properties
  cmp "checkpoints/$step/app-b.properties" app-b/src/main/resources/application.properties
  if [[ "$step" == starter ]]; then
    grep -q 'TODO 1: send to JMS' app-a/src/main/java/workshop/SendRoute.java
    grep -q 'TODO 2: update stock' app-b/src/main/java/workshop/StockRoute.java
  fi
done

if bash scripts/run.sh a > result 2>&1; then echo 'Missing build was accepted'; exit 1; fi
grep -q 'bash mvnw clean package' result
for app in app-a app-b; do
  find "$app/src" "$app/pom.xml" pom.xml .mvn -exec touch -t 202001010000 {} +
  mkdir -p "$app/target/quarkus-app/lib"
  touch "$app/target/quarkus-app/quarkus-run.jar"
done
bash scripts/run.sh b2
printf '%s\n' -jar app-b/target/quarkus-app/quarkus-run.jar b2 8082 .runtime/tx-b2 > expected
diff -u expected "$CAPTURE"
touch -t 203001010000 app-b/src/main/resources/application.properties
if bash scripts/run.sh b2 > result 2>&1; then echo 'Outdated build was accepted'; exit 1; fi
grep -q 'bash mvnw clean package' result
if bash scripts/run.sh unknown > result 2>&1; then echo 'Invalid app was accepted'; exit 1; fi
echo 'PASS: Compose arguments, checkpoints, missing/stale packages and B2 launch settings.'
