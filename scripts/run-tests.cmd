@echo off
rem Builds the project and runs the unit test suite.
setlocal
set ROOT=%~dp0..
call mvn -f "%ROOT%\pom.xml" clean test %*
exit /b %ERRORLEVEL%
