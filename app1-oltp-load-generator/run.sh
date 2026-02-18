#!/bin/bash

# Run script for OLTP Load Generator with .env support

set -e  # Exit on error

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading configuration from .env file..."
    # Export variables, ignoring comments and empty lines
    set -a
    source <(cat .env | sed 's/#.*//g' | grep -v '^$' | grep '=')
    set +a
else
    echo "WARNING: .env file not found. Using default environment variables or application.properties"
    echo "Copy .env.example to .env and configure your database credentials"
fi

# Check if JAR file exists
JAR_FILE="target/app1-oltp-load-generator-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: $JAR_FILE not found"
    echo "Please build the application first: mvn clean package"
    exit 1
fi

# Set thread count from environment or default
THREADS=${THREADS:-50}

echo "=========================================="
echo "Starting OLTP Load Generator"
echo "=========================================="
echo "Database: ${DB_URL}"
echo "Username: ${DB_USERNAME}"
echo "Threads: ${THREADS}"
echo "Pool Max: ${DB_POOL_MAX:-100}"
echo "=========================================="
echo ""

# Check if New Relic should be enabled
if [ -f "newrelic.jar" ] && [ "${NEW_RELIC_ENABLED}" = "true" ] && [ "${NEW_RELIC_LICENSE_KEY}" != "YOUR_LICENSE_KEY_HERE" ] && [ ! -z "${NEW_RELIC_LICENSE_KEY}" ]; then
    echo "=========================================="
    echo "New Relic Agent: ENABLED"
    echo "=========================================="
    echo "License Key: ${NEW_RELIC_LICENSE_KEY:0:15}...${NEW_RELIC_LICENSE_KEY: -4}"
    echo "App Name: ${NEW_RELIC_APP_NAME}"
    echo "Log Level: ${NEW_RELIC_LOG_LEVEL}"
    echo "Distributed Tracing: ${NEW_RELIC_DISTRIBUTED_TRACING}"
    echo "=========================================="
    echo ""

    # Build Java command with New Relic configuration
    JAVA_OPTS="-javaagent:newrelic.jar"
    JAVA_OPTS="$JAVA_OPTS -Dnewrelic.config.license_key=${NEW_RELIC_LICENSE_KEY}"
    JAVA_OPTS="$JAVA_OPTS -Dnewrelic.config.app_name=${NEW_RELIC_APP_NAME}"
    JAVA_OPTS="$JAVA_OPTS -Dnewrelic.config.log_level=${NEW_RELIC_LOG_LEVEL}"
    JAVA_OPTS="$JAVA_OPTS -Dnewrelic.config.distributed_tracing.enabled=${NEW_RELIC_DISTRIBUTED_TRACING}"

    # Add staging collector host if configured
    if [ ! -z "${NEW_RELIC_HOST}" ]; then
        JAVA_OPTS="$JAVA_OPTS -Dnewrelic.config.host=${NEW_RELIC_HOST}"
        echo "Collector Host: ${NEW_RELIC_HOST}"
    fi

    JAVA_OPTS="$JAVA_OPTS -Dthreads=${THREADS}"

    java $JAVA_OPTS -jar "$JAR_FILE"
else
    echo "=========================================="
    echo "New Relic Agent: DISABLED"
    echo "=========================================="
    if [ ! -f "newrelic.jar" ]; then
        echo "Reason: newrelic.jar not found"
    elif [ "${NEW_RELIC_ENABLED}" != "true" ]; then
        echo "Reason: NEW_RELIC_ENABLED=${NEW_RELIC_ENABLED}"
    elif [ -z "${NEW_RELIC_LICENSE_KEY}" ] || [ "${NEW_RELIC_LICENSE_KEY}" = "YOUR_LICENSE_KEY_HERE" ]; then
        echo "Reason: NEW_RELIC_LICENSE_KEY not configured in .env"
    fi
    echo "=========================================="
    echo ""

    java -Dthreads=${THREADS} -jar "$JAR_FILE"
fi
