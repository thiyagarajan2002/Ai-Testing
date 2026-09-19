#!/usr/bin/env bash
# Packages the runnable jar and executes a collection.
#
#   ./scripts/run-demo.sh                          built-in demo plan
#   ./scripts/run-demo.sh -c samples/bruno         a Bruno collection
#   ./scripts/run-demo.sh -c api.json --parallel   any other options
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mvn -q -f "$ROOT/pom.xml" -DskipTests package
exec java -jar "$ROOT/target/ai-api-testing.jar" "$@"
