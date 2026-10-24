#!/usr/bin/env bash
# Runs Gradle, keeps the full log, and on failure turns the useful lines into GitHub annotations
# so the reason is visible on the run page without opening the raw log.
# Usage: run-gradle.sh <label> <gradle args...>
set -uo pipefail
label="$1"; shift
log="${RUNNER_TEMP:-/tmp}/gradle-${label// /-}.log"
./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ]; then
  {
    grep -E '^(e: |.*FAILED$|.*AssertionError|.*Assertion|.*Exception|> )' "$log" | head -30
  } | while IFS= read -r line; do
    echo "::error title=${label}::${line//$'\r'/}"
  done
fi
exit "$status"
