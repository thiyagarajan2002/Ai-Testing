@echo off
rem Packages the runnable jar and executes a collection.
setlocal
set ROOT=%~dp0..
call mvn -q -f "%ROOT%\pom.xml" -DskipTests package || exit /b %ERRORLEVEL%
java -jar "%ROOT%\target\ai-api-testing.jar" %*
exit /b %ERRORLEVEL%
