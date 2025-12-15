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
./start-dev-ui-wsl.sh
```
This creates a `docker-compose.override.yml` that fixes the IP-Address resolution problem in WSL2. 
If you restart your WSL2, just call the script again, to adjust to the new IP address your WSL2 might have now.

In both cases: Access the UI at **http://localhost:4200**

### Prerequisites

- OpenEMS Edge must be running on port 8085 (WebSocket)
- Docker and Docker Compose installed

### Configuration

#### Environment Variables

- `WEBSOCKET_HOST`: Hostname/IP for the Edge WebSocket server (default: `host.docker.internal`)
- `WEBSOCKET_PORT`: Port for the Edge WebSocket server (default: `8085`)

**Note:**  The ui container proxies the websocket connection and is the one that has to be able to reach the WEBSOCKET_HOST:WEBSOCKET_PORT

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
- **WSL2**: Requires workaround (use `start-dev-ui-wsl.sh`)

#### WebSocket Connection Flow

```
Browser → UI (nginx:4200) → Static Files
Browser → UI (nginx:4200) → Edge (host.docker.internal:8085) → WebSocket
```

The WebSocket connection is routed (proxied) through nginx in the UI container to OpenEMS Edge.

### Troubleshooting
Please have a look at the documentation in the [Troubleshooting section](https://openems.github.io/openems.io/openems/latest/ui/deploy.html#Troubleshooting) there.

---
## Files

- `Dockerfile.edge` / `Dockerfile.backend` - Build configurations
- `docker-compose.yml` - Main configuration to run pre-built UI for development (works on all platforms, except WSL2)
- `start-dev-ui-wsl.sh` - WSL2 helper script (optional) to create a suitable `docker-compose.override.yml` with the proper IP address of your current WSL2 instance. 
- `README.md` - This file

