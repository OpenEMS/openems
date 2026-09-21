# State-Machine

```mermaid
graph LR

    UNDEFINED -->|startStopTarget == START AND battery.isStarted| GO_RUNNING
    UNDEFINED -->|startStopTarget == START AND NOT battery.isStarted| UNDEFINED
    UNDEFINED -->|startStopTarget == STOP| GO_STOPPED
    UNDEFINED -->|hasFailure| ERROR

    GO_RUNNING -->|isRunning| RUNNING
    GO_RUNNING -->|hasFailure / timeout| ERROR

    RUNNING -->|startStopTarget == STOP| UNDEFINED
    RUNNING -->|NOT battery.isStarted| UNDEFINED
    RUNNING -->|hasFailure| ERROR
    RUNNING -->|NOT isRunning| ERROR

    GO_STOPPED -->|NOT battery.isStarted AND NOT isRunning| STOPPED
    GO_STOPPED -->|isInStandby or isShutdown| STOPPED
    GO_STOPPED -->|hasFailure / timeout| ERROR

    STOPPED -->|startStopTarget == START| UNDEFINED
    STOPPED -->|hasFaults| ERROR

    ERROR -->|hasFailure cleared| UNDEFINED
```
View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor