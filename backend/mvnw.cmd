@REM Maven Wrapper for Windows
@REM Downloads Maven automatically if not installed

@echo off
setlocal

set MAVEN_VERSION=3.9.6
set MAVEN_DIR=%USERPROFILE%\.m2\wrapper\maven-%MAVEN_VERSION%
set MAVEN_ZIP=%MAVEN_DIR%-bin.zip
set MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
set MVN_CMD=%MAVEN_DIR%\bin\mvn.cmd

if exist "%MVN_CMD%" goto runMaven

echo [INFO] Maven %MAVEN_VERSION% not found. Downloading...
if not exist "%MAVEN_DIR%" mkdir "%MAVEN_DIR%"

powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%USERPROFILE%\.m2\wrapper\' -Force"
move "%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%" "%MAVEN_DIR%"
del "%MAVEN_ZIP%"
echo [INFO] Maven downloaded successfully.

:runMaven
set JAVA_HOME=%JAVA_HOME%
"%MVN_CMD%" %*
