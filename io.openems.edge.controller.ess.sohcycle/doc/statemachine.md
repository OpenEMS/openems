
# State Machine

## High-level SoH flow

```mermaid
flowchart TD
    IDLE --> PREPARE

    PREPARE -->|"SoC = 0%"| START_FLOW
    PREPARE -->|" Discharge blocked; recovery unused "| PREPARE_PRECHARGE
    PREPARE -->|" After recovery: blocked for 5 min "| START_FLOW
    PREPARE -->|" Continue discharging towards 0%SoC "| PREPARE
    PREPARE_PRECHARGE -->|" Discharge capability restored "| PREPARE
    PREPARE_PRECHARGE -->|" 30 min timeout; abort SoH cycle "| ERROR_ABORT

    START_FLOW -->|"Reference enabled"| REFERENCE_CYCLE_CHARGING
    START_FLOW -->|"Reference disabled"| MEASUREMENT_CYCLE_CHARGING
    REFERENCE_CYCLE_CHARGING -->|" Endpoint reached or accepted "| REFERENCE_CYCLE_CHARGING_WAIT
    REFERENCE_CYCLE_CHARGING_WAIT -->|" 30 min rest "| REFERENCE_CYCLE_DISCHARGING
    REFERENCE_CYCLE_DISCHARGING -->|" Endpoint reached or accepted "| REFERENCE_CYCLE_DISCHARGING_WAIT
    REFERENCE_CYCLE_DISCHARGING_WAIT -->|" 30 min rest "| MEASUREMENT_CYCLE_CHARGING
    MEASUREMENT_CYCLE_CHARGING -->|" Endpoint reached or accepted "| MEASUREMENT_CYCLE_CHARGING_WAIT
    MEASUREMENT_CYCLE_CHARGING_WAIT -->|" 30 min rest "| CHECK_BALANCING
    CHECK_BALANCING --> MEASUREMENT_CYCLE_DISCHARGING
    MEASUREMENT_CYCLE_DISCHARGING -->|" Endpoint reached or accepted "| MEASUREMENT_CYCLE_DISCHARGING_WAIT
    MEASUREMENT_CYCLE_DISCHARGING_WAIT -->|" 30 min rest "| EVALUATE_RESULT

    EVALUATE_RESULT --> DONE
    DONE --> IDLE
    OTHER_FATAL_ERROR["Fatal error"] --> ERROR_ABORT
    ERROR_ABORT --> IDLE
```

Each WAIT state provides a 30-minute battery rest with a 0 W battery-power target. DC production is taken into account
when calculating the ESS active-power setpoint. The power ramp is synchronized with the zero battery-power target so
that the next active phase ramps up from zero.

`PREPARE_PRECHARGE` is attempted at most once per SoH run. If discharge capability is restored within 30 minutes,
the controller returns immediately to `PREPARE`. Otherwise, the cycle aborts.

After successful recovery, if discharge remains continuously blocked for five minutes in `PREPARE` before
reaching 0% SoC, the current SoC is accepted as the lower endpoint and the cycle continues.

If discharge capability remains unavailable for 30 minutes, the SoH cycle transitions to `ERROR_ABORT`. Recovery at the
timeout boundary takes priority. Missing required ESS data can also cause `ERROR_ABORT`.

If discharge capability remains unavailable after five minutes, the current SoC is accepted as the lower endpoint and
the cycle continues, provided the required ESS data is valid. Recovery at the timeout boundary takes priority. Missing
required ESS data can still cause `ERROR_ABORT`.

## Shared five-minute endpoint rule

Reference and Measurement charging/discharging phases use the following rule when the nominal SoC endpoint cannot be
reached.

```mermaid
flowchart TD
    PHASE["Reference or Measurement charge/discharge"]
    TARGET{"Nominal SoC target reached?"}
    BLOCKED{"Required AllowedPower explicitly 0?"}
    TIMER["Continue 5-minute blocking interval"]
    RECOVERED{"Capability restored or undefined?"}
    ENDPOINT["Accept current SoC as endpoint"]
    RESET["Reset interval and continue"]
    NEXT["Corresponding WAIT state"]

    PHASE --> TARGET
    TARGET -->|" Yes "| NEXT
    TARGET -->|"No"| BLOCKED
    BLOCKED -->|"No"| RESET
    BLOCKED -->|"Yes"| TIMER
    TIMER --> RECOVERED
    RECOVERED -->|"Yes"| RESET
    RECOVERED -->|" No; blocked >= 5 min "| ENDPOINT
    RECOVERED -->|" No; blocked < 5 min "| PHASE
    ENDPOINT --> NEXT
    RESET --> PHASE
```

The blocking interval counts only continuously confirmed zero capability.
A nonzero or undefined required `AllowedPower` resets the interval.

When an endpoint is accepted early, the controller requests 0 W battery power for the transition cycle, including
DC-production compensation. The subsequent WAIT state follows the normal rest behavior.

No additional precharge or opposite-direction recovery is attempted during Reference or Measurement phases.

View using Mermaid, e.g. https://mermaid.live/
