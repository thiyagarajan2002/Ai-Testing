# Builds the project and runs the unit test suite.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
mvn -f "$root/pom.xml" clean test @args
exit $LASTEXITCODE
