# State-Machine

```mermaid
graph TD
%% =========================================================
%% Entry
%% =========================================================
    IDLE -->|" External trigger "| PREPARE

%% =========================================================
%% Predeclare nodes (layout control)
%% =========================================================
    PREPARE
    CHECK_BALANCING

    REF_DECISION
    REFERENCE_CYCLE_CHARGING
    REFERENCE_CYCLE_CHARGING_WAIT
    REFERENCE_CYCLE_DISCHARGING
    REFERENCE_CYCLE_DISCHARGING_WAIT

    MEASUREMENT_CYCLE_CHARGING
    MEASUREMENT_CYCLE_CHARGING_WAIT
    MEASUREMENT_CYCLE_DISCHARGING
    MEASUREMENT_CYCLE_DISCHARGING_WAIT

    EVALUATE_RESULT
    DONE
    ERROR_ABORT

%% =========================================================
%% Prepare Reference (initial SoC normalization)
%% =========================================================
    PREPARE -->|" SoC > 0% "| PREPARE
    PREPARE -->|" SoC == 0% "| REF_DECISION

%% =========================================================
%% Optional Reference Cycle switch
%% =========================================================
    REF_DECISION -->|" ReferenceCycle enabled "| REFERENCE_CYCLE_CHARGING
    REF_DECISION -->|" ReferenceCycle disabled "| MEASUREMENT_CYCLE_CHARGING

%% =========================================================
%% Reference Cycle (optional conditioning only)
%% =========================================================
    subgraph "Reference Cycle (optional conditioning)"
        REFERENCE_CYCLE_CHARGING --> REFERENCE_CYCLE_CHARGING_WAIT
        REFERENCE_CYCLE_CHARGING_WAIT -->|" Timer expired (30 min) "| REFERENCE_CYCLE_DISCHARGING
        REFERENCE_CYCLE_DISCHARGING --> REFERENCE_CYCLE_DISCHARGING_WAIT
    end

%% Transition Reference → Measurement
    REFERENCE_CYCLE_DISCHARGING_WAIT -->|" Timer expired (30 min) "| MEASUREMENT_CYCLE_CHARGING

%% =========================================================
%% Measurement Cycle (SOH relevant)
%% =========================================================
    subgraph "Measurement Cycle (SOH)"
        MEASUREMENT_CYCLE_CHARGING --> MEASUREMENT_CYCLE_CHARGING_WAIT
        MEASUREMENT_CYCLE_CHARGING_WAIT -->|" Timer expired (30 min) "| CHECK_BALANCING
        CHECK_BALANCING -->|" Balanced "| MEASUREMENT_CYCLE_DISCHARGING
        MEASUREMENT_CYCLE_DISCHARGING --> MEASUREMENT_CYCLE_DISCHARGING_WAIT
        MEASUREMENT_CYCLE_DISCHARGING_WAIT -->|" Timer expired (30 min) "| EVALUATE_RESULT
    end

%% =========================================================
%% Finalization
%% =========================================================
    EVALUATE_RESULT --> DONE
    DONE --> IDLE

%% =========================================================
%% Error Handling (global, outside subgraphs)
%% =========================================================
    PREPARE -->|" Error "| ERROR_ABORT
    REF_DECISION -->|" Error "| ERROR_ABORT
    REFERENCE_CYCLE_CHARGING -->|" Error "| ERROR_ABORT
    REFERENCE_CYCLE_CHARGING_WAIT -->|" Error "| ERROR_ABORT
    REFERENCE_CYCLE_DISCHARGING -->|" Error "| ERROR_ABORT
    REFERENCE_CYCLE_DISCHARGING_WAIT -->|" Error "| ERROR_ABORT
    MEASUREMENT_CYCLE_CHARGING -->|" Error "| ERROR_ABORT
    MEASUREMENT_CYCLE_CHARGING_WAIT -->|" Error "| ERROR_ABORT
    CHECK_BALANCING -->|" Not balanced / Timeout "| ERROR_ABORT
    MEASUREMENT_CYCLE_DISCHARGING -->|" Error "| ERROR_ABORT
    MEASUREMENT_CYCLE_DISCHARGING_WAIT -->|" Error "| ERROR_ABORT
    EVALUATE_RESULT -->|" Error "| ERROR_ABORT
    ERROR_ABORT --> IDLE


```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor

