#!/bin/bash
#
# Start OpenEMS UI Docker Container (WSL2 Helper Script)
#
# This script is only needed for WSL2 environments where host-gateway
# doesn't resolve correctly. On Windows/Mac Docker Desktop and native Linux,
# you can use: docker compose up -d
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "OpenEMS UI Docker Startup Script (WSL2 Helper)"
echo "==============================================="

# Detect if running in WSL2
if grep -qi microsoft /proc/version 2>/dev/null; then
    echo "✓ WSL2 detected - applying IP workaround"
    IS_WSL2=true
else
    echo "✓ Native Linux/Docker Desktop detected"
    IS_WSL2=false
fi

# For WSL2, update the IP address
if [ "$IS_WSL2" = true ]; then
    # Get the WSL2 host IP address
    HOST_IP=$(hostname -I | awk '{print $1}')

    if [ -z "$HOST_IP" ]; then
        echo "❌ Error: Could not determine host IP address"
        exit 1
    fi

    echo "✓ Detected WSL2 host IP: $HOST_IP"

    # Check if docker-compose.yml exists
    if [ ! -f "docker-compose.yml" ]; then
        echo "❌ Error: docker-compose.yml not found in $SCRIPT_DIR"
        exit 1
    fi

    # Update the extra_hosts entry with current IP
    # Replace either host-gateway or existing IP with the WSL2 IP
    sed -i "s/host\.docker\.internal:host-gateway/host.docker.internal:$HOST_IP/" docker-compose.yml
    sed -i "s/host\.docker\.internal:[0-9]\+\.[0-9]\+\.[0-9]\+\.[0-9]\+/host.docker.internal:$HOST_IP/" docker-compose.yml

    echo "✓ Updated docker-compose.yml with WSL2 host IP"
else
    sed -i "s/host\.docker\.internal:[0-9]\+\.[0-9]\+\.[0-9]\+\.[0-9]\+/host.docker.internal:host-gateway/" docker-compose.yml
    echo "✓ Using host-gateway (default, no IP update needed)"
fi

# Stop existing containers
if docker ps -a --format '{{.Names}}' | grep -q '^openems_ui$'; then
    echo "✓ Stopping existing container..."
    docker compose down
fi

# Start the container
echo "✓ Starting OpenEMS UI container..."
docker compose up -d

# Wait a moment for container to start
sleep 2

# Verify container is running
if docker ps --format '{{.Names}}' | grep -q '^openems_ui$'; then
    echo ""
    echo "✅ OpenEMS UI is running!"
    echo ""
    echo "   Web UI:       http://localhost:4200"
    echo "   HTTPS:        https://localhost:443"
    if [ "$IS_WSL2" = true ]; then
        echo "   WebSocket:    host.docker.internal:8085 → $HOST_IP:8085"
    else
        echo "   WebSocket:    host.docker.internal:8085"
    fi
    echo ""

    # Test WebSocket connectivity
    echo "Testing WebSocket connectivity..."
    if docker exec openems_ui curl -s -o /dev/null -w "%{http_code}" http://host.docker.internal:8085 2>/dev/null | grep -q "404"; then
        echo "✅ WebSocket connection successful (Edge is reachable)"
    else
        echo "⚠️  Warning: Could not connect to Edge WebSocket"
        echo "   Make sure OpenEMS Edge is running on port 8085"
    fi
else
    echo "❌ Error: Container failed to start"
    docker compose logs --tail 20
    exit 1
fi

echo ""
echo "To view logs: docker compose logs -f"
echo "To stop:      docker compose down"
