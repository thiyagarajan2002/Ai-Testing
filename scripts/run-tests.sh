#!/usr/bin/env bash
# Builds the project and runs the unit test suite.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec mvn -f "$ROOT/pom.xml" clean test "$@"
