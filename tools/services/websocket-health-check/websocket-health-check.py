#!/usr/bin/env python3

# This script checks if a WebSocket server is reachable.
# It can be used as a watchdog to monitor WebSocket services.
# It will:
# - Do nothing if the WebSocket is reachable
# - Print a log if the WebSocket is not reachable but below the threshold
# - Restart the Service, if the WebSocket is not reachable and above the threshold
#
# Requirements:
# - Python 3.7 or higher
# - websockets library (install with `pip install websockets` or `sudo apt install python3-websockets`)
# - systemd service for the WebSocket server
#
# example usage: ./websocket-health-check.py "ws://localhost:8082" "openems-backend.service" --state-file "/tmp/ws-watchdog/state" --threshold 3

import asyncio
import websockets
import argparse
import os
import subprocess
import sys
from datetime import datetime

DEFAULT_STATE_FILE = "/tmp/websocket-health-check/failcount"
DEFAULT_THRESHOLD = 3

async def check_websocket(uri) -> bool:
    try:
        async with websockets.connect(uri) as websocket:
            print(f"[Info] Connected to {uri}")
            await websocket.close(code=1000, reason="Health check")
            return True
    except Exception as e:
        print(f"[Error] Failed to connect: {e}")
        return False

def read_state(state_file: str, pid: str) -> int:
    if not os.path.exists(state_file):
        return -1
    try:
        with open(state_file, "r") as f:
            value: str = f.read().strip()
            if not value:
                return -1
            parts = value.split(":")
            if len(parts) != 2 or not parts[1].isdigit():
                return -1
            if parts[0] != pid:
                # The PID in the state file does not match the current service PID. OpenEMS was restarted.
                # Therefore, reset the fail count, to wait for the next WebSocket connection.
                return -1
            return int(parts[1])
    except Exception as e:
        print(f"[Error] Could not read state file {state_file}. Resetting state: {e}")
        return -1

def write_state(state_file: str, pid: str, count: int) -> None:
    os.makedirs(os.path.dirname(state_file), exist_ok=True)
    try:
        with open(state_file, "w") as f:
            f.write(f"{pid}:{count}")
    except Exception as e:
        print(f"[Error] Could not write to state file {state_file}: {e}")
        sys.exit(1)

def get_service_pid(service: str) -> str:
    try:
        result = subprocess.run(
            ["systemctl", "show", service, "--property=MainPID", "--value"],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"[Error] Could not get PID for service {service}: {e}")
        sys.exit(1)

def restart_service(service: str):
    try:
        subprocess.run(["systemctl", "restart", service], check=True)
        print(f"[Watchdog] {service} was restarted.")
    except Exception as e:
        print(f"[Error] Could not restart service {service}: {e}")

def write_log(log_file: str, message: str) -> None:
    os.makedirs(os.path.dirname(log_file), exist_ok=True)
    try:
        with open(log_file, "a") as log:
            log.write(f"{datetime.now()}: {message}\n")
    except Exception as e:
        print(f"[Error] Could not write to log file {log_file}: {e}")

def main() -> None:
    parser = argparse.ArgumentParser(description="Check WebSocket connection.")
    parser.add_argument("uri", help="WebSocket URI (e.g., ws://localhost:8082)")
    parser.add_argument("service", help="Name of the systemd-Services")
    parser.add_argument(
        "--state-file",
        default=DEFAULT_STATE_FILE,
        help=f"Path to state file (default: {DEFAULT_STATE_FILE})",
    )
    parser.add_argument(
        "--threshold",
        default=DEFAULT_THRESHOLD,
        type=int,
        help=f"Threshold for fail count before considering the service down (default: {DEFAULT_THRESHOLD})",
    )
    parser.add_argument(
        "--log-file",
        default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "trigger.log"),
        help="Pfad zur Logdatei"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        default=False,
        help="If set, the script will not restart the service but only check the WebSocket"
    )
    args = parser.parse_args()

    if args.threshold < 1:
        print("[Error] Threshold must be at least 1.")
        sys.exit(1)
    if args.dry_run:
        print("[Info] Running in dry run mode. No service will be restarted.")

    pid = get_service_pid(args.service)
    log_file = args.log_file

    fail_count = read_state(args.state_file, pid)
    ws_online = asyncio.run(check_websocket(args.uri))

    if ws_online:
        write_state(args.state_file, pid, 0)
        print("[Watchdog] WebSocket healthy.")
    elif fail_count == -1:
        # Initial state, WebSocket connection never succeeded, so we do not count this as a failure
        # This watchdog only starts, after the WebSocket connection has been established at least once.
        print("[Watchdog] WebSocket connection never succeeded. Exiting without failure.")
    else:
        fail_count += 1
        write_state(args.state_file, pid, fail_count)
        if fail_count >= args.threshold:
            if args.dry_run:
                print(f"[Watchdog] Threshold exceeded! (Dry run mode, not restarting {args.service}).")
                write_state(args.state_file, pid, -1) # Still reset, to avoid flooding the log
                write_log(log_file, f"Dry run: Triggered restart! (fail count: {fail_count})")
            else:
                print(f"[Watchdog] Threshold exceeded! Restarting {args.service}.")
                write_state(args.state_file, pid, -1)
                write_log(log_file, f"Triggered restart!")
                restart_service(args.service)
        else:
            print(f"[Watchdog] WebSocket not working! ({fail_count}/{args.threshold}).")

if __name__ == '__main__':
    main()
