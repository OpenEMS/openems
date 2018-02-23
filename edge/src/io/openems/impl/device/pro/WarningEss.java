package io.openems.impl.device.pro;

import io.openems.api.channel.thingstate.WarningEnum;

public enum WarningEss implements WarningEnum {
	FailTheSystemShouldBeStopped(0), //
	CommonLowVoltageAlarm(1), //
	CommonHighVoltageAlarm(2), //
	ChargingOverCurrentAlarm(3), //
	DischargingOverCurrentAlarm(4), //
	OverTemperatureAlarm(5), //
	InteralCommunicationAbnormal(6), //
	GridUndervoltageL1(7), //
	GridOvervoltageL1(8), //
	GridUnderFrequencyL1(9), //
	GridOverFrequencyL1(10), //
	GridPowerSupplyOffL1(11), //
	GridConditionUnmeetL1(12), //
	DCUnderVoltageL1(13), //
	InputOverResistanceL1(14), //
	CombinationErrorL1(15), //
	CommWithInverterErrorL1(16), //
	TmeErrorL1(17), //
	GridUndervoltageL2(18), //
	GridOvervoltageL2(19), //
	GridUnderFrequencyL2(20), //
	GridOverFrequencyL2(21), //
	GridPowerSupplyOffL2(22), //
	GridConditionUnmeetL2(23), //
	DCUnderVoltageL2(24), //
	InputOverResistanceL2(25), //
	CombinationErrorL2(26), //
	CommWithInverterErrorL2(27), //
	TmeErrorL2(28), //
	GridUndervoltageL3(29), //
	GridOvervoltageL3(30), //
	GridUnderFrequencyL3(31), //
	GridOverFrequencyL3(32), //
	GridPowerSupplyOffL3(33), //
	GridConditionUnmeetL3(34), //
	DCUnderVoltageL3(35), //
	InputOverResistanceL3(36), //
	CombinationErrorL3(37), //
	CommWithInverterErrorL3(38), //
	TmeErrorL3(39), //
	OFFGrid(40);

	public final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
