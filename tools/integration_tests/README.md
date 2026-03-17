# Integration Tests for OpenEMS

This package provides an integration test framework and integration tests for OpenEMS.
It's in a very early stage and currently only target to edge specific integration tests.

## Requirements
This project requires [poetry](https://python-poetry.org) to run.

The openems project must have a build jar file (with `./gradlew buildEdge` executed in the openems directory) so the
integration test can start and stop the ems service as desired.

Each time you update the project `pyproject.toml` file or when you setup the project initially execute:
```
poetry install
```

## Usage
Integration tests take care about starting/stopping the EMS service. So don't run another instance of the EMS service
while executing the integration tests.

By default, this package assumes you're using a Voltfang OpenEMS environment. You can override this by setting the
`EMS_INTEGRATION_TEST_ENVIRONMENT` environment variable to a value defined in 
`openems_integration_tests.conftest.ExecutionEnvironment` which are:
- `DEVELOPER` - Vanilla OpenEMS environment

You can execute tests with
```
poetry run pytest openems_integration_tests
```

To select specific tests by name you can execute
```
# Execute all tests in the power_solver module
poetry run pytest openems_integration_tests -k power_solver
# Test smoke tests only
poetry run pytest openems_integration_tests -k smoke
```

Example output:
```
s> poetry run pytest openems_integration_tests -k power_solver
=================================================================================== test session starts ===================================================================================
platform win32 -- Python 3.12.10, pytest-8.4.1, pluggy-1.6.0
rootdir: C:\Users\FelixRemmel\development\original_openems\integration_tests
configfile: pyproject.toml
collected 5 items / 3 deselected / 2 selected

openems_integration_tests\tests\edge\ems\power_solver\test_keep_all_equals.py ..                                                                                                     [100%]

======================================================================= 2 passed, 3 deselected in 77.33s (0:01:17) ========================================================================
```

## Development
To write a new integration test, place it into the `openems_integration_tests/tests/` module or any submodule of it and name the file `test_[some_name_in_snake_case].py`.
You should get familiar with [pytest](https://docs.pytest.org) and expecially how to use [fixtures](https://docs.pytest.org/en/stable/how-to/fixtures.html).

If your test method looks like the following, you get an EMS Client for free - meaning you don't have to care about initialization and startup of OpenEMS.
```python
from openems_integration_tests.ems.ems import EmsClient, EmsState

def test_ems_default_config_is_ok(ems_cli: EmsClient):
    assert ems_cli.get_state() == EmsState.OK
```

Most tests will require specific configuration of the EMS. The default configuration is located in `openems_integration_tests/ems_configurations/default`.
If you want to use another configuration you can create it in that folder and annotate your test with the `@pytest.mark.ems_config.with_args("relative_path_to_config_based_on_ems_configurations_directory")`.
Most of the time you want to extend from the default config, so you don't have to create basic configurations again and again in each folder.
Just add the file `EXTEND_DEFAULT_CONFIG` in your config directory and then the test framework takes care about the rest.
A test with a specific test configuration then looks like the following:
```python
import pytest
from openems_integration_tests.ems.ems import EmsClient

# yeah it's a bit of non sense, but it should show the concept. 
# We load the default config again now, but at least we explictly configured
# the folder openems_integration_tests/ems_configurations/default now ;).
# Change it to any folder placed in openems_integration_tests/ems_configurations
@pytest.mark.ems_config.with_args("default")
def your_example_test(ems_cli: EmsClient):
    ems_cli.set_consumption_power(5000)
    assert ems_cli.get_active_power() == 0
```
