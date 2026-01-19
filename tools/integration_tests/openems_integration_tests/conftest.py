import os
import pathlib
import shutil
from dataclasses import dataclass
from enum import Enum
from io import UnsupportedOperation

import polling
import pytest
import requests.exceptions

import openems_integration_tests
from openems_integration_tests.ems.ems import EmsClient, EmsState
from openems_integration_tests.ems.felix import ApacheFelixClient
from openems_integration_tests.ems.inverter import InverterClient
from openems_integration_tests.ems.service.service_factory import get_openems_service, ServiceType
from openems_integration_tests.ems.service.service_interface import OpenEmsService

EMS_CONFIG_BASE_DIR = "ems_configurations"
DEFAULT_EMS_CONFIG = "default"
DELETE_CONFIG_FILE_PREFIX = "delete_"


@dataclass
class ExecutionEnvironmentMixin:
    ems_url: str
    ems_config_dir: str
    felix_url: str


# TODO make this somehow configurable so it's usable for the open source project as well.
# Best way is probably to make this importable depending on the environment it's running in - indicated by an
# environment variable. Then a file name is derived and importet depending on the environment variable
class ExecutionEnvironment(ExecutionEnvironmentMixin, Enum):
    DEVELOPER = (
        "http://localhost:8084/rest",
        pathlib.Path("/openems/integration_test_config").resolve(),
        "http://localhost:8080",
    )
    # TODO add stages such as beta (develop branch state), prod (main branch state)


@pytest.fixture
def execution_environment() -> ExecutionEnvironment:
    return ExecutionEnvironment[os.environ.get("EMS_INTEGRATION_TEST_ENVIRONMENT", "DEVELOPER")]


@pytest.fixture
def ems_cli(execution_environment: ExecutionEnvironment):
    return EmsClient(
        url=execution_environment.ems_url,
        auth=("admin", "admin"),
    )


@pytest.fixture()
def felix_cli(execution_environment: ExecutionEnvironment):
    return ApacheFelixClient(base_url=execution_environment.felix_url, username="admin", password="admin")


@pytest.fixture
def inverter(ems_cli: EmsClient):
    return InverterClient(component_id="batteryInverter0", ems_cli=ems_cli)


@pytest.fixture(autouse=True)
def setup_ems_config(
    request,
    execution_environment: ExecutionEnvironment,
    felix_cli: ApacheFelixClient,
    ems_cli: EmsClient,
    openems_service: OpenEmsService,
):
    # TODO should we evaluate EMS state in between test runs somehow?
    # pro: enforce that every test cleans up after itself
    # con: more work for every test case - some auto cleanup possible? So indirectly clean up? Maybe only required for
    #      hardware related tests? Simulations are reset anyway and new test is as a new environment..
    prepare_configs(request.keywords, execution_environment)
    try:
        openems_service.start()

        polling.poll(
            lambda: ems_cli.get_state() == EmsState.OK,
            step=1,
            timeout=120,
            ignore_exceptions=(requests.exceptions.ConnectionError, requests.exceptions.HTTPError),
        )

        yield  # this starts the actual test
    finally:
        openems_service.stop()


def prepare_configs(request_keywords: dict, execution_environment: ExecutionEnvironment):
    config = get_config_dir(DEFAULT_EMS_CONFIG)
    if "ems_config" in request_keywords:
        ems_config_marker = request_keywords["ems_config"]
        if len(ems_config_marker.args) != 1:
            raise ValueError(
                "Invalid arguments for marker ems_config. Length of args must be exactly one, which "
                "references to the config dir to use."
            )
        config = get_config_dir(ems_config_marker.args[0])
    target_dir = os.environ.get("EMS_CONFIG_DIR", execution_environment.ems_config_dir)
    if target_dir is None:
        raise UnsupportedOperation(
            f"Auto config directory configuration not supported for execution environment "
            f"{execution_environment}. Please set environment variable EMS_CONFIG_DIR"
        )
    if os.path.exists(target_dir):
        shutil.rmtree(target_dir)
    if os.path.exists(os.path.join(config, "EXTEND_DEFAULT_CONFIG")):
        # config default config first and then overwrite with configured config
        shutil.copytree(get_config_dir(DEFAULT_EMS_CONFIG), target_dir)
    shutil.copytree(config, target_dir, dirs_exist_ok=True)

    delete_marked_config_files(target_dir)


def delete_marked_config_files(target_dir):
    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if file.startswith(DELETE_CONFIG_FILE_PREFIX):
                os.remove(os.path.join(root, file))
                os.remove(os.path.join(root, file[len(DELETE_CONFIG_FILE_PREFIX) :]))


def get_config_dir(config_name: str) -> str:
    return os.path.join(
        os.path.dirname(os.path.join(openems_integration_tests.__file__)),
        EMS_CONFIG_BASE_DIR,
        config_name,
    )


def pytest_addoption(parser):
    parser.addoption(
        "--run-on-hardware",
        action="store_true",
        default=False,
        help="Run tests that require hardware",
    )


@pytest.fixture
def openems_service(execution_environment: ExecutionEnvironment) -> OpenEmsService:
    """Fixture to provide an OpenEMS service implementation."""
    # Determine service type based on environment
    if execution_environment in [ExecutionEnvironment.DEVELOPER]:
        # For local development, use the local process implementation
        service = get_openems_service(ServiceType.LOCAL, java_opts=f"-Dfelix.cm.dir={execution_environment.ems_config_dir}")
    else:
        # For other environments, use systemd
        service = get_openems_service(ServiceType.SYSTEMD)

    yield service
