package io.openems.impl.device.mini;

import io.openems.api.channel.thingstate.FaultEnum;

public enum FaultEss  implements FaultEnum{
	ControlCurrentOverload100Percent(0), ControlCurrentOverload110Percent(1), ControlCurrentOverload150Percent(2), ControlCurrentOverload200Percent(3),
	ControlCurrentOverload120Percent(4), ControlCurrentOverload300Percent(5), ControlTransientLoad300Percent(6), GridOverCurrent(7), LockingWaveformTooManyTimes(8),
	InverterVoltageZeroDriftError(9), GridVoltageZeroDriftError(10), ControlCurrentZeroDriftError(11), InverterCurrentZeroDriftError(12), GridCurrentZeroDriftError(13),
	PDPProtection(14), HardwareControlCurrentProtection(15), HardwareACVoltProtection(16), HardwareDCCurrentProtection(17), HardwareTemperatureProtection(18),
	NoCapturingSignal(19), DCOvervoltage(20), DCDisconnected(21), InverterUndervoltage(22), InverterOvervoltage(23), CurrentSensorFail(24), VoltageSensorFail(25),
	PowerUncontrollable(26), CurrentUncontrollable(27), FanError(28), PhaseLack(29), InverterRelayFault(30), GridRelayFault(31), ControlPanelOvertemp(32), PowerPanelOvertemp(33),
	DCInputOvercurrent(34), CapacitorOvertemp(35), RadiatorOvertemp(36), TransformerOvertemp(37), CombinationCommError(38), EEPROMError(39), LoadCurrentZeroDriftError(40),
	CurrentLimitRError(41), PhaseSyncError(42), ExternalPVCurrentZeroDriftError(43), ExternalGridCurrentZeroDriftError(44);




	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
