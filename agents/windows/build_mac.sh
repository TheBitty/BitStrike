#!/bin/bash

# Check if MinGW is installed
if ! command -v x86_64-w64-mingw32-gcc &> /dev/null; then
    echo "Error: MinGW cross-compiler not found."
    echo "Please install it with: brew install mingw-w64"
    exit 1
fi

# Get the directory of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Cross-compiling Windows agent on macOS..."

# Build the agent
x86_64-w64-mingw32-gcc -Wall -Wextra -O2 -s agent.c -o agent.exe -lwinhttp -lws2_32

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful! agent.exe has been created."
    echo ""
    echo "Note: This executable can only be run on Windows."
    echo "Options for testing:"
    echo "1. Use a Windows VM (recommended)"
    echo "2. Copy to a Windows machine"
    echo "3. Use Wine (limited functionality)"
    
    # Get file size
    SIZE=$(ls -lh agent.exe | awk '{print $5}')
    echo ""
    echo "Agent size: $SIZE"
else
    echo "Build failed."
    exit 1
fi 