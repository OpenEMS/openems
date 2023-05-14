# State-Machine

```mermaid
graph LR
ON_BEFORE_PROCESS_IMAGE>ON_BEFORE_PROCESS_IMAGE]
ON_EXECUTE_WRITE>ON_EXECUTE_WRITE]

ON_EXECUTE_WRITE -->|event| WRITE
INITIAL_WAIT -->|short delay| READ_BEFORE_WRITE
READ_BEFORE_WRITE -->|ReadTasks finished too early| WAIT_FOR_WRITE
WAIT_FOR_WRITE -.- WRITE

INITIAL_WAIT -.->|long delay| WRITE

WRITE -->|WriteTasks finished| WAIT_BEFORE_READ
WAIT_BEFORE_READ -->|finished sleeping| READ_AFTER_WRITE
READ_AFTER_WRITE -->|ReadTasks finished| FINISHED

ON_BEFORE_PROCESS_IMAGE -->|event| INITIAL_WAIT
FINISHED -.- INITIAL_WAIT
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor