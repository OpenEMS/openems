# State-Machine

```mermaid
graph TD

Undefined -->|soc is not defined or start time is not initialized| Undefined
Undefined -->|CycleOrder : Start with Charge | StartCharge
Undefined -->|CycleOrder : Auto and if soc > 50| StartCharge
Undefined -->|CycleOrder : Start with Discharge| StartDischarge
Undefined -->|CycleOrder : Auto and if soc < 50| StartDischarge


StartCharge -->|MaxSoc == 100 && AllowedChargePower == 0 | ContinueWithDischarge
StartCharge -->|MaxSoc != 100 && MaxSoc == CurrentSoc | ContinueWithDischarge

StartDischarge -->|MinSoc == 100 && AllowedChargePower == 0 | ContinueWithCharge
StartDischarge -->|MinSoc != 100 && MinSoc == CurrentSoc | ContinueWithCharge

ContinueWithDischarge --> |Stopped with discharging| CompletedCycle
ContinueWithCharge --> |Stopped with charging| CompletedCycle

CompletedCycle --> | # of cycles completed| FinalSoc
CompletedCycle --> | cycle are not completed| StartCharge
CompletedCycle --> | # of cycles not completed| StartDischarge


FinalSoc --> |finalSoc reached| Finished
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor