# State-Machine

```mermaid
flowchart LR
Undefined -->|Read-Only| Read-Only
Undefined -->|GoodWe BT| BT
Undefined -->|GoodWe ET| ET

subgraph ET
    ET-Full -->|SoC < 98| ET-Default
    ET-Default -->|Soc < 1| ET-Empty
    ET-Default -->|Soc > 99| ET-Full
    ET-Empty -->|SoC > 2| ET-Default
end
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor