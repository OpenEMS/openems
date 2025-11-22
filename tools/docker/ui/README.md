# OpenEMS UI Docker

## Build Your Own OpenEMS UI Image

1. **Go to the root directory of the OpenEMS project**

2. **Build command:**

    *For Edge:*
    ```bash
    docker build . -t openems_ui-edge -f tools/docker/ui/Dockerfile.edge
    ```

    *For Backend:*
    ```bash
    docker build . -t openems_ui-backend -f tools/docker/ui/Dockerfile.backend
    ```

---
## Running the Pre-built UI + Edge/Backend Containers 

see [backend/README.md](../backend/README.md) and [edge/README.md](../edge/README.md) respectively.

## Running the Pre-built UI Container for Edge development

### Quick Start
Due to some networking quirks of WSL2/Linux on Windows machines there is a distinction needed:

**Standard (Edge Development on Windows/Mac/Linux):**
in `tools/docker/ui` run:
```bash
docker compose up -d
```

**WSL2 Users: (Edge Development on WSL2, Browser for UI on Windows)**
in `tools/docker/ui` run:
```bash
./start-ui.sh
```

In both cases: Access the UI at **http://localhost:4200**

### Prerequisites

- OpenEMS Edge must be running on port 8085 (WebSocket)
- Docker and Docker Compose installed

### Configuration

#### Environment Variables

- `WEBSOCKET_HOST`: Hostname/IP for the Edge WebSocket server (default: `host.docker.internal`)
- `WEBSOCKET_PORT`: Port for the Edge WebSocket server (default: `8085`)

**Note:** These configure where the **browser** connects to reach OpenEMS Edge, not the container itself.

#### Volumes

- `openems-ui-conf`: Nginx configuration (`/etc/nginx`)
- `openems-ui-log`: Nginx logs (`/var/log/nginx`)

### How It Works

The docker-compose.yml uses `host-gateway` to automatically resolve the host machine:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

- **Windows/Mac Docker Desktop**: Works automatically
- **Native Linux**: Works automatically
- **WSL2**: Requires workaround (use `start-ui.sh`)

#### WebSocket Connection Flow

```
Browser → UI (nginx:4200) → Static Files
Browser → Edge (host.docker.internal:8085) → WebSocket
```

The WebSocket connection is made **directly from your browser** to OpenEMS Edge (proxied through nginx).

### Troubleshooting

#### UI shows "Connection Failed"

**1. Verify Edge is running:**
```bash
curl -I http://localhost:8085
# Expected: HTTP/1.1 404 WebSocket Upgrade Failure
```

**2. For WSL2 - use the helper script:**
```bash
./start-ui.sh
```

#### Port 4200 already in use

```bash
docker compose down
lsof -i :4200  # Find process using port
```

#### Container won't start

```bash
docker compose logs -f
docker compose down
docker compose up -d
```

#### WSL2: IP changed after reboot

```bash
./start-ui.sh  # Auto-detects and updates IP
```

---

## Files

- `Dockerfile.edge` / `Dockerfile.backend` - Build configurations
- `docker-compose.yml` - Main configuration to run pre-built UI for development (works on all platforms, except WSL2)
- `start-ui.sh` - WSL2 helper script (optional) to run pre-built UI for development in WSL2
- `README.md` - This file

