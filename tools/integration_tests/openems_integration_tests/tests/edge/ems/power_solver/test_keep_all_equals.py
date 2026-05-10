from time import sleep
from typing import List

import pytest

from openems_integration_tests.ems.ems import EmsClient


def check_power_setting(consumption_power: int, expected_ess_power: int, ems_cli: EmsClient, num_ess: int = None, expected_per_ess: List[int] = None):
    if expected_per_ess is None and num_ess is None:
        raise ValueError("Either 'num_ess' or 'expected_per_ess' must be provided.")
    elif num_ess is not None and expected_per_ess is not None:
        raise ValueError("Both 'num_ess' and 'expected_per_ess' cannot be provided at the same time.")
    elif expected_per_ess is None:
        expected_per_ess = [expected_ess_power / num_ess] * num_ess
    ems_cli.set_consumption_power(consumption_power)
    sleep(5)
    assert ems_cli.get_active_power("essCluster") == expected_ess_power
    for i in range(len(expected_per_ess)):
        assert ems_cli.get_active_power(f"ess{i}") == expected_per_ess[i]

@pytest.mark.ems_config.with_args("power_solver/keep_all_equals/keep_all_equals_success")
def test_keep_all_equals_good_case(ems_cli: EmsClient):
    num_ess = 3
    consumption_power = num_ess * 5000
    check_power_setting(consumption_power, consumption_power, ems_cli=ems_cli, num_ess=num_ess)

    consumption_power = num_ess * 5000 + 1
    expected_ess_power = num_ess * 5000
    check_power_setting(consumption_power, expected_ess_power, ems_cli=ems_cli, num_ess=num_ess)

    consumption_power = num_ess * 5000 + 2
    expected_ess_power = num_ess * 5000
    check_power_setting(consumption_power, expected_ess_power, ems_cli=ems_cli, num_ess=num_ess)

    consumption_power = num_ess * 5000 + 3
    check_power_setting(consumption_power, consumption_power, ems_cli=ems_cli, num_ess=num_ess)

    # Set a value that is too high for all ESS to handle
    check_power_setting(1000000, 100000 * num_ess, ems_cli=ems_cli, num_ess=num_ess)

@pytest.mark.ems_config.with_args("power_solver/keep_all_equals/keep_all_equals_success_different_ess_constraints")
def test_keep_all_equals_one_ess_causes_trouble_due_to_its_constraints(ems_cli: EmsClient):
    num_ess = 3
    consumption_power = 5000 * num_ess
    check_power_setting(consumption_power, consumption_power, ems_cli=ems_cli, expected_per_ess=[consumption_power - 3000, 1000, 2000])

