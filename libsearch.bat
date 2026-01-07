@echo off
REM LibSearch Launcher Script for Windows
REM Version: 1.0.0

setlocal enabledelayedexpansion

REM Get script directory
set SCRIPT_DIR=%~dp0

REM JAR file location
set JAR_FILE=%SCRIPT_DIR%target\team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar

REM Check if JAR exists
if not exist "%JAR_FILE%" (
    echo Error: LibSearch JAR file not found at:
    echo %JAR_FILE%
    echo.
    echo Please build the project first:
    echo   mvn clean install
    exit /b 1
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
    if defined JAVAFX_HOME (
        java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml -jar "%JAR_FILE%"
    ) else (
        java -jar "%JAR_FILE%"
    )
) else (
    REM CLI mode
    java -jar "%JAR_FILE%" %*
)

exit /b %errorlevel%
