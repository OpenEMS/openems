# State-Machine

```mermaid
stateDiagram-v2
direction LR

state Undefined {
    state "check F2B communication" as Undefined_checkF2BCommunication
	state Undefined_checkContactorsState <<choice>>
	state Undefined_checkStopped <<choice>>
	state "→ Error" as Undefined_error
	state "→ Running" as Undefined_running
	state "→ Stopped" as Undefined_stopped
	state "→ GoStopped" as Undefined_goStopped
	[*] --> Undefined_checkF2BCommunication
	Undefined_checkF2BCommunication --> Undefined_checkContactorsState: F2B state != undefined
	Undefined_checkF2BCommunication --> Undefined_error: t > 10 s
	Undefined_checkContactorsState --> Undefined_checkStopped: contactors state != closed
	Undefined_checkContactorsState --> Undefined_running: contactors state = closed
	Undefined_checkStopped --> Undefined_goStopped: else
	Undefined_checkStopped --> Undefined_stopped: T30C = off && CAN = off
}
[*] --> Undefined

state GoStopped {
	state "wait for current reduction" as GoStopped_waitForCurrentReduction
	state "open HV contactors" as GoStopped_openContactors
	state "switch off T15sw and T15hw" as GoStopped_switchOffT15
	state "wait 40 s" as GoStopped_wait40s
	state "CAN and T30C off" as GoStopped_CanAndT30cOff
	state "→ Stopped" as GoStopped_stopped
	[*] --> GoStopped_waitForCurrentReduction
	GoStopped_waitForCurrentReduction --> GoStopped_openContactors: current <= 1 A || current = undefined
	GoStopped_openContactors --> GoStopped_switchOffT15: contactors state != closed
	GoStopped_switchOffT15 --> GoStopped_wait40s: T15hw = off & T15sw = off
	GoStopped_wait40s --> GoStopped_CanAndT30cOff: t > 40 s
	GoStopped_CanAndT30cOff --> GoStopped_stopped: CAN = off & T30C = off
}
Undefined --> GoStopped
Running --> GoStopped: Target = STOP

state GoRunning {
	state "enable CAN communication (T30C and T15)" as GoRunning_enableCanCommunication
	state "toggle T15" as GoRunning_toggleT15
	state GoRunning_checkErrors <<choice>>
	state GoRunning_checkNumberOfRetries <<choice>>
	state "close contactors" as GoRunning_closeContactors
	state "→ Running" as GoRunning_running
	state "→ Error" as GoRunning_error
	[*] --> GoRunning_enableCanCommunication
	GoRunning_enableCanCommunication --> GoRunning_toggleT15: T30C = on & T15 = on & CAN = on & no Errors
	GoRunning_toggleT15 --> GoRunning_checkErrors: t > 10 s
	GoRunning_checkErrors --> GoRunning_closeContactors: no CAT errors & contactors != stucked
	GoRunning_checkErrors --> GoRunning_checkNumberOfRetries: else
	GoRunning_checkNumberOfRetries --> GoRunning_toggleT15: n < 3
	GoRunning_checkNumberOfRetries --> GoRunning_error: else
    GoRunning_closeContactors --> GoRunning_running: contactors state = closed
}
Stopped --> GoRunning: Target = START

state Stopped
Undefined --> Stopped
GoStopped --> Stopped

state Running
Undefined --> Running
GoRunning --> Running

state Error
Running --> Error: hasFaults() || contactors state != closed
GoRunning --> Error: t > 120 s
Stopped --> Error: hasFaults()
GoStopped --> Error: t > 120 s
Undefined --> Error
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor