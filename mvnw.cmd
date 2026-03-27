@REM Maven Wrapper script for Windows
@REM Downloads Maven automatically if not present

@echo off
setlocal

set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven 3.9.9...
    if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
    set "TEMP_FILE=%TEMP%\maven-download.zip"
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%TEMP%\maven-download.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven-download.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
    del "%TEMP%\maven-download.zip" 2>nul
    echo Maven 3.9.9 installed.
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
