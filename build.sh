#!/bin/bash

# BitStrike C2 Framework build script
# This script builds both the Java server and Windows agent

# Get the directory of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
RESET="\033[0m"

# Print colored text
function print_color() {
    echo -e "${1}${2}${RESET}"
}

# Check prerequisites
function check_prereqs() {
    print_color "$BLUE" "Checking prerequisites..."
    
    # Check for Java
    if ! command -v java &> /dev/null; then
        print_color "$RED" "Error: Java not found."
        print_color "$YELLOW" "Please install Java 11 or higher."
        exit 1
    fi
    
    # Check Java version
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
    if [ "$java_version" -lt "11" ]; then
        print_color "$RED" "Error: Java 11 or higher is required."
        print_color "$YELLOW" "Current version: $java_version"
        exit 1
    fi
    
    # Check for Maven
    if ! command -v mvn &> /dev/null; then
        print_color "$RED" "Error: Maven not found."
        print_color "$YELLOW" "Please install Maven using: brew install maven"
        exit 1
    fi
    
    # Check for MinGW (optional)
    if ! command -v x86_64-w64-mingw32-gcc &> /dev/null; then
        print_color "$YELLOW" "Warning: MinGW-w64 not found. Windows agent compilation will be skipped."
        print_color "$YELLOW" "Install MinGW-w64 using: brew install mingw-w64"
        HAS_MINGW=0
    else
        HAS_MINGW=1
    fi
    
    print_color "$GREEN" "Prerequisites check completed."
}

# Build Java server
function build_server() {
    print_color "$BLUE" "Building Java C2 server..."
    
    cd "$SCRIPT_DIR/server"
    mvn clean package
    
    if [ $? -eq 0 ]; then
        print_color "$GREEN" "Server build successful!"
        print_color "$GREEN" "Server JAR: server/target/c2-server-1.0-SNAPSHOT.jar"
    else
        print_color "$RED" "Server build failed."
        exit 1
    fi
}

# Build Windows agent
function build_agent() {
    print_color "$BLUE" "Building Windows agent..."
    
    if [ $HAS_MINGW -eq 1 ]; then
        cd "$SCRIPT_DIR/agents/windows"
        ./build_mac.sh
        
        if [ $? -eq 0 ]; then
            print_color "$GREEN" "Windows agent build successful!"
        else
            print_color "$RED" "Windows agent build failed."
            exit 1
        fi
    else
        print_color "$YELLOW" "Skipping Windows agent build (MinGW-w64 not installed)."
    fi
}

# Main function
function main() {
    print_color "$BLUE" "==== BitStrike C2 Framework Build Script ===="
    
    check_prereqs
    build_server
    build_agent
    
    print_color "$GREEN" "==== Build completed successfully! ===="
    print_color "$GREEN" "To run the server: java -jar server/target/c2-server-1.0-SNAPSHOT.jar"
    
    if [ $HAS_MINGW -eq 1 ]; then
        print_color "$YELLOW" "Note: The Windows agent (agent.exe) must be run on Windows."
        print_color "$YELLOW" "See README.md for instructions on testing with a Windows VM."
    fi
}

# Run the main function
main 