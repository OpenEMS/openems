package io.openems.edge.fenecon.mini.core.api;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.fenecon.mini.charger.FeneconMiniCharger;
import io.openems.edge.fenecon.mini.ess.FeneconMiniEss;
import io.openems.edge.fenecon.mini.gridmeter.FeneconMiniGridMeter;

public interface FeneconMiniCore {

	public void setEss(FeneconMiniEss ess);

	public void unsetEss(FeneconMiniEss ess);

	public void setCharger(FeneconMiniCharger charger);

	public void unsetCharger(FeneconMiniCharger charger);

	public void setGridMeter(FeneconMiniGridMeter meter);

	public void unsetGridMeter(FeneconMiniGridMeter meter);

	enum SetWorkState {
		LOCAL_CONTROL, START, REMOTE_CONTROL_OF_GRID, STOP, EMERGENCY_STOP
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SET_WORK_STATE(new Doc() //
				.option(0, SetWorkState.LOCAL_CONTROL)//
				.option(1, SetWorkState.START) //
				.option(2, SetWorkState.REMOTE_CONTROL_OF_GRID) //
				.option(3, SetWorkState.STOP) //
				.option(4, SetWorkState.EMERGENCY_STOP)), //
		SYSTEM_WORK_MODE_STATE(new Doc()), //
		SYSTEM_STATE(new Doc() //
				.option(0, "STANDBY") //
				.option(1, "Start Off-Grid") //
				.option(2, "START") //
				.option(3, "FAULT") //
				.option(4, "Off-Grd PV")), //
		CONTROL_MODE(new Doc()//
				.option(1, "Remote")//
				.option(2, "Local")), //

		BATTERY_GROUP_STATE(new Doc()//
				.option(0, "Initial")//
				.option(1, "Stop")//
				.option(2, "Starting")//
				.option(3, "Running")//
				.option(4, "Stopping")//
				.option(5, "Fail")//
		), //
		ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		PHASE_ALLOWED_APPARENT(new Doc().unit(Unit.VOLT_AMPERE)), //

		PCS_OPERATION_STATE(new Doc()//
				.option(0, "Self-checking")//
				.option(1, "Standby")//
				.option(2, "Off-Grid PV")//
				.option(3, "Off-Grid")//
				.option(4, "ON_GRID")//
				.option(5, "Fail")//
				.option(6, "ByPass 1")//
				.option(7, "ByPass 2")), //
		RTC_YEAR(new Doc().text("Year")), //
		RTC_MONTH(new Doc().text("Month")), //
		RTC_DAY(new Doc().text("Day")), //
		RTC_HOUR(new Doc().text("Hour")), //
		RTC_MINUTE(new Doc().text("Minute")), //
		RTC_SECOND(new Doc().text("Second")), //
		SET_SETUP_MODE(new Doc()//
				.option(0, "OFF")//
				.option(1, "ON")), //
		SET_PCS_MODE(new Doc()//
				.option(0, "Emergency")//
				.option(1, "ConsumersPeakPattern")//
				.option(2, "Economic")//
				.option(3, "Eco")//
				.option(4, "Debug")//
				.option(5, "SmoothPv")//
				.option(6, "Remote")), //
		SETUP_MODE(new Doc()//
				.option(0, "OFF")//
				.option(1, "ON")), //
		PCS_MODE(new Doc()//
				.option(0, "Emergency")//
				.option(1, "ConsumersPeakPattern")//
				.option(2, "Economic")//
				.option(3, "Eco")//
				.option(4, "Debug")//
				.option(5, "SmoothPv")//
				.option(6, "Remote")//

		), //

