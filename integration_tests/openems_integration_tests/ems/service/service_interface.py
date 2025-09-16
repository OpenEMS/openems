from abc import ABC, abstractmethod


class OpenEmsService(ABC):
    """Interface for OpenEMS service management."""

    @abstractmethod
    def start(self) -> None:
        """Start the OpenEMS service."""
        pass

    @abstractmethod
    def stop(self) -> None:
        """Stop the OpenEMS service."""
        pass

    @abstractmethod
    def restart(self) -> None:
        """Restart the OpenEMS service."""
        pass

    @abstractmethod
    def status(self) -> bool:
        """Check if the OpenEMS service is running.

        Returns:
            bool: True if the service is running, False otherwise.
        """
        pass
