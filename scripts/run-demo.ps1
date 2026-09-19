# Packages the runnable jar and executes a collection.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
mvn -q -f "$root/pom.xml" -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -jar "$root/target/ai-api-testing.jar" @args
exit $LASTEXITCODE
