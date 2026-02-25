#!/bin/bash

# Script to run all load generator applications
# Usage: ./runBothApps.sh         - Start all apps in background
#        ./runBothApps.sh --stop   - Stop all apps
#        ./runBothApps.sh --logs   - View logs from all apps

set -e

APP1_DIR="app1-oltp-load-generator"
APP2_DIR="app2-analytics-load-generator"
APP3_DIR="app3-analytics-load-generator"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Stop mode
if [[ "$1" == "--stop" ]]; then
    echo "=========================================="
    echo "Stopping All Applications"
    echo "=========================================="

    echo -e "${YELLOW}Stopping App1 (OLTP)...${NC}"
    pkill -f 'java.*app1-oltp-load-generator' && echo -e "${GREEN}✓ App1 stopped${NC}" || echo -e "${YELLOW}⚠ App1 not running${NC}"

    echo -e "${YELLOW}Stopping App2 (Analytics)...${NC}"
    pkill -f 'java.*app2-analytics-load-generator' && echo -e "${GREEN}✓ App2 stopped${NC}" || echo -e "${YELLOW}⚠ App2 not running${NC}"

    echo -e "${YELLOW}Stopping App3 (Analytics3)...${NC}"
    pkill -f 'java.*app3-analytics-load-generator' && echo -e "${GREEN}✓ App3 stopped${NC}" || echo -e "${YELLOW}⚠ App3 not running${NC}"

    echo "=========================================="
    exit 0
fi

# View logs mode
if [[ "$1" == "--logs" ]]; then
    echo "=========================================="
    echo "Logs are not stored to disk"
    echo "=========================================="
    echo "Application logs are redirected to /dev/null to save disk space."
    echo "Monitor application status using:"
    echo "  - New Relic UI for transaction data"
    echo "  - ps aux | grep java to check if processes are running"
    echo "=========================================="
    exit 0
fi

# Check if apps are already running
echo "=========================================="
echo "Checking for Running Applications"
echo "=========================================="

APP1_RUNNING=$(pgrep -f 'java.*app1-oltp-load-generator' || echo "")
APP2_RUNNING=$(pgrep -f 'java.*app2-analytics-load-generator' || echo "")
APP3_RUNNING=$(pgrep -f 'java.*app3-analytics-load-generator' || echo "")

if [ -n "$APP1_RUNNING" ] || [ -n "$APP2_RUNNING" ] || [ -n "$APP3_RUNNING" ]; then
    echo -e "${YELLOW}Found running applications:${NC}"
    [ -n "$APP1_RUNNING" ] && echo "  App1 (OLTP): PID $APP1_RUNNING"
    [ -n "$APP2_RUNNING" ] && echo "  App2 (Analytics): PID $APP2_RUNNING"
    [ -n "$APP3_RUNNING" ] && echo "  App3 (Analytics3): PID $APP3_RUNNING"
    echo ""
    echo -e "${YELLOW}Stopping existing processes...${NC}"

    [ -n "$APP1_RUNNING" ] && pkill -f 'java.*app1-oltp-load-generator' && echo -e "${GREEN}✓ Stopped App1${NC}"
    [ -n "$APP2_RUNNING" ] && pkill -f 'java.*app2-analytics-load-generator' && echo -e "${GREEN}✓ Stopped App2${NC}"
    [ -n "$APP3_RUNNING" ] && pkill -f 'java.*app3-analytics-load-generator' && echo -e "${GREEN}✓ Stopped App3${NC}"

    # Wait for processes to fully terminate
    sleep 2
fi

echo -e "${GREEN}✓ No running applications${NC}"
echo ""

# Check if JAR files exist
echo "=========================================="
echo "Verifying Build Artifacts"
echo "=========================================="

if [ ! -f "$APP1_DIR/target/app1-oltp-load-generator-1.0.0.jar" ]; then
    echo -e "${RED}✗ App1 JAR not found${NC}"
    echo "Please build first: ./build-all.sh"
    exit 1
fi

if [ ! -f "$APP2_DIR/target/app2-analytics-load-generator-1.0.0.jar" ]; then
    echo -e "${RED}✗ App2 JAR not found${NC}"
    echo "Please build first: ./build-all.sh"
    exit 1
fi

if [ ! -f "$APP3_DIR/target/app3-analytics-load-generator-1.0.0.jar" ]; then
    echo -e "${RED}✗ App3 JAR not found${NC}"
    echo "Please build first: ./build-all.sh"
    exit 1
fi

echo -e "${GREEN}✓ All JAR files found${NC}"
echo ""

# Start App1
echo "=========================================="
echo "Starting App1 (OLTP Load Generator)"
echo "=========================================="
cd "$APP1_DIR"
./run.sh --bg
cd ..
echo ""

# Wait a moment before starting App2
sleep 2

# Start App2
echo "=========================================="
echo "Starting App2 (Analytics Load Generator)"
echo "=========================================="
cd "$APP2_DIR"
./run.sh --bg
cd ..
echo ""

# Wait a moment before starting App3
sleep 2

# Start App3
echo "=========================================="
echo "Starting App3 (Analytics Load Generator 3)"
echo "=========================================="
cd "$APP3_DIR"
./run.sh --bg
cd ..
echo ""

# Wait for apps to start
sleep 3

# Verify all apps are running
echo "=========================================="
echo "Verification"
echo "=========================================="

APP1_PID=$(pgrep -f 'java.*app1-oltp-load-generator' || echo "")
APP2_PID=$(pgrep -f 'java.*app2-analytics-load-generator' || echo "")
APP3_PID=$(pgrep -f 'java.*app3-analytics-load-generator' || echo "")

if [ -n "$APP1_PID" ]; then
    echo -e "${GREEN}✓ App1 running (PID: $APP1_PID)${NC}"
else
    echo -e "${RED}✗ App1 failed to start${NC}"
fi

if [ -n "$APP2_PID" ]; then
    echo -e "${GREEN}✓ App2 running (PID: $APP2_PID)${NC}"
else
    echo -e "${RED}✗ App2 failed to start${NC}"
fi

if [ -n "$APP3_PID" ]; then
    echo -e "${GREEN}✓ App3 running (PID: $APP3_PID)${NC}"
else
    echo -e "${RED}✗ App3 failed to start${NC}"
fi

echo "=========================================="
echo ""
echo "Useful Commands:"
echo "  Stop apps:     ./runBothApps.sh --stop"
echo "  Monitor CPU:   watch -n 2 'ps aux | head -1 && ps aux | grep -E \"java.*app[123]\" | grep -v grep'"
echo "  Check status:  ps aux | grep 'java.*app[123]'"
echo ""
echo "Note: Logs are not stored to disk (redirected to /dev/null)"
echo "      Monitor via New Relic UI for transaction data"
echo ""
echo "=========================================="
