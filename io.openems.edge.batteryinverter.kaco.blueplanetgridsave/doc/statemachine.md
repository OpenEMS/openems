# State-Machine

```mermaid
graph LR
Undefined --> |hasFault| Error
Undefined --> |isStarted or Grid Connected| Running
Undefined --> |isStopped or Off, Standby, Pre-charge| Stopped
Undefined --> |else| Go_Stopped

Go_Stopped --> |Off or Standby or Pre-charge| Stopped
Go_Stopped --> |hasFault or timeout| Error
Go_Stopped --> |try for 240 second| Go_Stopped

Stopped --> |targetStart| Go_Running
Stopped --> |hasFault| Error

Go_Running --> |targetStop| Go_Stopped
Go_Running --> |hasFault or timeout| Error
Go_Running --> |try for 240 second| Go_Running
Go_Running --> |Grid Connected or Throttled| Running

Running --> |hasFault| Error
Running --> |targetStop| Go_Stopped 

Error --> |!hasFault| Stopped
Error --> |hasFault| Error
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor