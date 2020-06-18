# State-Machine

```mermaid
graph LR
Undefined -->|target START| StartBattery

StartBattery -->|not timeout| StartBattery
StartBattery -->|isStarted| StartBatteryInverter
StartBattery -->|timeout| Undefined

StartBatteryInverter -->|not timeout| StartBatteryInverter
StartBatteryInverter -->|isStarted| Started
StartBatteryInverter -->|timeout| Undefined

Started -->|Battery+BatteryInverter.isStarted && everythingOk| Started
Started -->|otherwise| Undefined

Undefined -->|target STOP| StopBatteryInverter

StopBatteryInverter -->|not timeout| StopBatteryInverter
StopBatteryInverter -->|isStarted| StopBattery
StopBatteryInverter -->|timeout| Undefined

StopBattery -->|not timeout| StopBattery
StopBattery -->|isStopped| Stopped
StopBattery -->|timeout| Undefined

Stopped -->|Battery+BatteryInverter.isStopped && everythingOk| Stopped
Stopped -->|otherwise| Undefined

Undefined -->|hasFault| ErrorHandling
ErrorHandling -->|not timeout| ErrorHandling
ErrorHandling -->|eventually| Undefined
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor