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

# Find New Relic agent JAR
NEWRELIC_JAR=""

# Check in order of preference:
# 1. Local directory: ./newrelic.jar (included in project)
# 2. Environment variable NEW_RELIC_JAR_PATH (custom location)

if [ -f "newrelic.jar" ]; then
    NEWRELIC_JAR="newrelic.jar"
    echo "Found New Relic agent at: ./newrelic.jar"
elif [ ! -z "${NEW_RELIC_JAR_PATH}" ] && [ -f "${NEW_RELIC_JAR_PATH}" ]; then
    NEWRELIC_JAR="${NEW_RELIC_JAR_PATH}"
    echo "Found New Relic agent at: ${NEWRELIC_JAR}"
fi

# Check if New Relic agent is being used
if [ ! -z "${NEWRELIC_JAR}" ] && [ ! -z "${NEW_RELIC_LICENSE_KEY}" ]; then
    echo "Running with New Relic Java Agent..."
    echo "App Name: ${NEW_RELIC_APP_NAME:-Analytics Load Generator}"
    echo ""
    java -javaagent:"${NEWRELIC_JAR}" \
         -Dnewrelic.config.license_key="${NEW_RELIC_LICENSE_KEY}" \
         -Dnewrelic.config.app_name="${NEW_RELIC_APP_NAME:-Analytics Load Generator}" \
         -Dnewrelic.config.log_level="${NEW_RELIC_LOG_LEVEL:-info}" \
         -Dnewrelic.config.distributed_tracing.enabled="${NEW_RELIC_DISTRIBUTED_TRACING_ENABLED:-true}" \
         -Dthreads=${THREADS} \
         -jar "$JAR_FILE"
else
    if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
        echo "Running without New Relic (NEW_RELIC_LICENSE_KEY not set in .env)"
    else
        echo "Running without New Relic (newrelic.jar not found)"
        echo "Tip: Set NEW_RELIC_JAR_PATH in .env to point to your newrelic.jar location"
    fi
    echo ""
    java -Dthreads=${THREADS} -jar "$JAR_FILE"
fi
