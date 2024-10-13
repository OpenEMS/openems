# State-Machine

```mermaid
graph TD
start --> Undefined
Undefined --> |condition: Inside set time, task :  Charge to maintain soc| PreActivateState
PreActivateState --> |condition: Outside set time, task :  do nothing| Undefined
PreActivateState --> |condition: grid freq > freqlimit, task :  setpower  0Watt| ActivationTime
ActivationTime --> |condition: 1.7 sec, task : discharge setActivepower| SupportDuration
SupportDuration --> |condition: 30 sec, task : discharge setActivepower| DeactivationTime
DeactivationTime -->|condition: 1.7 sec, task : setpower  0Watt| BufferedTime 
BufferedTime --> |condition: 10 sec, task : setpower for 0Watt| BufferedSupportTime
BufferedSupportTime --> |condition: 15 min, Charge to maintain soc| RecoveryTime
RecoveryTime --> PreActivateState
RecoveryTime -->|condition: Outside set time, task :  do nothing| Undefined
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor