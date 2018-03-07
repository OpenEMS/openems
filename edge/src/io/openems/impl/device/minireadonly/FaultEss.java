package io.openems.impl.device.minireadonly;

import io.openems.api.channel.thingstate.FaultEnum;

public enum FaultEss implements FaultEnum
{

	BECU1DischargeSevereOvercurrent(0), BECU1ChargeSevereOvercurrent(1), BECU1GeneralUndervoltage(2), BECU1SevereOvervoltage(3), BECU1GeneralOvervoltage(4), BECU1SevereUndervoltage(5), BECU1InsideCANBroken(6),
	BECU1GeneralUndervoltageHighCurrentDischarge(7), BECU1BMUError(8), BECU1CurrentSamplingInvalidation(9), BECU1BatteryFail(10), BECU1TemperatureSamplingBroken(11), BECU1Contactor1TestBackIsAbnormalTurnOnAbnormity(12),
	BECU1Contactor1TestBackIsAbnormalTurnOffAbnormity(13), BECU1Contactor2TestBackIsAbnormalTurnOnAbnormity(14), BECU1Contactor2TestBackIsAbnormalTurnOffAbnormity(15), BECU1SevereHighTemperatureFault(16),
	BECU1HallInvalidation(17), BECU1ContactorInvalidation(18), BECU1OutsideCANBroken(19), BECU1CathodeContactorBroken(20), BECU2DischargeSevereOvercurrent(21), BECU2ChargeSevereOvercurrent(22), BECU2GeneralUndervoltage(23),
	BECU2SevereOvervoltage(24), BECU2GeneralOvervoltage(25), BECU2SevereUndervoltage(26), BECU2InsideCANBroken(27), BECU2GeneralUndervoltageHighCurrentDischarge(28), BECU2BMUError(29), BECU2CurrentSamplingInvalidation(30),
	BECU2BatteryFail(31), BECU2TemperatureSamplingBroken(32), BECU2Contactor1TestBackIsAbnormalTurnOnAbnormity(33), BECU2Contactor1TestBackIsAbnormalTurnOffAbnormity(34), BECU2Contactor2TestBackIsAbnormalTurnOnAbnormity(35),
	BECU2Contactor2TestBackIsAbnormalTurnOffAbnormity(36), BECU2SevereHighTemperatureFault(37), BECU2HallInvalidation(38), BECU2ContactorInvalidation(39), BECU2OutsideCANBroken(40), BECU2CathodeContactorBroken(41),
	NoAvailableBatteryGroup(42), StackGeneralLeakage(43), StackSevereLeakage(44), StackStartingFail(45), StackStoppingFail(46), BatteryProtection(47), StackAndGroup1CANCommunicationInterrupt(48),
	StackAndGroup2CANCommunicationInterrupt(49);
	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
