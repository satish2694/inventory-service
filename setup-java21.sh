#!/bin/bash

# Java 21 Setup Script for IntelliJ IDEA
# This script sets up the environment to use Java 21

echo "Setting up Java 21 environment..."

# Java 21 Path
JAVA21_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

# Verify Java 21 exists
if [ ! -d "$JAVA21_HOME" ]; then
    echo "❌ Error: Java 21 not found at $JAVA21_HOME"
    echo "Please install Java 21 using: brew install openjdk@21"
    exit 1
fi

# Verify Java 21 works
echo "✅ Java 21 found at: $JAVA21_HOME"
echo ""
echo "Java 21 Version:"
$JAVA21_HOME/bin/java -version
echo ""

# Export JAVA_HOME
export JAVA_HOME=$JAVA21_HOME
export PATH=$JAVA21_HOME/bin:$PATH

echo "✅ Environment variables set:"
echo "   JAVA_HOME=$JAVA_HOME"
echo "   PATH includes Java 21 bin directory"
echo ""

# Verify Maven can find Java 21
echo "Maven Configuration:"
mvn --version | head -5
echo ""

# Build command
echo "To build the project, run:"
echo "  mvn clean install -DskipTests"
echo ""

# Run command
echo "To run the project, use:"
echo "  mvn spring-boot:run"
echo ""

echo "✅ Setup complete!"
