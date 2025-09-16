from openems_integration_tests.ems.ems import EmsClient, EmsState


def test_ems_default_config_is_ok(ems_cli: EmsClient):
    assert ems_cli.get_state() == EmsState.OK
