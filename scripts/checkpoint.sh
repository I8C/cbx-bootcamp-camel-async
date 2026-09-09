#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
case "${1:-}" in starter|queue|topic) checkpoint=$1 ;; *) echo 'Usage: bash scripts/checkpoint.sh starter|queue|topic'; exit 1 ;; esac
echo 'Stop the apps first. This replaces your two route files and two property files.'
read -r -p "Load $checkpoint? Type YES: " answer
[[ "$answer" == YES ]] || exit 1
cp "checkpoints/$checkpoint/SendRoute.java.txt" app-a/src/main/java/workshop/SendRoute.java
cp "checkpoints/$checkpoint/StockRoute.java.txt" app-b/src/main/java/workshop/StockRoute.java
cp "checkpoints/$checkpoint/app-a.properties" app-a/src/main/resources/application.properties
cp "checkpoints/$checkpoint/app-b.properties" app-b/src/main/resources/application.properties
echo "Loaded $checkpoint. Now run: bash mvnw package"
