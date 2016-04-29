package de.fenecon.femscore.modbus.device.ess;

public enum EssProtocol {
	SystemState, SystemMode,

	ActivePower, ReactivePower, ApparentPower,

	AllowedCharge, AllowedDischarge, AllowedApparent,

	BatteryStringSoc,

	SetActivePower, SetReactivePower;

	public enum SystemStates {
		Stop, PvCharging, Standby, Running, Fault, Debug
	}
}
