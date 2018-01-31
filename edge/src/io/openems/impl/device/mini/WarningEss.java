package io.openems.impl.device.mini;

import io.openems.api.channel.thingstate.WarningEnum;

public enum WarningEss implements WarningEnum{
	FailTheSystemShouldBeStopped(0), CommonLowVoltageAlarm(1), CommonHighVoltageAlarm(2), ChargingOverCurrentAlarm(3),
	DischargingOverCurrentAlarm(4), OverTemperatureAlarm(5), InteralCommunicationAbnormal(6), GridUndervoltage(7),
	GridOvervoltage(8), GridUnderFrequency(9), GridOverFrequency(10), GridPowerSupplyOff(11), GridConditionUnmeet(12),
	DCUnderVoltage(13), InputOverResistance(14), CombinationError(15), CommWithInverterError(16), TmeError(17);

	public final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
