#!/bin/bash

# Build script for both load generator applications

set -e  # Exit on error

echo "=========================================="
echo "Building Oracle Load Generator Applications"
echo "=========================================="
echo ""

# Pull latest code from git
echo "Pulling latest code from git..."
if git pull; then
    echo "✓ Code updated successfully"
else
    echo "⚠ Git pull failed or not a git repository"
    echo "Continuing with existing code..."
fi

echo ""

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven 3.6+ and try again"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep version | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "ERROR: Java 11 or higher is required"
    echo "Current Java version: $JAVA_VERSION"
    exit 1
fi

echo "Maven version:"
mvn -version | head -1

echo "Java version:"
java -version 2>&1 | head -1

echo ""
echo "Building Application 1: OLTP Load Generator"
echo "--------------------------------------------"
cd app1-oltp-load-generator
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "✓ OLTP Load Generator built successfully"
    echo "  Output: app1-oltp-load-generator/target/app1-oltp-load-generator-1.0.0.jar"
else
    echo "✗ OLTP Load Generator build failed"
    exit 1
fi
cd ..

echo ""
echo "Building Application 2: Analytics Load Generator"
echo "------------------------------------------------"
cd app2-analytics-load-generator
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "✓ Analytics Load Generator built successfully"
    echo "  Output: app2-analytics-load-generator/target/app2-analytics-load-generator-1.0.0.jar"
else
    echo "✗ Analytics Load Generator build failed"
    exit 1
fi
cd ..

echo ""
echo "Building Application 3: Analytics Load Generator 3"
echo "------------------------------------------------"
cd app3-analytics-load-generator
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "✓ Analytics Load Generator 3 built successfully"
    echo "  Output: app3-analytics-load-generator/target/app3-analytics-load-generator-1.0.0.jar"
else
    echo "✗ Analytics Load Generator 3 build failed"
    exit 1
fi
cd ..

echo ""
echo "=========================================="
echo "Build Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Set up Oracle database (see oracle-setup.sql)"
echo "2. Configure database connection (see application.properties or use environment variables)"
echo "3. (Optional) Set up New Relic Java agent (see newrelic-setup.md)"
echo "4. Run the applications:"
echo ""
echo "   # Without New Relic:"
echo "   cd app1-oltp-load-generator"
echo "   java -Dthreads=50 -jar target/app1-oltp-load-generator-1.0.0.jar"
echo ""
echo "   # With New Relic:"
echo "   java -javaagent:newrelic.jar -Dnewrelic.config.license_key=YOUR_KEY -jar target/app1-oltp-load-generator-1.0.0.jar"
echo ""
echo "See README.md for complete documentation"
