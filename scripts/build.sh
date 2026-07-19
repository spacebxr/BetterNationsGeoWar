#!/usr/bin/env bash
# Local build helper: pins JDK 21 and runs the Gradle wrapper offline.
set -euo pipefail
export JAVA_HOME="C:/Users/leeno/jdk21/jdk-21.0.11+10"
cd "$(dirname "$0")/.."
./gradlew.bat "${@:-build}" --offline