		BECU_NUM(new Doc()), //
		BECU_WORK_STATE(new Doc()), //
		BECU_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU1_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU2_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU1_VERSION(new Doc()), //
		BECU1_MIN_VOLT_NO(new Doc()), //
		BECU1_MIN_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_MAX_VOLT_NO(new Doc()), //
		BECU1_MAX_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_MIN_TEMP_NO(new Doc()), //
		BECU1_MIN_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BECU1_MAX_TEMP_NO(new Doc()), //
		BECU1_MAX_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		BECU2_VERSION(new Doc()), //
		BECU2_MIN_VOLT_NO(new Doc()), //
		BECU2_MIN_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_MAX_VOLT_NO(new Doc()), //
		BECU2_MAX_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_MIN_TEMP_NO(new Doc()), //
		BECU2_MIN_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BECU2_MAX_TEMP_NO(new Doc()), //
		BECU2_MAX_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		STATE_0(new Doc().level(Level.WARNING).text("FailTheSystemShouldBeStopped")), //
		STATE_1(new Doc().level(Level.WARNING).text("CommonLowVoltageAlarm")), //
		STATE_2(new Doc().level(Level.WARNING).text("CommonHighVoltageAlarm")), //
		STATE_3(new Doc().level(Level.WARNING).text("ChargingOverCurrentAlarm")), //
		STATE_4(new Doc().level(Level.WARNING).text("DischargingOverCurrentAlarm")), //
		STATE_5(new Doc().level(Level.WARNING).text("OverTemperatureAlarm")), //
		STATE_6(new Doc().level(Level.WARNING).text("InteralCommunicationAbnormal")), //
		STATE_7(new Doc().level(Level.WARNING).text("GridUndervoltage")), //
		STATE_8(new Doc().level(Level.WARNING).text("GridOvervoltage")), //
		STATE_9(new Doc().level(Level.WARNING).text("")), //
		STATE_10(new Doc().level(Level.WARNING).text("GridUnderFrequency")), //
		STATE_11(new Doc().level(Level.WARNING).text("GridOverFrequency")), //
		STATE_12(new Doc().level(Level.WARNING).text("GridPowerSupplyOff")), //
		STATE_13(new Doc().level(Level.WARNING).text("GridConditionUnmeet")), //
		STATE_14(new Doc().level(Level.WARNING).text("DCUnderVoltage")), //
		STATE_15(new Doc().level(Level.WARNING).text("InputOverResistance")), //
		STATE_16(new Doc().level(Level.WARNING).text("CombinationError")), //
		STATE_17(new Doc().level(Level.WARNING).text("CommWithInverterError")), //
		STATE_18(new Doc().level(Level.WARNING).text("TmeError")), //
		STATE_19(new Doc().level(Level.WARNING).text("PcsAlarm2")), //
		STATE_20(new Doc().level(Level.FAULT).text("ControlCurrentOverload100Percent")), //
		STATE_21(new Doc().level(Level.FAULT).text("ControlCurrentOverload110Percent")), //
		STATE_22(new Doc().level(Level.FAULT).text("ControlCurrentOverload150Percent")), //
		STATE_23(new Doc().level(Level.FAULT).text("ControlCurrentOverload200Percent")), //
		STATE_24(new Doc().level(Level.FAULT).text("ControlCurrentOverload120Percent")), //
		STATE_25(new Doc().level(Level.FAULT).text("ControlCurrentOverload300Percent")), //
		STATE_26(new Doc().level(Level.FAULT).text("ControlTransientLoad300Percent")), //
		STATE_27(new Doc().level(Level.FAULT).text("GridOverCurrent")), //
		STATE_28(new Doc().level(Level.FAULT).text("LockingWaveformTooManyTimes")), //
		STATE_29(new Doc().level(Level.FAULT).text("InverterVoltageZeroDriftError")), //
		STATE_30(new Doc().level(Level.FAULT).text("GridVoltageZeroDriftError")), //
		STATE_31(new Doc().level(Level.FAULT).text("ControlCurrentZeroDriftError")), //
		STATE_32(new Doc().level(Level.FAULT).text("InverterCurrentZeroDriftError")), //
		STATE_33(new Doc().level(Level.FAULT).text("GridCurrentZeroDriftError")), //
		STATE_34(new Doc().level(Level.FAULT).text("PDPProtection")), //
		STATE_35(new Doc().level(Level.FAULT).text("HardwareControlCurrentProtection")), //
		STATE_36(new Doc().level(Level.FAULT).text("HardwareACVoltProtection")), //
		STATE_37(new Doc().level(Level.FAULT).text("HardwareDCCurrentProtection")), //
		STATE_38(new Doc().level(Level.FAULT).text("HardwareTemperatureProtection")), //
		STATE_39(new Doc().level(Level.FAULT).text("NoCapturingSignal")), //
		STATE_40(new Doc().level(Level.FAULT).text("DCOvervoltage")), //
		STATE_41(new Doc().level(Level.FAULT).text("DCDisconnected")), //
		STATE_42(new Doc().level(Level.FAULT).text("InverterUndervoltage")), //
		STATE_43(new Doc().level(Level.FAULT).text("InverterOvervoltage")), //
		STATE_44(new Doc().level(Level.FAULT).text("CurrentSensorFail")), //
		STATE_45(new Doc().level(Level.FAULT).text("VoltageSensorFail")), //
		STATE_46(new Doc().level(Level.FAULT).text("PowerUncontrollable")), //
		STATE_47(new Doc().level(Level.FAULT).text("CurrentUncontrollable")), //
		STATE_48(new Doc().level(Level.FAULT).text("FanError")), //
		STATE_49(new Doc().level(Level.FAULT).text("PhaseLack")), //
		STATE_50(new Doc().level(Level.FAULT).text("InverterRelayFault")), //
		STATE_51(new Doc().level(Level.FAULT).text("GridRelayFault")), //
		STATE_52(new Doc().level(Level.FAULT).text("ControlPanelOvertemp")), //
		STATE_53(new Doc().level(Level.FAULT).text("PowerPanelOvertemp")), //
		STATE_54(new Doc().level(Level.FAULT).text("DCInputOvercurrent")), //
		STATE_55(new Doc().level(Level.FAULT).text("CapacitorOvertemp")), //
		STATE_56(new Doc().level(Level.FAULT).text("RadiatorOvertemp")), //
		STATE_57(new Doc().level(Level.FAULT).text("TransformerOvertemp")), //
		STATE_58(new Doc().level(Level.FAULT).text("CombinationCommError")), //
		STATE_59(new Doc().level(Level.FAULT).text("EEPROMError")), //
		STATE_60(new Doc().level(Level.FAULT).text("LoadCurrentZeroDriftError")), //
		STATE_61(new Doc().level(Level.FAULT).text("CurrentLimitRError")), //
		STATE_62(new Doc().level(Level.FAULT).text("PhaseSyncError")), //
		STATE_63(new Doc().level(Level.FAULT).text("ExternalPVCurrentZeroDriftError")), //
		STATE_64(new Doc().level(Level.FAULT).text("ExternalGridCurrentZeroDriftError")), //
		STATE_65(new Doc().level(Level.WARNING).text("BECU1GeneralChargeOverCurrentAlarm")), //
		STATE_66(new Doc().level(Level.WARNING).text("BECU1GeneralDischargeOverCurrentAlarm")), //
		STATE_67(new Doc().level(Level.WARNING).text("BECU1ChargeCurrentLimitAlarm")), //
		STATE_68(new Doc().level(Level.WARNING).text("BECU1DischargeCurrentLimitAlarm")), //
		STATE_69(new Doc().level(Level.WARNING).text("BECU1GeneralHighVoltageAlarm")), //
		STATE_70(new Doc().level(Level.WARNING).text("BECU1GeneralLowVoltageAlarm")), //
		STATE_71(new Doc().level(Level.WARNING).text("BECU1AbnormalVoltageChangeAlarm")), //
		STATE_72(new Doc().level(Level.WARNING).text("BECU1GeneralHighTemperatureAlarm")), //
		STATE_73(new Doc().level(Level.WARNING).text("BECU1GeneralLowTemperatureAlarm")), //
		STATE_74(new Doc().level(Level.WARNING).text("BECU1AbnormalTemperatureChangeAlarm")), //
		STATE_75(new Doc().level(Level.WARNING).text("BECU1SevereHighVoltageAlarm")), //
		STATE_76(new Doc().level(Level.WARNING).text("BECU1SevereLowVoltageAlarm")), //
		STATE_77(new Doc().level(Level.WARNING).text("BECU1SevereLowTemperatureAlarm")), //
		STATE_78(new Doc().level(Level.WARNING).text("BECU1SeverveChargeOverCurrentAlarm")), //
		STATE_79(new Doc().level(Level.WARNING).text("BECU1SeverveDischargeOverCurrentAlarm")), //
		STATE_80(new Doc().level(Level.WARNING).text("BECU1AbnormalCellCapacityAlarm")), //
		STATE_81(new Doc().level(Level.WARNING).text("BECU1BalancedSamplingAlarm")), //
		STATE_82(new Doc().level(Level.WARNING).text("BECU1BalancedControlAlarm")), //
		STATE_83(new Doc().level(Level.WARNING).text("BECU1HallSensorDoesNotWorkAccurately")), //
		STATE_84(new Doc().level(Level.WARNING).text("BECU1Generalleakage")), //
		STATE_85(new Doc().level(Level.WARNING).text("BECU1Severeleakage")), //
		STATE_86(new Doc().level(Level.WARNING).text("BECU1Contactor1TurnOnAbnormity")), //
		STATE_87(new Doc().level(Level.WARNING).text("BECU1Contactor1TurnOffAbnormity")), //
		STATE_88(new Doc().level(Level.WARNING).text("BECU1Contactor2TurnOnAbnormity")), //
		STATE_89(new Doc().level(Level.WARNING).text("BECU1Contactor2TurnOffAbnormity")), //
		STATE_90(new Doc().level(Level.WARNING).text("BECU1Contactor4CheckAbnormity")), //
		STATE_91(new Doc().level(Level.WARNING).text("BECU1ContactorCurrentUnsafe")), //
		STATE_92(new Doc().level(Level.WARNING).text("BECU1Contactor5CheckAbnormity")), //
		STATE_93(new Doc().level(Level.WARNING).text("BECU1HighVoltageOffset")), //
		STATE_94(new Doc().level(Level.WARNING).text("BECU1LowVoltageOffset")), //
		STATE_95(new Doc().level(Level.WARNING).text("BECU1HighTemperatureOffset")), //
		STATE_96(new Doc().level(Level.FAULT).text("BECU1DischargeSevereOvercurrent")), //
		STATE_97(new Doc().level(Level.FAULT).text("BECU1ChargeSevereOvercurrent")), //
		STATE_98(new Doc().level(Level.FAULT).text("BECU1GeneralUndervoltage")), //
		STATE_99(new Doc().level(Level.FAULT).text("BECU1SevereOvervoltage")), //
		STATE_100(new Doc().level(Level.FAULT).text("BECU1GeneralOvervoltage")), //
		STATE_101(new Doc().level(Level.FAULT).text("BECU1SevereUndervoltage")), //
		STATE_102(new Doc().level(Level.FAULT).text("BECU1InsideCANBroken")), //
		STATE_103(new Doc().level(Level.FAULT).text("BECU1GeneralUndervoltageHighCurrentDischarge")), //
		STATE_104(new Doc().level(Level.FAULT).text("BECU1BMUError")), //
		STATE_105(new Doc().level(Level.FAULT).text("BECU1CurrentSamplingInvalidation")), //
		STATE_106(new Doc().level(Level.FAULT).text("BECU1BatteryFail")), //
		STATE_107(new Doc().level(Level.FAULT).text("BECU1TemperatureSamplingBroken")), //
		STATE_108(new Doc().level(Level.FAULT).text("BECU1Contactor1TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_109(new Doc().level(Level.FAULT).text("BECU1Contactor1TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_110(new Doc().level(Level.FAULT).text("BECU1Contactor2TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_111(new Doc().level(Level.FAULT).text("BECU1Contactor2TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_112(new Doc().level(Level.FAULT).text("BECU1SevereHighTemperatureFault")), //
		STATE_113(new Doc().level(Level.FAULT).text("BECU1HallInvalidation")), //
		STATE_114(new Doc().level(Level.FAULT).text("BECU1ContactorInvalidation")), //
		STATE_115(new Doc().level(Level.FAULT).text("BECU1OutsideCANBroken")), //
		STATE_116(new Doc().level(Level.FAULT).text("BECU1CathodeContactorBroken")), //

		STATE_117(new Doc().level(Level.WARNING).text("BECU2GeneralChargeOverCurrentAlarm")), //
		STATE_118(new Doc().level(Level.WARNING).text("BECU2GeneralDischargeOverCurrentAlarm")), //
		STATE_119(new Doc().level(Level.WARNING).text("BECU2ChargeCurrentLimitAlarm")), //
		STATE_120(new Doc().level(Level.WARNING).text("BECU2DischargeCurrentLimitAlarm")), //
		STATE_121(new Doc().level(Level.WARNING).text("BECU2GeneralHighVoltageAlarm")), //
		STATE_122(new Doc().level(Level.WARNING).text("BECU2GeneralLowVoltageAlarm")), //
		STATE_123(new Doc().level(Level.WARNING).text("BECU2AbnormalVoltageChangeAlarm")), //
		STATE_124(new Doc().level(Level.WARNING).text("BECU2GeneralHighTemperatureAlarm")), //
		STATE_125(new Doc().level(Level.WARNING).text("BECU2GeneralLowTemperatureAlarm")), //
		STATE_126(new Doc().level(Level.WARNING).text("BECU2AbnormalTemperatureChangeAlarm")), //
		STATE_127(new Doc().level(Level.WARNING).text("BECU2SevereHighVoltageAlarm")), //
		STATE_128(new Doc().level(Level.WARNING).text("BECU2SevereLowVoltageAlarm")), //
		STATE_129(new Doc().level(Level.WARNING).text("BECU2SevereLowTemperatureAlarm")), //
		STATE_130(new Doc().level(Level.WARNING).text("BECU2SeverveChargeOverCurrentAlarm")), //
		STATE_131(new Doc().level(Level.WARNING).text("BECU2SeverveDischargeOverCurrentAlarm")), //
		STATE_132(new Doc().level(Level.WARNING).text("BECU2AbnormalCellCapacityAlarm")), //
		STATE_133(new Doc().level(Level.WARNING).text("BECU2BalancedSamplingAlarm")), //
		STATE_134(new Doc().level(Level.WARNING).text("BECU2BalancedControlAlarm")), //
		STATE_135(new Doc().level(Level.WARNING).text("BECU2HallSensorDoesNotWorkAccurately")), //
		STATE_136(new Doc().level(Level.WARNING).text("BECU2Generalleakage")), //
		STATE_137(new Doc().level(Level.WARNING).text("BECU2Severeleakage")), //
		STATE_138(new Doc().level(Level.WARNING).text("BECU2Contactor1TurnOnAbnormity")), //
		STATE_139(new Doc().level(Level.WARNING).text("BECU2Contactor1TurnOffAbnormity")), //
		STATE_140(new Doc().level(Level.WARNING).text("BECU2Contactor2TurnOnAbnormity")), //
		STATE_141(new Doc().level(Level.WARNING).text("BECU2Contactor2TurnOffAbnormity")), //
		STATE_142(new Doc().level(Level.WARNING).text("BECU2Contactor4CheckAbnormity")), //
		STATE_143(new Doc().level(Level.WARNING).text("BECU2ContactorCurrentUnsafe")), //
		STATE_144(new Doc().level(Level.WARNING).text("BECU2Contactor5CheckAbnormity")), //
		STATE_145(new Doc().level(Level.WARNING).text("BECU2HighVoltageOffset")), //
		STATE_146(new Doc().level(Level.WARNING).text("BECU2LowVoltageOffset")), //
		STATE_147(new Doc().level(Level.WARNING).text("BECU2HighTemperatureOffset")), //
		STATE_148(new Doc().level(Level.FAULT).text("BECU2DischargeSevereOvercurrent")), //
		STATE_149(new Doc().level(Level.FAULT).text("BECU2ChargeSevereOvercurrent")), //
		STATE_150(new Doc().level(Level.FAULT).text("BECU2GeneralUndervoltage")), //
		STATE_151(new Doc().level(Level.FAULT).text("BECU2SevereOvervoltage")), //
		STATE_152(new Doc().level(Level.FAULT).text("BECU2GeneralOvervoltage")), //
		STATE_153(new Doc().level(Level.FAULT).text("BECU2SevereUndervoltage")), //
		STATE_154(new Doc().level(Level.FAULT).text("BECU2InsideCANBroken")), //
		STATE_155(new Doc().level(Level.FAULT).text("BECU2GeneralUndervoltageHighCurrentDischarge")), //
		STATE_156(new Doc().level(Level.FAULT).text("BECU2BMUError")), //
		STATE_157(new Doc().level(Level.FAULT).text("BECU2CurrentSamplingInvalidation")), //
		STATE_158(new Doc().level(Level.FAULT).text("BECU2BatteryFail")), //
		STATE_159(new Doc().level(Level.FAULT).text("BECU2TemperatureSamplingBroken")), //
		STATE_160(new Doc().level(Level.FAULT).text("BECU2Contactor1TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_161(new Doc().level(Level.FAULT).text("BECU2Contactor1TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_162(new Doc().level(Level.FAULT).text("BECU2Contactor2TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_163(new Doc().level(Level.FAULT).text("BECU2Contactor2TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_164(new Doc().level(Level.FAULT).text("BECU2SevereHighTemperatureFault")), //
		STATE_165(new Doc().level(Level.FAULT).text("BECU2HallInvalidation")), //
		STATE_166(new Doc().level(Level.FAULT).text("BECU2ContactorInvalidation")), //
		STATE_167(new Doc().level(Level.FAULT).text("BECU2OutsideCANBroken")), //
		STATE_168(new Doc().level(Level.FAULT).text("BECU2CathodeContactorBroken")), //

		STATE_169(new Doc().level(Level.FAULT).text("NoAvailableBatteryGroup")), //
		STATE_170(new Doc().level(Level.FAULT).text("StackGeneralLeakage")), //
		STATE_171(new Doc().level(Level.FAULT).text("StackSevereLeakage")), //
		STATE_172(new Doc().level(Level.FAULT).text("StackStartingFail")), //
		STATE_173(new Doc().level(Level.FAULT).text("StackStoppingFail")), //
		STATE_174(new Doc().level(Level.FAULT).text("BatteryProtection")), //
		STATE_175(new Doc().level(Level.FAULT).text("StackAndGroup1CANCommunicationInterrupt")), //
		STATE_176(new Doc().level(Level.FAULT).text("StackAndGroup2CANCommunicationInterrupt")), //
		STATE_177(new Doc().level(Level.WARNING).text("GeneralOvercurrentAlarmAtCellStackCharge")), //
		STATE_178(new Doc().level(Level.WARNING).text("GeneralOvercurrentAlarmAtCellStackDischarge")), //
		STATE_179(new Doc().level(Level.WARNING).text("CurrentLimitAlarmAtCellStackCharge")), //
		STATE_180(new Doc().level(Level.WARNING).text("CurrentLimitAlarmAtCellStackDischarge")), //
		STATE_181(new Doc().level(Level.WARNING).text("GeneralCellStackHighVoltageAlarm")), //
		STATE_182(new Doc().level(Level.WARNING).text("GeneralCellStackLowVoltageAlarm")), //
		STATE_183(new Doc().level(Level.WARNING).text("AbnormalCellStackVoltageChangeAlarm")), //
		STATE_184(new Doc().level(Level.WARNING).text("GeneralCellStackHighTemperatureAlarm")), //
		STATE_185(new Doc().level(Level.WARNING).text("GeneralCellStackLowTemperatureAlarm")), //
		STATE_186(new Doc().level(Level.WARNING).text("AbnormalCellStackTemperatureChangeAlarm")), //
		STATE_187(new Doc().level(Level.WARNING).text("SevereCellStackHighVoltageAlarm")), //
		STATE_188(new Doc().level(Level.WARNING).text("SevereCellStackLowVoltageAlarm")), //
		STATE_189(new Doc().level(Level.WARNING).text("SevereCellStackLowTemperatureAlarm")), //
		STATE_190(new Doc().level(Level.WARNING).text("SeverveOverCurrentAlarmAtCellStackDharge")), //
		STATE_191(new Doc().level(Level.WARNING).text("SeverveOverCurrentAlarmAtCellStackDischarge")), //
		STATE_192(new Doc().level(Level.WARNING).text("AbnormalCellStackCapacityAlarm")), //
		STATE_193(new Doc().level(Level.WARNING).text("TheParameterOfEEPROMInCellStackLoseEffectiveness")), //
		STATE_194(new Doc().level(Level.WARNING).text("IsolatingSwitchInConfluenceArkBreak")), //
		STATE_195(
				new Doc().level(Level.WARNING).text("TheCommunicationBetweenCellStackAndTemperatureOfCollectorBreak")), //
		STATE_196(new Doc().level(Level.WARNING).text("TheTemperatureOfCollectorFail")), //
		STATE_197(new Doc().level(Level.WARNING).text("HallSensorDoNotWorkAccurately")), //
		STATE_198(new Doc().level(Level.WARNING).text("TheCommunicationOfPCSBreak")), //
		STATE_199(new Doc().level(Level.WARNING).text("AdvancedChargingOrMainContactorCloseAbnormally")), //
		STATE_200(new Doc().level(Level.WARNING).text("AbnormalSampledVoltage")), //
		STATE_201(new Doc().level(Level.WARNING).text("AbnormalAdvancedContactorOrAbnormalRS485GalleryOfPCS")), //
		STATE_202(new Doc().level(Level.WARNING).text("AbnormalMainContactor")), //
		STATE_203(new Doc().level(Level.WARNING).text("GeneralCellStackLeakage")), //
		STATE_204(new Doc().level(Level.WARNING).text("SevereCellStackLeakage")), //
		STATE_205(new Doc().level(Level.WARNING).text("SmokeAlarm")), //
		STATE_206(new Doc().level(Level.WARNING).text("TheCommunicationWireToAmmeterBreak")), //
		STATE_207(new Doc().level(Level.WARNING).text("TheCommunicationWireToDredBreak")//
		); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
