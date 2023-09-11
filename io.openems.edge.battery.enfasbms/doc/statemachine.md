# State-Machine

```mermaid
graph TD
A[Undefined] -->|Battery has faults, More then 30sec, BMS_ERROR_STATE, BMS_SAFE_STATE | B[Error]
A --> |BMS_OFFLINE_STATE|E[GO_STOPPED]
A --> |BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE| D[Running]
A --> |BMS_SLEEP_STATE, UNDEFINED| A

E --> | Current <= 1A or Current = undefined, then COMMAND_OPEN_CONTACTORS, then wait for BMS_OFFLINE_STATE| F[STOPPED] 
E --> |WAIT for 120 SEC, if current is not reduced| B

D --> |BMS_ERROR_STATE, BMS_SAFE_STATE| B
D --> |BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE|D
D --> |BMS_OFFLINE_STATE| C 
D --> |StopStartTarget is STOP| E

C[Go_running] --> |COMMAND_CLOSE_CONTACTORS, BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE| D
C --> |battery has faults, More then 30sec, BMS_ERROR_STATE, BMS_SAFE_STATE| B
C --> |BMS_SLEEP_STATE, UNDEFINED, BMS_INITIALIZATION, BMS_OFFLINE_STATE| C

F --> |StopStartTarget is START| C
F --> |battery has faults, | B
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor