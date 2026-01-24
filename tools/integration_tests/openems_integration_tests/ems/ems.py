from datetime import timedelta
from enum import Enum
from typing import Tuple, Any, Optional, Callable

import polling
import requests
from dataclasses import dataclass

from openems_integration_tests.ems.felix import ApacheFelixClient


@dataclass
class ChannelResponse:
    # channel address in format componentName/channelName
    address: str
    # data type (INTEGER, STRING, ...)
    type: str
    # accessMode (RO, RW)
    accessMode: str
    # text that describes the channel
    text: str
    # unit like kWH, A, ... if applicable. empty string if not
    unit: str
    # the actual value
    value: any

    def __post_init__(self):
        if self.type == "BOOLEAN":
            self.value = bool(self.value)


class EmsState(Enum):
    OK = 0
    INFO = 1
    WARNING = 2
    FAULT = 3


class EmsClient:
    def __init__(self, url: str, auth: Tuple[str, str]):
        self.url = url
        self.auth = auth

    def read_channel(self, component: str, channel: str) -> ChannelResponse:
        response = requests.get(f"{self.url}/channel/{component}/{channel}", auth=self.auth)
        response.raise_for_status()
        return ChannelResponse(**response.json())

    def write_channel(self, component: str, channel: str, value: Any) -> None:
        response = requests.post(
            f"{self.url}/channel/{component}/{channel}",
            json={"value": value},
            auth=self.auth,
        )
        response.raise_for_status()

    def get_active_power(self, ess="ess0") -> int:
        # TODO configure ess id dynamically
        return self.read_channel(ess, "ActivePower").value

    def get_state(self) -> EmsState:
        return EmsState(self.read_channel("_sum", "State").value)

    def get_soc(self, ess="ess0") -> int:
        # TODO configure ess id dynamically
        return self.read_channel(ess, "Soc").value

    def wait_for_soc(
        self,
        desired_soc: int,
        timeout: timedelta,
        ess="ess0",
        exact_stop: bool = False,
        side_condition: Optional[Callable[[], bool]] = None,
    ) -> None:
        """
        This method waits until the battery reaches the desired soc.
        :param desired_soc: The desired soc to reach
        :param timeout: How long to wait until soc is reached
        :param exact_stop: If this is set to true, the function verifies that the soc reaches exactly the desired soc.
                           If it's set to false, the function verifies, that the soc passes the desired soc.
        :param side_condition: Expression that can execute any kind of assertion. If none is specified, it's asserted
                               that the EMS system is in an OK state.
        """
        soc = self.get_soc(ess=ess)
        verify_soc = lambda: True
        if soc < desired_soc:
            verify_soc = lambda: self.get_soc(ess=ess) >= desired_soc
        elif soc > desired_soc:
            verify_soc = lambda: self.get_soc(ess=ess) <= desired_soc

        def wait_for():
            if side_condition is None:
                current_state = self.get_state()
                assert EmsState.OK == current_state, f"Expected EMS state to be OK, but got {current_state}"
            else:
                side_condition()
            return verify_soc()

        polling.poll(wait_for, step=1, timeout=timeout.total_seconds())
        if exact_stop:
            assert desired_soc == self.get_soc(ess=ess)

    def set_production_power(self, power: int):
        self.write_channel("simulateProduction", "Data", power)

    def set_consumption_power(self, power: int):
        self.write_channel("simulateConsumption", "Data", power)
