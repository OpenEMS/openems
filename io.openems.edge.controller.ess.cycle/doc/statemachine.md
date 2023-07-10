# State-Machine

```mermaid
stateDiagram-v2
direction LR

state Undefined {
	[*] --> Undefined_waitForDefinedSoc
	state "wait for defined SoC" as Undefined_waitForDefinedSoc
	state "Cycle order" as Undefined_getCycleOrder
	state "-> Start Charge" as Undefined_startCharge
	state "-> Start Discharge" as Undefined_startDischarge
	state "Auto" as Undefined_auto
		
	state Undefined_isSocDefined <<choice>>
	state Undefined_isStartTimeInitialized <<choice>>

	Undefined_waitForDefinedSoc --> Undefined_isSocDefined: Soc Defined
	Undefined_isSocDefined --> Undefined_waitForDefinedSoc: Soc Not Defined
	Undefined_isSocDefined --> Undefined_isStartTimeInitialized: Start Time Initialized
	Undefined_isStartTimeInitialized --> Undefined_waitForDefinedSoc: Start Time Not Initialized
	Undefined_isStartTimeInitialized --> Undefined_getCycleOrder    
  
	
	Undefined_getCycleOrder --> Undefined_startCharge: StartWithCharge
   	Undefined_getCycleOrder --> Undefined_startDischarge: StartWithDischarge
	Undefined_getCycleOrder --> Undefined_auto: Auto	
	Undefined_auto --> Undefined_startDischarge: Soc < 50
	Undefined_auto --> Undefined_startCharge: Soc >= 50 
}

state StartCharge {
	[*] --> StartCharge_applyActivePower

	state StartCharge_isMaxSoc100 <<choice>>
	state StartCharge_isAllowedChargePower0 <<choice>>
	state StartCharge_isCurrentSocReached <<choice>>

	state "Apply Active Power" as StartCharge_applyActivePower
	state "-> set Last State Change Time" as StartCharge_setNextState
	state "-> Wait For State Change" as StartCharge_WaitForStateChange


	StartCharge_applyActivePower --> StartCharge_isMaxSoc100: Config Max Soc
	StartCharge_isMaxSoc100 --> StartCharge_isCurrentSocReached: ConfigMaxSoc != 100 
	StartCharge_isCurrentSocReached --> StartCharge_applyActivePower: continue to apply power

	StartCharge_isCurrentSocReached --> StartCharge_setNextState: Current Soc == ConfigMaxSoc 
	StartCharge_isMaxSoc100 --> StartCharge_isAllowedChargePower0 : ConfigMaxSoc == 100 
	StartCharge_isAllowedChargePower0 --> StartCharge_setNextState: Allowed Charge Power == 0 
	StartCharge_isAllowedChargePower0 --> StartCharge_applyActivePower: continue to apply power

	StartCharge_setNextState --> StartCharge_WaitForStateChange: Next State = Continue With Discharge
}

state StartDischarge {
	[*] --> StartDischarge_applyActivePower

	state StartDischarge_isMinSoc0 <<choice>>
	state StartDischarge_isAllowedDischargePower0 <<choice>>
	state StartDischarge_isCurrentSocReached <<choice>>

	state "Apply Active Power" as StartDischarge_applyActivePower
	state "-> set Last State Change Time" as StartDischarge_setNextState
	state "-> Wait For State Change" as StartDischarge_WaitForStateChange

	StartDischarge_applyActivePower --> StartDischarge_isMinSoc0: Config Min Soc
	StartDischarge_isMinSoc0 --> StartDischarge_isCurrentSocReached: ConfigMinSoc != 0 
	StartDischarge_isCurrentSocReached --> StartDischarge_applyActivePower: continue to apply power

	StartDischarge_isCurrentSocReached --> StartDischarge_setNextState: Current Soc == ConfigMinSoc 
	StartDischarge_isMinSoc0 --> StartDischarge_isAllowedDischargePower0 : ConfigMinSoc == 100 
	StartDischarge_isAllowedDischargePower0 --> StartDischarge_setNextState: Allowed Discharge Power == 0 
	StartDischarge_isAllowedDischargePower0 --> StartDischarge_applyActivePower: continue to apply power

	StartDischarge_setNextState --> StartDischarge_WaitForStateChange: Next State = Continue With Charge
}

state ContinueWithCharge {
	[*] --> ContinueWithCharge_applyActivePower

	state ContinueWithCharge_isMaxSoc100 <<choice>>
	state ContinueWithCharge_isAllowedChargePower0 <<choice>>
	state ContinueWithCharge_isCurrentSocReached <<choice>>

	state "Apply Active Power" as ContinueWithCharge_applyActivePower
	state "-> set Last State Change Time" as ContinueWithCharge_setNextState
	state "-> Wait For State Change" as ContinueWithCharge_WaitForStateChange

	ContinueWithCharge_applyActivePower --> ContinueWithCharge_isMaxSoc100: Config Max Soc
	ContinueWithCharge_isMaxSoc100 --> ContinueWithCharge_isCurrentSocReached: ConfigMaxSoc != 100 
	ContinueWithCharge_isCurrentSocReached --> ContinueWithCharge_applyActivePower: continue to apply power

	ContinueWithCharge_isCurrentSocReached --> ContinueWithCharge_setNextState: Current Soc == ConfigMaxSoc 
	ContinueWithCharge_isMaxSoc100 --> ContinueWithCharge_isAllowedChargePower0 : ConfigMaxSoc == 100 
	ContinueWithCharge_isAllowedChargePower0 --> ContinueWithCharge_setNextState: Allowed Charge Power == 0 
	ContinueWithCharge_isAllowedChargePower0 --> ContinueWithCharge_applyActivePower: continue to apply power

	ContinueWithCharge_setNextState --> ContinueWithCharge_WaitForStateChange: Next State = Continue With Charge
}

state ContinueWithDischarge {
	[*] --> ContinueWithDischarge_applyActivePower

	state ContinueWithDischarge_isMinSoc0 <<choice>>
	state ContinueWithDischarge_isAllowedDischargePower0 <<choice>>
	state ContinueWithDischarge_isCurrentSocReached <<choice>>

	state "Apply Active Power" as ContinueWithDischarge_applyActivePower
	state "-> set Last State Change Time" as ContinueWithDischarge_setNextState
	state "-> Wait For State Change" as ContinueWithDischarge_WaitForStateChange

	ContinueWithDischarge_applyActivePower --> ContinueWithDischarge_isMinSoc0: Config Min Soc
	ContinueWithDischarge_isMinSoc0 --> ContinueWithDischarge_isCurrentSocReached: ConfigMinSoc != 0 
	ContinueWithDischarge_isCurrentSocReached --> ContinueWithDischarge_applyActivePower: continue to apply power

	ContinueWithDischarge_isCurrentSocReached --> ContinueWithDischarge_setNextState: Current Soc == ConfigMinSoc 
	ContinueWithDischarge_isMinSoc0 --> ContinueWithDischarge_isAllowedDischargePower0 : ConfigMinSoc == 100 
	ContinueWithDischarge_isAllowedDischargePower0 --> ContinueWithDischarge_setNextState: Allowed Discharge Power == 0 
	ContinueWithDischarge_isAllowedDischargePower0 --> ContinueWithDischarge_applyActivePower: continue to apply power
		
	ContinueWithDischarge_setNextState --> ContinueWithDischarge_WaitForStateChange: Next State = Continue With Discharge
}

state WaitForStateChange {
	[*] --> WaitForStateChange_wait
	state "Standby Time" as WaitForStateChange_wait
	state "→ setNextState" as WaitForStateChange_changeState  

	WaitForStateChange_wait --> WaitForStateChange_changeState:waiting time passed 
} 

state CompletedCycle {
	[*] --> CompletedCycle_wait
	
	state "Set Completed Cycle Number" as CompletedCycle_wait
	state "-> Final Soc" as CompletedCycle_finalSoc
	state "Cycle order" as CompletedCycle_getCycleOrder
	state "-> Start Charge" as CompletedCycle_startCharge
	state "-> Start Discharge" as CompletedCycle_startDischarge
	state "Auto" as CompletedCycle_auto
	state CompletedCycle_totalCycleNumberComReached <<choice>>
	CompletedCycle_wait --> CompletedCycle_totalCycleNumberComReached 
	CompletedCycle_totalCycleNumberComReached -->   CompletedCycle_finalSoc: total # of cycles completed
	CompletedCycle_totalCycleNumberComReached -->   CompletedCycle_getCycleOrder: continue with next State
	
	CompletedCycle_getCycleOrder --> CompletedCycle_startCharge: StartWithCharge
   	CompletedCycle_getCycleOrder --> CompletedCycle_startDischarge: StartWithDischarge
	CompletedCycle_getCycleOrder --> CompletedCycle_auto: Auto	
	CompletedCycle_auto --> CompletedCycle_startDischarge: Soc < 50
	CompletedCycle_auto --> CompletedCycle_startCharge: Soc >= 50 
} 
state FinalSoc {
	[*] --> FinalSoc_isSocReached
	
	state "-> Finished" as FinalSoc_finished
	state "-> Charge" as FinalSoc_charge
	state "-> Discharge" as FinalSoc_discharge
	state FinalSoc_isSocReached <<choice>>
	
	FinalSoc_isSocReached --> FinalSoc_finished: Final Soc Reached
	FinalSoc_isSocReached --> FinalSoc_discharge: CurrentSoc > FinalSoc
	FinalSoc_isSocReached --> FinalSoc_charge: CurrentSoc > FinalSoc
	FinalSoc_charge --> FinalSoc_isSocReached
	FinalSoc_discharge --> FinalSoc_isSocReached

} 

state Finished {
	[*] --> [FINISHED]
} 

Undefined --> StartCharge
StartCharge --> WaitForStateChange
WaitForStateChange --> ContinueWithDischarge
ContinueWithDischarge --> WaitForStateChange
WaitForStateChange --> CompletedCycle

Undefined --> StartDischarge
StartDischarge --> WaitForStateChange
WaitForStateChange --> ContinueWithCharge
ContinueWithCharge --> WaitForStateChange

CompletedCycle --> StartDischarge
CompletedCycle --> StartCharge
CompletedCycle --> FinalSoc
FinalSoc --> Finished
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor