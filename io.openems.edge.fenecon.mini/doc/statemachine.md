# State-Machine

```mermaid
graph LR
Undefined -->|target Readonly-Mode| GoReadonlyMode
Undefined -->|target Write-Mode| GoWriteMode    

GoReadonlyMode -->|not configured| ActivateEconomicMode1
ActivateEconomicMode1 -->|Set SetupMode ON| ActivateEconomicMode2
ActivateEconomicMode2 -->|Set PCS-Mode ECONOMIC| ActivateEconomicMode3
ActivateEconomicMode3 -->|Set Setup-Mode OFF| ActivateEconomicMode4
ActivateEconomicMode4 -->|Setup-Mode is OFF| GoReadonlyMode

GoReadonlyMode -->|configured| ReadonlyMode

GoWriteMode -->|not configured| ActivateDebugMode1
ActivateDebugMode1 -->|Set SetupMode ON| ActivateDebugMode2
ActivateDebugMode2 -->|Set PCS-Mode DEBUG| ActivateDebugMode3

ActivateDebugMode3 -->|Set Setup-Mode OFF| ActivateDebugMode4
ActivateDebugMode4 -->|Setup-Mode is OFF| GoWriteMode

GoWriteMode -->|configured| WriteMode
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor