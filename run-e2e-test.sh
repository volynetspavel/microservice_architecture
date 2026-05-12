#!/bin/bash
set -e

# ---------------------------------------------------------------------------
# Runs E2E tests against the full Docker Compose stack.
#
# Usage:
#   ./run-e2e-test.sh
#
# What it does:
#   1. Starts all services via docker compose
#   2. Waits for each service to respond on its HTTP port
#   3. Runs ResourceServiceE2EIT via Maven Failsafe
#   4. Brings down all services (runs even on failure)
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# --- 1. Ensure Docker daemon is running ---
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Starting Docker daemon..."
    sudo service docker start
    echo "Waiting for Docker to be ready..."
    until docker info > /dev/null 2>&1; do
        sleep 1
    done
    echo "Docker is ready."
else
    echo "Docker is already running."
fi

# --- 2. Resolve JAVA_HOME ---
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    for candidate in /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/default-java; do
        if [ -x "$candidate/bin/java" ]; then
            export JAVA_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    WIN_JAVA="/mnt/c/Program Files/Java/jdk-21.0.8"
    if [ -x "$WIN_JAVA/bin/java" ]; then
        export JAVA_HOME="$WIN_JAVA"
    else
        echo "ERROR: Java not found. Install it in WSL with:  sudo apt install openjdk-21-jdk"
        exit 1
    fi
fi

export PATH="$JAVA_HOME/bin:$PATH"
echo "Using Java: $JAVA_HOME"

# --- 3. Resolve Maven ---
if command -v mvn > /dev/null 2>&1; then
    MVN="mvn"
elif [ -f "/mnt/c/Program Files/Maven/apache-maven-3.9.11/bin/mvn" ]; then
    MVN="/mnt/c/Program Files/Maven/apache-maven-3.9.11/bin/mvn"
else
    echo "ERROR: Maven not found."
    echo "Install it in WSL with:  sudo apt install maven"
    exit 1
fi
"$MVN" -v

# --- 4. Start Docker Compose stack ---
echo ""
echo "Starting Docker Compose stack..."
cd "$SCRIPT_DIR"
docker compose up -d --build
echo "Waiting 30s for containers to initialise..."
sleep 30

# --- 5. Register cleanup trap ---
trap 'echo ""; echo "Stopping Docker Compose stack..."; docker compose down' EXIT

# --- 6. Wait for all services to be ready ---
wait_for_service() {
    local url=$1
    local name=$2
    local max_retries=60
    local attempt=0
    echo "Waiting for $name..."
    while [ $attempt -lt $max_retries ]; do
        local code
        code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null) || true
        if [ -n "$code" ] && [ "$code" != "000" ]; then
            echo "  $name is up (HTTP $code)."
            return 0
        fi
        attempt=$((attempt + 1))
        echo "  [$attempt/$max_retries] Not ready, retrying in 2s..."
        sleep 2
    done
    echo "ERROR: $name did not become ready within $((max_retries * 2))s."
    exit 1
}

echo ""
wait_for_service "http://localhost:8761/"            "Eureka Server       (8761)"
wait_for_service "http://localhost:8081/resources/1" "Resource Service    (8081)"
wait_for_service "http://localhost:8082/songs/1"     "Song Service        (8082)"
wait_for_service "http://localhost:8084/"            "Resource Processor  (8084)"

# --- 7. Run E2E tests ---
echo ""
echo "All services are up. Running E2E tests..."
cd "$SCRIPT_DIR/resource-service"
"$MVN" test \
    -Dtest="ResourceServiceE2E" \
    --no-transfer-progress