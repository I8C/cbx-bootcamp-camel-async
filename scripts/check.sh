#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version=$(java -version 2>&1)
echo "$version"
[[ "$version" == *'version "21'* ]] || { echo 'Select JDK 21 in JAVA_HOME and PATH first.'; exit 1; }
git --version
bash mvnw -version
bash scripts/containers.sh version
docker info >/dev/null
echo 'Tools found. Use Java 21 and Docker Desktop. Build and pull images before the workshop.'
