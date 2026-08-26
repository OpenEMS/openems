from enum import Enum

from openems_integration_tests.ems.ems import EmsClient


class InverterState(Enum):
    UNDEFINED = -1
    GO_RUNNING = 10
    OK = 11
    GO_STOPPED = 20
    STOPPED = 21
    ERROR = 30


class InverterClient:
    def __init__(self, component_id: str, ems_cli: EmsClient):
        self.component_id = component_id
        self.ems_cli = ems_cli

    def get_state(self) -> InverterState:
        return InverterState(self.ems_cli.read_channel(self.component_id, "StateMachine").value)

    def ip(self) -> str:
        pass

    def port(self) -> str:
        pass
