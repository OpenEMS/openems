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
if ! ( command -v wslinfo > /dev/null 2>&1 ); then
    echo "No WSL2 environment detected. Will exit."
    exit 1
fi

# Get the WSL2 host IP address
HOST_IP=$(hostname -I | awk '{print $1}')

if [ -z "$HOST_IP" ]; then
    echo "❌ Error: Could not determine host IP address"
    exit 1
fi

echo "✓ Detected WSL2 host IP: $HOST_IP"

# Create or update docker-compose.override.yml with host IP
echo "
services:
  openems-ui-dev:
    extra_hosts:
      - "host.docker.internal:$HOST_IP"
" > docker-compose.override.yml
echo "✓ Created or Updated docker-compose.override.yml with WSL2 host IP"
echo "Note: Docker merges it with docker-compose.yml on start, so it will use host.docker.internal:$HOST_IP"


# (re)Start the container
echo "✓ (Re)Starting OpenEMS UI container..."
docker compose up -d

# Wait a moment for container to start
sleep 2

# Verify container is running
if docker ps --format '{{.Names}}' | grep -q '^openems_ui_dev$'; then
    echo ""
    echo "✅ OpenEMS UI is running!"
    echo ""
    echo "   Web UI:       http://localhost:4200"

    echo "   WebSocket:    host.docker.internal:8085 → $HOST_IP:8085"


    # Test WebSocket connectivity
    echo "Testing WebSocket connectivity..."
    if docker exec openems_ui_dev curl -s -o /dev/null -w "%{http_code}" http://host.docker.internal:8085 2>/dev/null | grep -q "404"; then
        echo "✅ WebSocket connection successful (Edge is reachable)"
    else
        echo "⚠️  Warning: Could not connect to Edge WebSocket"
        echo "   Make sure OpenEMS Edge WebSocket is running on port 8085"
    fi
else
    echo "❌ Error: Container failed to start"
    docker compose logs --tail 20
    exit 1
fi

echo ""
echo "To view logs: docker compose logs -f"
echo "To stop:      docker compose down"
