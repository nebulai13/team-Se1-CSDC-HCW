#!/bin/bash
# LibSearch Launcher Script for Linux/macOS
# Version: 1.0.0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# JAR file location (Gradle builds to build/libs/)
JAR_FILE="$SCRIPT_DIR/build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}LibSearch JAR not found. Building...${NC}"

    # Use gradlew.mac on macOS, gradlew on Linux
    if [[ "$OSTYPE" == "darwin"* ]] && [ -f "$SCRIPT_DIR/gradlew.mac" ]; then
        GRADLE_CMD="$SCRIPT_DIR/gradlew.mac"
        # gradlew.mac runs 'gradlew run' by default, we need shadowJar instead
        chmod +x "$SCRIPT_DIR/gradlew"
        export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home}"
        "$SCRIPT_DIR/gradlew" shadowJar
    else
        chmod +x "$SCRIPT_DIR/gradlew"
        "$SCRIPT_DIR/gradlew" shadowJar
    fi

    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}Error: Build failed. JAR file not found at:${NC}"
        echo "$JAR_FILE"
        echo ""
        echo "Please build the project manually:"
        echo "  ./gradlew shadowJar"
        exit 1
    fi
    echo -e "${GREEN}Build successful!${NC}"
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F'.' '{print $1}')

if [ -z "$JAVA_VERSION" ]; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    echo "LibSearch requires Java 21 or later"
    echo "Download from: https://adoptium.net/"
    exit 1
fi

if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${YELLOW}Warning: Java $JAVA_VERSION detected${NC}"
    echo "LibSearch requires Java 21 or later"
    echo "Current version may not work correctly"
    echo ""
fi

# Run LibSearch
if [ $# -eq 0 ]; then
    echo -e "${GREEN}Starting LibSearch GUI...${NC}"
    java -jar "$JAR_FILE"
else
    # CLI mode with arguments
    java -jar "$JAR_FILE" "$@"
fi

exit $?
