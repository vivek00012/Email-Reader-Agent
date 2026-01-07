#!/bin/bash

# Email Reader Agent - Quick Start Script
# This script helps you quickly build and run the application

set -e

echo "=================================================="
echo "   Email Reader Agent - Quick Start"
echo "=================================================="
echo ""

# Check Java version
echo "Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "✓ Java version: $JAVA_VERSION"
else
    echo "✗ Java not found. Please install Java 17 or higher."
    exit 1
fi

# Check Maven
echo "Checking Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo "✓ $MVN_VERSION"
else
    echo "✗ Maven not found. Please install Maven 3.6 or higher."
    exit 1
fi

echo ""

# Check for credentials file
if [ ! -f "src/main/resources/credentials.json" ]; then
    echo "⚠️  WARNING: credentials.json not found!"
    echo ""
    echo "Please follow these steps:"
    echo "1. Go to https://console.cloud.google.com/"
    echo "2. Create a new project and enable Gmail API"
    echo "3. Create OAuth 2.0 credentials (Desktop app)"
    echo "4. Download the credentials file"
    echo "5. Save it as: src/main/resources/credentials.json"
    echo ""
    echo "See SETUP.md for detailed instructions."
    echo ""
    read -p "Do you want to continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "Building the application..."
mvn clean install -DskipTests

echo ""
echo "=================================================="
echo "   Starting Email Reader Agent..."
echo "=================================================="
echo ""
echo "📝 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "🏥 Health Check: http://localhost:8080/actuator/health"
echo "📊 API Docs: http://localhost:8080/v3/api-docs"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

mvn spring-boot:run
