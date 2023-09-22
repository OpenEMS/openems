# State-Machine

```mermaid
stateDiagram-v2
direction LR

state Undefined {
    state "wait for batteries status" as Undefined_waitForBatteriesStatus
	state Undefined_checkAllBatteriesRunning <<choice>>
	state Undefined_checkAllBatteriesStopped <<choice>>
	state "→ Error (TimeoutWaitForBatteriesStatus)" as Undefined_error
	state "→ Running" as Undefined_running
	state "→ Stopped" as Undefined_stopped
	state "→ GoStopped" as Undefined_goStopped
	[*] --> Undefined_waitForBatteriesStatus
	Undefined_waitForBatteriesStatus --> Undefined_checkAllBatteriesRunning: batteries status defined
	Undefined_waitForBatteriesStatus --> Undefined_error: t > 20 s
	Undefined_checkAllBatteriesRunning --> Undefined_checkAllBatteriesStopped: else
	Undefined_checkAllBatteriesRunning --> Undefined_running: all batteries == running
	Undefined_checkAllBatteriesStopped --> Undefined_goStopped: else
	Undefined_checkAllBatteriesStopped --> Undefined_stopped: all batteries == stopped
}
[*] --> Undefined

state GoStopped {
	state "stop all batteries" as GoStopped_stopBatteries
    state "→ Error (TimeoutStopBatteries)" as GoStopped_error
	state "→ Stopped" as GoStopped_stopped
	[*] --> GoStopped_stopBatteries
    GoStopped_stopBatteries --> GoStopped_stopped: all batteries == stopped
	GoStopped_stopBatteries --> GoStopped_error: t > ... s
}
Undefined --> GoStopped
Running --> GoStopped: Target = STOP
GoRunning --> GoStopped: Target = STOP
Error --> GoStopped: hasFaults() == false

state GoRunning {
	state "start batteries (locked)" as GoRunning_startBatteries
    state "wait for unlock (Timeout paused)" as GoRunning_waitForUnlock
	state "unlock batteries one by one" as GoRunning_unlockBatteriesOneByOne
    state "→ Running" as GoRunning_running
	[*] --> GoRunning_startBatteries
	GoRunning_startBatteries --> GoRunning_waitForUnlock: all batteries have values
	GoRunning_waitForUnlock --> GoRunning_unlockBatteriesOneByOne: Cluster Contactors unlocked
    GoRunning_unlockBatteriesOneByOne --> GoRunning_running: all batteries started
}
Stopped --> GoRunning: Target = START
note right of GoRunning
Transient to "Go Running" necessary, because
it's possible that the battery should stopped
with locked Contactors
end note

state Stopped
Undefined --> Stopped
GoStopped --> Stopped

state Running
Undefined --> Running
GoRunning --> Running

note right of Error
    Additional Error checks:
    - At least one battery in Error
end note

note right of Error_do
    Battery Error
    → heal if status[x] != Error && BatteriesStatusDefined
    → reaction: reset error "AtLeastOneBatteryInError"
end note

state Error{
    state "on entry: stop all batteries" as Error_onEntry
    state "do: fault Healing" as Error_do
}
Running --> Error: hasFaults()
GoRunning --> Error: hasFaults()
Stopped --> Error: hasFaults()
GoStopped --> Error: hasFaults()
Undefined --> Error: hasFaults()
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor