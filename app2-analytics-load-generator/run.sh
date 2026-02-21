#!/bin/bash

# Run script for Analytics Load Generator with .env support
# Usage: ./run.sh           - Run in foreground
#        ./run.sh --bg       - Run in background with logs in app.log
#        ./run.sh --stop     - Stop background process

set -e  # Exit on error

# Check for background flag
BACKGROUND=false
STOP=false
if [[ "$1" == "--bg" || "$1" == "-b" ]]; then
    BACKGROUND=true
elif [[ "$1" == "--stop" ]]; then
    STOP=true
fi

# Stop mode: kill the process
if [ "$STOP" = true ]; then
    echo "Stopping Analytics Load Generator..."
    pkill -f 'java.*app2-analytics-load-generator' && echo "Process stopped" || echo "No running process found"
    exit 0
fi

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

# Check if New Relic agent is available
if [ -f "newrelic.jar" ] && [ -f "newrelic.yml" ]; then
    echo "=========================================="
    echo "New Relic Agent: ENABLED"
    echo "=========================================="
    echo "Config File: ./newrelic.yml"
    echo "Agent JAR: ./newrelic.jar"
    echo "Note: Configuration is read from newrelic.yml"
    echo "=========================================="
    echo ""

    if [ "$BACKGROUND" = true ]; then
        nohup java -javaagent:newrelic.jar -Dthreads=${THREADS} -jar "$JAR_FILE" > app.log 2>&1 &
        PID=$!
        echo "Started in background with PID: $PID"
        echo "Logs: tail -f app.log"
    else
        java -javaagent:newrelic.jar -Dthreads=${THREADS} -jar "$JAR_FILE"
    fi
else
    echo "=========================================="
    echo "New Relic Agent: DISABLED"
    echo "=========================================="
    if [ ! -f "newrelic.jar" ]; then
        echo "Reason: newrelic.jar not found"
    fi
    if [ ! -f "newrelic.yml" ]; then
        echo "Reason: newrelic.yml not found"
    fi
    echo "=========================================="
    echo ""

    if [ "$BACKGROUND" = true ]; then
        nohup java -Dthreads=${THREADS} -jar "$JAR_FILE" > app.log 2>&1 &
        PID=$!
        echo "Started in background with PID: $PID"
        echo "Logs: tail -f app.log"
    else
        java -Dthreads=${THREADS} -jar "$JAR_FILE"
    fi
fi
