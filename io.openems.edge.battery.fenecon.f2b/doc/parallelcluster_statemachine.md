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
    state GoRunning_hasAllBatteriesFault <<choice>>
    state GoRunning_stringVoltagesDifference <<choice>>
   	state "start batteries (locked)" as GoRunning_startBatteries
    state "start the battery which has communication" as GoRunning_startOneBattery
    state "start communication" as GoRunning_startCommunication
    state "compare string voltages" as GoRunning_compareStringVoltages
    state "start the battery with low voltage and stop the other one" as GoRunning_startOneBatteryStopOne
    state "-> Error" as GoRunning_error
    state "-> Go stopped" as GoRunning_goStopped     
    state "→ Running" as GoRunning_running

    [*] --> GoRunning_hasAllBatteriesFault:has all batteries fault
    GoRunning_hasAllBatteriesFault --> GoRunning_error:yes
    GoRunning_hasAllBatteriesFault --> GoRunning_startCommunication:else
    GoRunning_startCommunication --> GoRunning_startOneBattery: one battery has communication
    GoRunning_startCommunication --> GoRunning_compareStringVoltages:all batteries have values
    GoRunning_startCommunication --> GoRunning_goStopped: Target STOP
    GoRunning_startCommunication --> GoRunning_error :not started after t > ...  
    GoRunning_compareStringVoltages -->  GoRunning_stringVoltagesDifference
    GoRunning_stringVoltagesDifference --> GoRunning_startOneBatteryStopOne: Voltage difference is bigger than 4 V
    GoRunning_startOneBatteryStopOne --> GoRunning_error: can not start after t > ...
    GoRunning_startBatteries--> GoRunning_error: can not start after t > ...
    GoRunning_startOneBatteryStopOne --> GoRunning_running: One Battery Started
    GoRunning_stringVoltagesDifference --> GoRunning_startBatteries: Voltage Difference less than 4V
    GoRunning_startBatteries --> GoRunning_running: all batteries started 
    GoRunning_startOneBattery --> GoRunning_running: Battery Started
    GoRunning_startOneBattery--> GoRunning_error: can not start after t > ...
}
Stopped --> GoRunning: Target = START

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