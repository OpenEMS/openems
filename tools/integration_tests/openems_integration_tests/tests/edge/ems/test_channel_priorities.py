import dataclasses
from time import sleep
from typing import Optional

import pytest

from openems_integration_tests.ems.ems import EmsClient


@pytest.mark.ems_config.with_args("channel_priorities/two_competing_controllers")
def test_channel_priority(ems_cli: EmsClient):
    first_api = EmsClient("http://localhost:9000/rest", ("admin", "admin"))
    second_api = EmsClient("http://localhost:9001/rest", ("admin", "admin"))

    def assert_competing_channel_values(first_value, second_value, expected_value):
        first_api.write_channel("io0", "InputOutput0", first_value)
        second_api.write_channel("io0", "InputOutput0", second_value)
        # wait for two cycles, first cycle for api controller to propagate, second cycle for io component to set value
        sleep(2)
        # Test that channels with higher priority are evaluated first
        assert ems_cli.read_channel("io0", "InputOutput0").value == expected_value
        sleep(11)  # wait for timeout of api when no value is set anymore
        assert ems_cli.read_channel("io0", "InputOutput0").value == expected_value

    assert_competing_channel_values(True, True, True)
    assert_competing_channel_values(True, False, False)
    assert_competing_channel_values(False, True, True)
    assert_competing_channel_values(False, False, False)


@pytest.mark.ems_config.with_args("channel_priorities/two_competing_controllers")
def test_ess_priority(ems_cli: EmsClient):
    first_api = EmsClient("http://localhost:9000/rest", ("admin", "admin"))
    second_api = EmsClient("http://localhost:9001/rest", ("admin", "admin"))

    @dataclasses.dataclass
    class EssPower:
        active_power_equals: Optional[int] = None
        active_power_less_or_equals: Optional[int] = None
        active_power_greater_or_equals: Optional[int] = None

    def assert_setting_ess_values(first_values: EssPower, second_values: EssPower, expected_value) -> None:
        def set_values(api: EmsClient, ess_values: EssPower) -> None:
            api.write_channel("ess0", "SetActivePowerEquals", ess_values.active_power_equals)
            api.write_channel("ess0", "SetActivePowerLessOrEquals", ess_values.active_power_less_or_equals)
            api.write_channel("ess0", "SetActivePowerGreaterOrEquals", ess_values.active_power_greater_or_equals)

        set_values(first_api, first_values)
        set_values(second_api, second_values)
        # wait for two cycles, first cycle for api controller to propagate, second cycle for ess component to set value
        sleep(2)
        assert expected_value == ems_cli.get_active_power()

    assert_setting_ess_values(EssPower(active_power_equals=10_000), EssPower(active_power_equals=5_000), 10_000)
    assert_setting_ess_values(EssPower(active_power_equals=-10_000), EssPower(active_power_equals=5_000), -10_000)
    assert_setting_ess_values(EssPower(active_power_equals=10_000), EssPower(active_power_equals=-5_000), 10_000)
    assert_setting_ess_values(
        EssPower(active_power_less_or_equals=500), EssPower(active_power_less_or_equals=-300), -300
    )
    assert_setting_ess_values(
        EssPower(active_power_greater_or_equals=500), EssPower(active_power_greater_or_equals=800), 800
    )
    assert_setting_ess_values(
        EssPower(active_power_less_or_equals=500), EssPower(active_power_greater_or_equals=800), 500
    )
    assert_setting_ess_values(EssPower(), EssPower(active_power_greater_or_equals=800), 800)
    assert_setting_ess_values(EssPower(active_power_equals=500), EssPower(active_power_less_or_equals=300), 500)
