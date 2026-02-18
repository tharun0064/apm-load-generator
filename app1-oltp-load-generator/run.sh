#!/bin/bash

# Run script for OLTP Load Generator with .env support

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

# Check if New Relic agent JAR and config exist
if [ -f "newrelic.jar" ] && [ -f "newrelic.yml" ]; then
    echo "Found New Relic agent at: ./newrelic.jar"
    echo "Found New Relic config at: ./newrelic.yml"
    echo ""
    echo "Running with New Relic Java Agent..."
    echo "App Name: OLTP Load Generator (from newrelic.yml)"
    echo ""
    java -javaagent:newrelic.jar \
         -Dthreads=${THREADS} \
         -jar "$JAR_FILE"
else
    if [ ! -f "newrelic.jar" ]; then
        echo "WARNING: newrelic.jar not found"
    fi
    if [ ! -f "newrelic.yml" ]; then
        echo "WARNING: newrelic.yml not found"
    fi
    echo ""
    echo "Running without New Relic agent"
    echo ""
    java -Dthreads=${THREADS} -jar "$JAR_FILE"
fi
