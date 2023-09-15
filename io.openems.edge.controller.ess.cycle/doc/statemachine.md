# State-Machine

```mermaid
graph TD

Undefined -->|SoC or StartTime not available| Undefined
Undefined -->|CycleOrder<br/>'Start with Charge' | StartCharge
Undefined -->|CycleOrder<br/>'Auto'<br/>&& SoC => 50| StartCharge
Undefined -->|CycleOrder<br/>'Start with Discharge' | StartDischarge
Undefined -->|CycleOrder<br/>'Auto'<br/>&& SoC < 50| StartDischarge

StartCharge -->|Reached 'MaxSoc'<br/>or 'MaxSoc' == 100 && AllowedChargePower == 0| ContinueWithDischarge
StartDischarge -->|Reached 'MinSoc'<br/>or 'MinSoc' == 0 && AllowedDischargePower == 0 | ContinueWithCharge

ContinueWithDischarge --> |Finished discharging| CompletedCycle
ContinueWithCharge --> |Finished charging| CompletedCycle

CompletedCycle --> |Reached 'totalCycleNumber'| FinalSoc
CompletedCycle --> |CycleOrder<br/>'Start with Charge' | StartCharge
CompletedCycle --> |CycleOrder<br/>'Start with Discharge' | StartDischarge

FinalSoc --> |Charge/Discharge to 'finalSoc'| FinalSoc
FinalSoc --> |Reached 'finalSoc'| Finished
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor