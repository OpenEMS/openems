# State-Machine

```mermaid
graph LR
Undefined -->|"Min-Cell-Voltage <= Start-Charge-Below (2850)"| WaitForForce

WaitForForce -->|"Min-Cell-Voltage > Start-Charge-Below (2850)"| Undefined
WaitForForce -->|"waited 60 seconds"| Force

Force -->|"Min-Cell-Voltage > Charge-Below (2910)"| Block

Block -->|"Min-Cell-Voltage > Start-Charge-Below (2850)"| Force
Block -->|"Min-Cell-Voltage > Block-Discharge-Below (3000)"| Undefined
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor