#!/bin/bash

# Fix package errors in Java files
find server/src/main/java/com/bitstrike -name "*.java" | while read file; do
    # Replace incorrect package declarations
    sed -i '' 's/package com\.bitstrike\./package com.bitstrike./g' "$file"
    
    # Remove any remaining package errors
    sed -i '' 's/package BitStrike\.server\.src\.main\.java\.com\.bitstrike/package com.bitstrike/g' "$file"
    sed -i '' 's/package server\.src\.main\.java\.com\.bitstrike/package com.bitstrike/g' "$file"
done

# Create necessary directories for resources
mkdir -p server/src/main/resources
mkdir -p server/src/test/java
mkdir -p server/src/test/resources

# Copy logback configuration to the correct location
cp -f server/src/main/resources/logback.xml server/src/main/resources/logback.xml 2>/dev/null || :

echo "Package paths fixed successfully." 