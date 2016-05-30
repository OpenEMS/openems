package de.fenecon.femscore.modbus.device.ess;

public enum EssProtocol {
	SystemState, SystemMode,

	ActivePower, ReactivePower, ApparentPower,

	AllowedCharge, AllowedDischarge, AllowedApparent,

	BatteryStringSoc,

	SetActivePower, SetReactivePower,

	Pv1State, Pv1OutputVoltage, Pv1OutputCurrent, Pv1OutputPower, Pv1InputVoltage, Pv1InputCurrent, Pv1InputPower, Pv1InputEnergy, Pv1OutputEnergy,

	Pv2State, Pv2OutputVoltage, Pv2OutputCurrent, Pv2OutputPower, Pv2InputVoltage, Pv2InputCurrent, Pv2InputPower, Pv2InputEnergy, Pv2OutputEnergy;

	public enum SystemStates {
		Stop, PvCharging, Standby, Running, Fault, Debug
	}

	public enum DcStates {
		Initial, Stop, Ready, Running, Fault, Debug, Locked
	}
}
