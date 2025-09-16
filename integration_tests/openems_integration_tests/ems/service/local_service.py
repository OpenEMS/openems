import logging
import os
import subprocess
import signal
import sys
import time
from typing import Optional
from pathlib import Path

from openems_integration_tests.ems.service.service_interface import OpenEmsService


class LocalOpenEmsService(OpenEmsService):
    """OpenEMS service implementation that manages a local Java process."""

    def __init__(self, jar_path: Optional[str] = None, java_opts: str = ""):
        """Initialize the local OpenEMS service.

        Args:
            jar_path: Path to the OpenEMS Edge JAR file
            java_opts: Additional Java options to pass to the JVM
        """
        if jar_path is None:
            # Get the base directory of the project and append the relative path
            projects_dir = Path(__file__).resolve().parent.parent.parent.parent.parent
            jar_path = str(projects_dir / "build/openems-edge.jar")
        self.jar_path = Path(jar_path).resolve()
        self.java_opts = java_opts
        self.process: Optional[subprocess.Popen] = None

    def start(self) -> None:
        """Start the OpenEMS service by executing the JAR file."""
        if self.status():
            logging.info("OpenEMS is already running")
            return

        cmd = f"java {self.java_opts} -jar {self.jar_path}"
        logging.info(f"Starting OpenEMS with command: {cmd}")
        self.process = subprocess.Popen(
            cmd.split(),
            stdout=subprocess.DEVNULL if os.environ.get("HIDE_OPENEMS_LOGS", "false").lower() == "true" else None,
            stderr=subprocess.STDOUT,
            text=True,
            preexec_fn=os.setsid if os.name != "nt" else None,
            creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0,
        )

        # Wait a moment to ensure the process starts
        time.sleep(2)

        if not self.status():
            stdout = "Process not initialized"
            if self.process:
                stdout, _ = self.process.communicate(timeout=1)
            raise RuntimeError(f"Failed to start OpenEMS: {stdout}")

        logging.info(f"OpenEMS started with PID {self.process.pid}")

    def stop(self) -> None:
        """Stop the OpenEMS service by terminating the process."""
        if not self.status():
            logging.info("OpenEMS is not running")
            return

        if self.process:
            if os.name == "nt":
                # Windows
                self.process.send_signal(signal.CTRL_BREAK_EVENT)
            else:
                # Unix-like
                os.killpg(os.getpgid(self.process.pid), signal.SIGTERM)

            try:
                self.process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                logging.info("OpenEMS did not terminate gracefully, forcing termination")
                if os.name == "nt":
                    self.process.kill()
                else:
                    os.killpg(os.getpgid(self.process.pid), signal.SIGKILL)

            self.process = None
            logging.info("OpenEMS stopped")

    def restart(self) -> None:
        """Restart the OpenEMS service."""
        self.stop()
        time.sleep(2)  # Give it a moment to fully stop
        self.start()

    def status(self) -> bool:
        """Check if the OpenEMS service is running.

        Returns:
            bool: True if the service is running, False otherwise.
        """
        if self.process is None:
            return False

        return self.process.poll() is None
