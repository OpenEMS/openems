# Sequence Diagram

```mermaid
sequenceDiagram
    participant cluster as EVSE Cluster Controller
    participant single0 as EVSE Single Controller 1
    participant cp0 as Charge-Point 1
    participant ev0 as Car 1
    cluster->>+single0: getParams()
    single0->>+cp0: getStatus()
    single0->>+cp0: getChargeParams()
    single0->>+ev0: getChargeParams()
    single0->>+single0: mergeLimits()
    cluster->>+single0: applyCharge()
    single0->>+ev0: applyCharge()
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor