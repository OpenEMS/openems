import subprocess
import time

from openems_integration_tests.ems.service.service_interface import OpenEmsService


# TODO This class is not yet tested. Currently integration tests are not executed with a systemd environment.
class SystemdOpenEmsService(OpenEmsService):
    """OpenEMS service implementation that uses systemd."""

    def __init__(self, service_name: str = "openems-edge"):
        """Initialize the systemd OpenEMS service.

        Args:
            service_name: Name of the systemd service
        """
        self.service_name = service_name

    def _run_systemctl_command(self, command: str) -> tuple[int, str, str]:
        """Run a systemctl command and return the result.

        Args:
            command: The systemctl command to run

        Returns:
            tuple: (return_code, stdout, stderr)
        """
        process = subprocess.Popen(
            f"systemctl {command} {self.service_name}",
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        stdout, stderr = process.communicate()
        return process.returncode, stdout, stderr

    def start(self) -> None:
        """Start the OpenEMS service using systemd."""
        if self.status():
            print(f"Service {self.service_name} is already running")
            return

        returncode, stdout, stderr = self._run_systemctl_command("start")
        if returncode != 0:
            raise RuntimeError(f"Failed to start {self.service_name}: {stderr}")

        # Wait for service to start
        for _ in range(10):
            if self.status():
                print(f"Service {self.service_name} started successfully")
                return
            time.sleep(1)

        raise RuntimeError(f"Service {self.service_name} failed to start within timeout")

    def stop(self) -> None:
        """Stop the OpenEMS service using systemd."""
        if not self.status():
            print(f"Service {self.service_name} is not running")
            return

        returncode, stdout, stderr = self._run_systemctl_command("stop")
        if returncode != 0:
            raise RuntimeError(f"Failed to stop {self.service_name}: {stderr}")

        # Wait for service to stop
        for _ in range(10):
            if not self.status():
                print(f"Service {self.service_name} stopped successfully")
                return
            time.sleep(1)

        raise RuntimeError(f"Service {self.service_name} failed to stop within timeout")

    def restart(self) -> None:
        """Restart the OpenEMS service using systemd."""
        returncode, stdout, stderr = self._run_systemctl_command("restart")
        if returncode != 0:
            raise RuntimeError(f"Failed to restart {self.service_name}: {stderr}")

        # Wait for service to restart
        for _ in range(10):
            if self.status():
                print(f"Service {self.service_name} restarted successfully")
                return
            time.sleep(1)

        raise RuntimeError(f"Service {self.service_name} failed to restart within timeout")

    def status(self) -> bool:
        """Check if the OpenEMS service is running using systemd.

        Returns:
            bool: True if the service is running, False otherwise.
        """
        returncode, stdout, stderr = self._run_systemctl_command("is-active")
        return returncode == 0
