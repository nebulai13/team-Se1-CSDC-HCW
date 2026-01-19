@echo off
REM LibSearch Launcher Script for Windows
REM Version: 1.0.0

setlocal enabledelayedexpansion

REM Get script directory
set SCRIPT_DIR=%~dp0

REM JAR file location (Gradle builds to build/libs/)
set JAR_FILE=%SCRIPT_DIR%build\libs\team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

REM Check if JAR exists, if not build it
if not exist "%JAR_FILE%" (
    echo LibSearch JAR not found. Building...
    call "%SCRIPT_DIR%gradlew.bat" shadowJar
    if not exist "%JAR_FILE%" (
        echo Error: Build failed. JAR file not found at:
        echo %JAR_FILE%
        echo.
        echo Please build the project manually:
        echo   gradlew shadowJar
        exit /b 1
    )
    echo Build successful!
)

REM Check Java version
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    echo LibSearch requires Java 21 or later
    echo Download from: https://adoptium.net/
    exit /b 1
)

REM Run LibSearch
if "%~1"=="" (
    echo Starting LibSearch GUI...
    java -jar "%JAR_FILE%"
) else (
    REM CLI mode with arguments
    java -jar "%JAR_FILE%" %*
)

exit /b %errorlevel%
