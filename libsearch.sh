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

# JAR file location
JAR_FILE="$SCRIPT_DIR/target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Error: LibSearch JAR file not found at:${NC}"
    echo "$JAR_FILE"
    echo ""
    echo "Please build the project first:"
    echo "  mvn clean install"
    exit 1
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

# Check for JavaFX (if running GUI mode)
if [ $# -eq 0 ]; then
    echo -e "${GREEN}Starting LibSearch GUI...${NC}"
    java --module-path "$JAVAFX_HOME/lib" \
         --add-modules javafx.controls,javafx.fxml \
         -jar "$JAR_FILE"
else
    # CLI mode - no JavaFX needed
    java -jar "$JAR_FILE" "$@"
fi

exit $?
