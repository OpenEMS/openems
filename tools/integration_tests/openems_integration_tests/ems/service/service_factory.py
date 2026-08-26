from enum import Enum

from openems_integration_tests.ems.service.service_interface import OpenEmsService
from openems_integration_tests.ems.service.local_service import LocalOpenEmsService
from openems_integration_tests.ems.service.systemd_service import SystemdOpenEmsService


class ServiceType(Enum):
    LOCAL = "local"
    SYSTEMD = "systemd"


def get_openems_service(service_type: ServiceType, **kwargs) -> OpenEmsService:
    """Factory method to get the appropriate OpenEMS service implementation.

    Args:
        service_type: Type of service to create. If None, determined from environment.
        **kwargs: Additional arguments to pass to the service constructor.

    Returns:
        OpenEmsService: An implementation of the OpenEmsService interface.
    """
    if service_type == ServiceType.LOCAL:
        return LocalOpenEmsService(**kwargs)
    elif service_type == ServiceType.SYSTEMD:
        return SystemdOpenEmsService(**kwargs)
    else:
        raise ValueError(f"Unknown service type: {service_type}")
