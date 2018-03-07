package io.openems.impl.device.minireadonly;

import io.openems.api.channel.thingstate.WarningEnum;

public enum WarningEss implements WarningEnum
{
	BECU1GeneralChargeOverCurrentAlarm(0), BECU1GeneralDischargeOverCurrentAlarm(1), BECU1ChargeCurrentLimitAlarm(2), BECU1DischargeCurrentLimitAlarm(3),
	BECU1GeneralHighVoltageAlarm(4), BECU1GeneralLowVoltageAlarm(5), BECU1AbnormalVoltageChangeAlarm(6), BECU1GeneralHighTemperatureAlarm(7),BECU1GeneralLowTemperatureAlarm(8),
	BECU1AbnormalTemperatureChangeAlarm(9), BECU1SevereHighVoltageAlarm(10), BECU1SevereLowVoltageAlarm(11),BECU1SevereLowTemperatureAlarm(12), BECU1SeverveChargeOverCurrentAlarm(13),
	BECU1SeverveDischargeOverCurrentAlarm(14), BECU1AbnormalCellCapacityAlarm(15), BECU1BalancedSamplingAlarm(16), BECU1BalancedControlAlarm(17), BECU1HallSensorDoesNotWorkAccurately(18),
	BECU1Generalleakage(19), BECU1Severeleakage(20), BECU1Contactor1TurnOnAbnormity(21), BECU1Contactor1TurnOffAbnormity(22), BECU1Contactor2TurnOnAbnormity(23), BECU1Contactor2TurnOffAbnormity(24),
	BECU1Contactor4CheckAbnormity(25), BECU1ContactorCurrentUnsafe(26), BECU1Contactor5CheckAbnormity(27), BECU1HighVoltageOffset(28), BECU1LowVoltageOffset(29), BECU1HighTemperatureOffset(30),
	BECU2GeneralChargeOverCurrentAlarm(31), BECU2GeneralDischargeOverCurrentAlarm(32), BECU2ChargeCurrentLimitAlarm(33), BECU2DischargeCurrentLimitAlarm(34), BECU2GeneralHighVoltageAlarm(35),
	BECU2GeneralLowVoltageAlarm(36), BECU2AbnormalVoltageChangeAlarm(37), BECU2GeneralHighTemperatureAlarm(38), BECU2GeneralLowTemperatureAlarm(39), BECU2AbnormalTemperatureChangeAlarm(40),
	BECU2SevereHighVoltageAlarm(41), BECU2SevereLowVoltageAlarm(42), BECU2SevereLowTemperatureAlarm(43), BECU2SeverveChargeOverCurrentAlarm(44), BECU2SeverveDischargeOverCurrentAlarm(45),
	BECU2AbnormalCellCapacityAlarm(46), BECU2BalancedSamplingAlarm(47), BECU2BalancedControlAlarm(48), BECU2HallSensorDoesNotWorkAccurately(49), BECU2Generalleakage(50), BECU2Severeleakage(51),
	BECU2Contactor1TurnOnAbnormity(52), BECU2Contactor1TurnOffAbnormity(53), BECU2Contactor2TurnOnAbnormity(54), BECU2Contactor2TurnOffAbnormity(55), BECU2Contactor4CheckAbnormity(56),
	BECU2ContactorCurrentUnsafe(57), BECU2Contactor5CheckAbnormity(58), BECU2HighVoltageOffset(59), BECU2LowVoltageOffset(60), BECU2HighTemperatureOffset(61), GeneralOvercurrentAlarmAtCellStackCharge(62),
	GeneralOvercurrentAlarmAtCellStackDischarge(63), CurrentLimitAlarmAtCellStackCharge(64), CurrentLimitAlarmAtCellStackDischarge(65), GeneralCellStackHighVoltageAlarm(66), GeneralCellStackLowVoltageAlarm(67),
	AbnormalCellStackVoltageChangeAlarm(68), GeneralCellStackHighTemperatureAlarm(69), GeneralCellStackLowTemperatureAlarm(70), AbnormalCellStackTemperatureChangeAlarm(71), SevereCellStackHighVoltageAlarm(72),
	SevereCellStackLowVoltageAlarm(73), SevereCellStackLowTemperatureAlarm(74), SeverveOverCurrentAlarmAtCellStackDharge(75), SeverveOverCurrentAlarmAtCellStackDischarge(76), AbnormalCellStackCapacityAlarm(77),
	TheParameterOfEEPROMInCellStackLoseEffectiveness(78), IsolatingSwitchInConfluenceArkBreak(79), TheCommunicationBetweenCellStackAndTemperatureOfCollectorBreak(80), TheTemperatureOfCollectorFail(81),
	HallSensorDoNotWorkAccurately(82), TheCommunicationOfPCSBreak(83), AdvancedChargingOrMainContactorCloseAbnormally(84), AbnormalSampledVoltage(85), AbnormalAdvancedContactorOrAbnormalRS485GalleryOfPCS(86),
	AbnormalMainContactor(87), GeneralCellStackLeakage(88), SevereCellStackLeakage(89), SmokeAlarm(90), TheCommunicationWireToAmmeterBreak(91), TheCommunicationWireToDredBreak(92);



	public final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
