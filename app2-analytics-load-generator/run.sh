#!/bin/bash

# Run script for Analytics Load Generator with .env support

set -e  # Exit on error

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading configuration from .env file..."
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
else
    echo "WARNING: .env file not found. Using default environment variables or application.properties"
    echo "Copy .env.example to .env and configure your database credentials"
fi

# Check if JAR file exists
JAR_FILE="target/app2-analytics-load-generator-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: $JAR_FILE not found"
    echo "Please build the application first: mvn clean package"
    exit 1
fi

# Set thread count from environment or default
THREADS=${THREADS:-20}

echo "=========================================="
echo "Starting Analytics Load Generator"
echo "=========================================="
echo "Database: ${DB_URL}"
echo "Username: ${DB_USERNAME}"
echo "Threads: ${THREADS}"
echo "Pool Max: ${DB_POOL_MAX:-50}"
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

    java -javaagent:newrelic.jar \
         -Dnewrelic.config.license_key="${NEW_RELIC_LICENSE_KEY}" \
         -Dnewrelic.config.app_name="${NEW_RELIC_APP_NAME}" \
         -Dnewrelic.config.log_level="${NEW_RELIC_LOG_LEVEL}" \
         -Dnewrelic.config.distributed_tracing.enabled="${NEW_RELIC_DISTRIBUTED_TRACING}" \
         -Dthreads=${THREADS} \
         -jar "$JAR_FILE"
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
