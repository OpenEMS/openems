package io.openems.impl.device.refu;

import io.openems.api.channel.thingstate.WarningEnum;

public enum WarningEss implements WarningEnum
{
	NormalChargingOverCurrent(0), CharginigCurrentOverLimit(1), DischargingCurrentOverLimit(2), NormalHighVoltage(3), NormalLowVoltage(4),
	AbnormalVoltageVariation(5), NormalHighTemperature(6), NormalLowTemperature(7), AbnormalTemperatureVariation(8), SeriousHighVoltage(9),
	SeriousLowVoltage(10), SeriousLowTemperature(11), ChargingSeriousOverCurrent(12), DischargingSeriousOverCurrent(13), AbnormalCapacityAlarm(14),
	EEPROMParameterFailure(15), SwitchOfInsideCombinedCabinet(16), ShouldNotBeConnectedToGridDueToTheDCSideCondition(17),
	EmergencyStopRequireFromSystemController(18), BatteryGroup1EnableAndNotConnectedToGrid(19), BatteryGroup2EnableAndNotConnectedToGrid(20),
	BatteryGroup3EnableAndNotConnectedToGrid(21), BatteryGroup4EnableAndNotConnectedToGrid(22), TheIsolationSwitchOfBatteryGroup1Open(23),
	TheIsolationSwitchOfBatteryGroup2Open(24), TheIsolationSwitchOfBatteryGroup3Open(25), TheIsolationSwitchOfBatteryGroup4Open(26),
	BalancingSamplingFailureOfBatteryGroup1(27), BalancingSamplingFailureOfBatteryGroup2(28), BalancingSamplingFailureOfBatteryGroup3(29),
	BalancingSamplingFailureOfBatteryGroup4(30), BalancingControlFailureOfBatteryGroup1(31), BalancingControlFailureOfBatteryGroup2(32),
	BalancingControlFailureOfBatteryGroup3(33), BalancingControlFailureOfBatteryGroup4(34);


	public final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
