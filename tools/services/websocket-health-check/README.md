# WebSocket Health Check

A comprehensive monitoring solution for OpenEMS WebSocket connections that provides automated health checking and service recovery.

**Location:** `websocket-health-check/`

## Components

- **`websocket-health-check.py`** - Python health check script
- **`websocket-health-check.service`** - Systemd service definition
- **`websocket-health-check.timer`** - Systemd timer for periodic execution

## Features

- **Automated WebSocket Monitoring**: Periodically connects to WebSocket endpoints to verify availability
- **Smart Failure Tracking**: Tracks consecutive failures with configurable thresholds
- **Automatic Service Recovery**: Restarts failed services when thresholds are exceeded
- **Dry Run Mode**: Test mode for validation without actual service restarts

## Configuration

The health check can be configured with the following parameters:

| Parameter | Description | Default |
|-----------|-------------|---------|
| `uri` | WebSocket URI to monitor | Required |
| `service` | Target systemd service name | Required |
| `--state-file` | Path to failure count state file | `/tmp/websocket-health-check/failcount` |
| `--threshold` | Failure count before service restart | `3` |
| `--log-file` | Path to action log file | `./trigger.log` |
| `--dry-run` | Test mode without actual restarts | `false` |

## Installation

1. **Install Dependencies**:
   ```bash
   # Debian/Ubuntu
   sudo apt install python3-websockets
   
   # Or via pip
   pip install websockets
   ```

2. **Deploy Service Files**:
   ```bash
   # Copy files to service directory
   sudo cp websocket-health-check/ /opt/ -r
   sudo chmod +x /opt/websocket-health-check/websocket-health-check.py
   
   # Install systemd service
   sudo cp websocket-health-check.service /usr/lib/systemd/system/
   sudo cp websocket-health-check.timer /usr/lib/systemd/system/
   
   # Reload systemd and enable services
   sudo systemctl daemon-reload
   sudo systemctl enable websocket-health-check.timer
   sudo systemctl start websocket-health-check.timer
   ```

3. **Configure for Your Environment**:
   Edit `/etc/systemd/system/websocket-health-check.service` to match your setup:
   ```ini
   ExecStart=/opt/websocket-health-check/websocket-health-check.py \
       "ws://localhost:port" \
       "<your-service>.service" \
       --threshold 6 \
       --state-file /tmp/websocket-health-check/failcount \
       --log-file /var/log/websocket-health-check.log
   ```

## Monitoring

Check service status:
```bash
# Timer status
sudo systemctl status websocket-health-check.timer

# Service execution logs
sudo journalctl -u websocket-health-check.service -f

# Check restart triggers
tail -f /opt/websocket-health-check/trigger.log
```