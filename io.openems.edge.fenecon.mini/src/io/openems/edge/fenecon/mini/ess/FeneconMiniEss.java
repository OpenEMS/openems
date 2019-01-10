package io.openems.edge.fenecon.mini.ess;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.fenecon.mini.FeneconMiniConstants;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Mini.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FeneconMiniEss extends AbstractOpenemsModbusComponent
		implements SinglePhaseEss, AsymmetricEss, SymmetricEss, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public FeneconMiniEss() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), FeneconMiniConstants.UNIT_ID,
				this.cm, "Modbus", config.modbus_id());
		this.getPhase().setNextValue(config.Phase());
		SinglePhaseEss.initializeCopyPhaseChannel(this, config.Phase());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYSTEM_STATE(new Doc().options(SystemState.values())), //
		CONTROL_MODE(new Doc().options(ControlMode.values())), //
		BATTERY_GROUP_STATE(new Doc().options(BatteryGroupState.values())), //
		SET_WORK_STATE(new Doc().options(SetWorkState.values())),

		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //

		BECU1_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU1_VERSION(new Doc()), //
		BECU1_MIN_VOLT_NO(new Doc()), //
		BECU1_MIN_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_MAX_VOLT_NO(new Doc()), //
		BECU1_MAX_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_MIN_TEMP_NO(new Doc()), //
		BECU1_MIN_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BECU1_MAX_TEMP_NO(new Doc()), //
		BECU1_MAX_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		BECU2_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU2_VERSION(new Doc()), //
		BECU2_MIN_VOLT_NO(new Doc()), //
		BECU2_MIN_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_MAX_VOLT_NO(new Doc()), //
		BECU2_MAX_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_MIN_TEMP_NO(new Doc()), //
		BECU2_MIN_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BECU2_MAX_TEMP_NO(new Doc()), //
		BECU2_MAX_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		SYSTEM_WORK_MODE_STATE(new Doc()), //
		SYSTEM_WORK_STATE(new Doc()), //

		BECU_NUM(new Doc()), //
		BECU_WORK_STATE(new Doc()), //
		BECU_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU_CURRENT(new Doc().unit(Unit.AMPERE)), //

		RTC_YEAR(new Doc().text("Year")), //
		RTC_MONTH(new Doc().text("Month")), //
		RTC_DAY(new Doc().text("Day")), //
		RTC_HOUR(new Doc().text("Hour")), //
		RTC_MINUTE(new Doc().text("Minute")), //
		RTC_SECOND(new Doc().text("Second")), //
		SETUP_MODE(new Doc().options(SetupMode.values())), //
		PCS_MODE(new Doc().options(PcsMode.values())), //

		STATE_1(new Doc().level(Level.WARNING).text("BECU1GeneralChargeOverCurrentAlarm")), //
		STATE_2(new Doc().level(Level.WARNING).text("BECU1GeneralDischargeOverCurrentAlarm")), //
		STATE_3(new Doc().level(Level.WARNING).text("BECU1ChargeCurrentLimitAlarm")), //
		STATE_4(new Doc().level(Level.WARNING).text("BECU1DischargeCurrentLimitAlarm")), //
		STATE_5(new Doc().level(Level.WARNING).text("BECU1GeneralHighVoltageAlarm")), //
		STATE_6(new Doc().level(Level.WARNING).text("BECU1GeneralLowVoltageAlarm")), //
		STATE_7(new Doc().level(Level.WARNING).text("BECU1AbnormalVoltageChangeAlarm")), //
		STATE_8(new Doc().level(Level.WARNING).text("BECU1GeneralHighTemperatureAlarm")), //
		STATE_9(new Doc().level(Level.WARNING).text("BECU1GeneralLowTemperatureAlarm")), //
		STATE_10(new Doc().level(Level.WARNING).text("BECU1AbnormalTemperatureChangeAlarm")), //
		STATE_11(new Doc().level(Level.WARNING).text("BECU1SevereHighVoltageAlarm")), //
		STATE_12(new Doc().level(Level.WARNING).text("BECU1SevereLowVoltageAlarm")), //
		STATE_13(new Doc().level(Level.WARNING).text("BECU1SevereLowTemperatureAlarm")), //
		STATE_14(new Doc().level(Level.WARNING).text("BECU1SeverveChargeOverCurrentAlarm")), //
		STATE_15(new Doc().level(Level.WARNING).text("BECU1SeverveDischargeOverCurrentAlarm")), //
		STATE_16(new Doc().level(Level.WARNING).text("BECU1AbnormalCellCapacityAlarm")), //
		STATE_17(new Doc().level(Level.WARNING).text("BECU1BalancedSamplingAlarm")), //
		STATE_18(new Doc().level(Level.WARNING).text("BECU1BalancedControlAlarm")), //
		STATE_19(new Doc().level(Level.WARNING).text("BECU1HallSensorDoesNotWorkAccurately")), //
		STATE_20(new Doc().level(Level.WARNING).text("BECU1Generalleakage")), //
		STATE_21(new Doc().level(Level.WARNING).text("BECU1Severeleakage")), //
		STATE_22(new Doc().level(Level.WARNING).text("BECU1Contactor1TurnOnAbnormity")), //
		STATE_23(new Doc().level(Level.WARNING).text("BECU1Contactor1TurnOffAbnormity")), //
		STATE_24(new Doc().level(Level.WARNING).text("BECU1Contactor2TurnOnAbnormity")), //
		STATE_25(new Doc().level(Level.WARNING).text("BECU1Contactor2TurnOffAbnormity")), //
		STATE_26(new Doc().level(Level.WARNING).text("BECU1Contactor4CheckAbnormity")), //
		STATE_27(new Doc().level(Level.WARNING).text("BECU1ContactorCurrentUnsafe")), //
		STATE_28(new Doc().level(Level.WARNING).text("BECU1Contactor5CheckAbnormity")), //
		STATE_29(new Doc().level(Level.WARNING).text("BECU1HighVoltageOffset")), //
		STATE_30(new Doc().level(Level.WARNING).text("BECU1LowVoltageOffset")), //
		STATE_31(new Doc().level(Level.WARNING).text("BECU1HighTemperatureOffset")), //
		STATE_32(new Doc().level(Level.FAULT).text("BECU1DischargeSevereOvercurrent")), //
		STATE_33(new Doc().level(Level.FAULT).text("BECU1ChargeSevereOvercurrent")), //
		STATE_34(new Doc().level(Level.FAULT).text("BECU1GeneralUndervoltage")), //
		STATE_35(new Doc().level(Level.FAULT).text("BECU1SevereOvervoltage")), //
		STATE_36(new Doc().level(Level.FAULT).text("BECU1GeneralOvervoltage")), //
		STATE_37(new Doc().level(Level.FAULT).text("BECU1SevereUndervoltage")), //
		STATE_38(new Doc().level(Level.FAULT).text("BECU1InsideCANBroken")), //
		STATE_39(new Doc().level(Level.FAULT).text("BECU1GeneralUndervoltageHighCurrentDischarge")), //
		STATE_40(new Doc().level(Level.FAULT).text("BECU1BMUError")), //
		STATE_41(new Doc().level(Level.FAULT).text("BECU1CurrentSamplingInvalidation")), //
		STATE_42(new Doc().level(Level.FAULT).text("BECU1BatteryFail")), //
		STATE_43(new Doc().level(Level.FAULT).text("BECU1TemperatureSamplingBroken")), //
		STATE_44(new Doc().level(Level.FAULT).text("BECU1Contactor1TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_45(new Doc().level(Level.FAULT).text("BECU1Contactor1TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_46(new Doc().level(Level.FAULT).text("BECU1Contactor2TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_47(new Doc().level(Level.FAULT).text("BECU1Contactor2TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_48(new Doc().level(Level.FAULT).text("BECU1SevereHighTemperatureFault")), //
		STATE_49(new Doc().level(Level.FAULT).text("BECU1HallInvalidation")), //
		STATE_50(new Doc().level(Level.FAULT).text("BECU1ContactorInvalidation")), //
		STATE_51(new Doc().level(Level.FAULT).text("BECU1OutsideCANBroken")), //
		STATE_52(new Doc().level(Level.FAULT).text("BECU1CathodeContactorBroken")), //

		STATE_53(new Doc().level(Level.WARNING).text("BECU2GeneralChargeOverCurrentAlarm")), //
		STATE_54(new Doc().level(Level.WARNING).text("BECU2GeneralDischargeOverCurrentAlarm")), //
		STATE_55(new Doc().level(Level.WARNING).text("BECU2ChargeCurrentLimitAlarm")), //
		STATE_56(new Doc().level(Level.WARNING).text("BECU2DischargeCurrentLimitAlarm")), //
		STATE_57(new Doc().level(Level.WARNING).text("BECU2GeneralHighVoltageAlarm")), //
		STATE_58(new Doc().level(Level.WARNING).text("BECU2GeneralLowVoltageAlarm")), //
		STATE_59(new Doc().level(Level.WARNING).text("BECU2AbnormalVoltageChangeAlarm")), //
		STATE_60(new Doc().level(Level.WARNING).text("BECU2GeneralHighTemperatureAlarm")), //
		STATE_61(new Doc().level(Level.WARNING).text("BECU2GeneralLowTemperatureAlarm")), //
		STATE_62(new Doc().level(Level.WARNING).text("BECU2AbnormalTemperatureChangeAlarm")), //
		STATE_63(new Doc().level(Level.WARNING).text("BECU2SevereHighVoltageAlarm")), //
		STATE_64(new Doc().level(Level.WARNING).text("BECU2SevereLowVoltageAlarm")), //
		STATE_65(new Doc().level(Level.WARNING).text("BECU2SevereLowTemperatureAlarm")), //
		STATE_66(new Doc().level(Level.WARNING).text("BECU2SeverveChargeOverCurrentAlarm")), //
		STATE_67(new Doc().level(Level.WARNING).text("BECU2SeverveDischargeOverCurrentAlarm")), //
		STATE_68(new Doc().level(Level.WARNING).text("BECU2AbnormalCellCapacityAlarm")), //
		STATE_69(new Doc().level(Level.WARNING).text("BECU2BalancedSamplingAlarm")), //
		STATE_70(new Doc().level(Level.WARNING).text("BECU2BalancedControlAlarm")), //
		STATE_71(new Doc().level(Level.WARNING).text("BECU2HallSensorDoesNotWorkAccurately")), //
		STATE_72(new Doc().level(Level.WARNING).text("BECU2Generalleakage")), //
		STATE_73(new Doc().level(Level.WARNING).text("BECU2Severeleakage")), //
		STATE_74(new Doc().level(Level.WARNING).text("BECU2Contactor1TurnOnAbnormity")), //
		STATE_75(new Doc().level(Level.WARNING).text("BECU2Contactor1TurnOffAbnormity")), //
		STATE_76(new Doc().level(Level.WARNING).text("BECU2Contactor2TurnOnAbnormity")), //
		STATE_77(new Doc().level(Level.WARNING).text("BECU2Contactor2TurnOffAbnormity")), //
		STATE_78(new Doc().level(Level.WARNING).text("BECU2Contactor4CheckAbnormity")), //
		STATE_79(new Doc().level(Level.WARNING).text("BECU2ContactorCurrentUnsafe")), //
		STATE_80(new Doc().level(Level.WARNING).text("BECU2Contactor5CheckAbnormity")), //
		STATE_81(new Doc().level(Level.WARNING).text("BECU2HighVoltageOffset")), //
		STATE_82(new Doc().level(Level.WARNING).text("BECU2LowVoltageOffset")), //
		STATE_83(new Doc().level(Level.WARNING).text("BECU2HighTemperatureOffset")), //
		STATE_84(new Doc().level(Level.FAULT).text("BECU2DischargeSevereOvercurrent")), //
		STATE_85(new Doc().level(Level.FAULT).text("BECU2ChargeSevereOvercurrent")), //
		STATE_86(new Doc().level(Level.FAULT).text("BECU2GeneralUndervoltage")), //
		STATE_87(new Doc().level(Level.FAULT).text("BECU2SevereOvervoltage")), //
		STATE_88(new Doc().level(Level.FAULT).text("BECU2GeneralOvervoltage")), //
		STATE_89(new Doc().level(Level.FAULT).text("BECU2SevereUndervoltage")), //
		STATE_90(new Doc().level(Level.FAULT).text("BECU2InsideCANBroken")), //
		STATE_91(new Doc().level(Level.FAULT).text("BECU2GeneralUndervoltageHighCurrentDischarge")), //
		STATE_92(new Doc().level(Level.FAULT).text("BECU2BMUError")), //
		STATE_93(new Doc().level(Level.FAULT).text("BECU2CurrentSamplingInvalidation")), //
		STATE_94(new Doc().level(Level.FAULT).text("BECU2BatteryFail")), //
		STATE_95(new Doc().level(Level.FAULT).text("BECU2TemperatureSamplingBroken")), //
		STATE_96(new Doc().level(Level.FAULT).text("BECU2Contactor1TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_97(new Doc().level(Level.FAULT).text("BECU2Contactor1TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_98(new Doc().level(Level.FAULT).text("BECU2Contactor2TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_99(new Doc().level(Level.FAULT).text("BECU2Contactor2TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_100(new Doc().level(Level.FAULT).text("BECU2SevereHighTemperatureFault")), //
		STATE_101(new Doc().level(Level.FAULT).text("BECU2HallInvalidation")), //
		STATE_102(new Doc().level(Level.FAULT).text("BECU2ContactorInvalidation")), //
		STATE_103(new Doc().level(Level.FAULT).text("BECU2OutsideCANBroken")), //
		STATE_104(new Doc().level(Level.FAULT).text("BECU2CathodeContactorBroken")), //

		STATE_105(new Doc().level(Level.FAULT).text("NoAvailableBatteryGroup")), //
		STATE_106(new Doc().level(Level.FAULT).text("StackGeneralLeakage")), //
		STATE_107(new Doc().level(Level.FAULT).text("StackSevereLeakage")), //
		STATE_108(new Doc().level(Level.FAULT).text("StackStartingFail")), //
		STATE_109(new Doc().level(Level.FAULT).text("StackStoppingFail")), //
		STATE_110(new Doc().level(Level.FAULT).text("BatteryProtection")), //
		STATE_111(new Doc().level(Level.FAULT).text("StackAndGroup1CANCommunicationInterrupt")), //
		STATE_112(new Doc().level(Level.FAULT).text("StackAndGroup2CANCommunicationInterrupt")), //
		STATE_113(new Doc().level(Level.WARNING).text("GeneralOvercurrentAlarmAtCellStackCharge")), //
		STATE_114(new Doc().level(Level.WARNING).text("GeneralOvercurrentAlarmAtCellStackDischarge")), //
		STATE_115(new Doc().level(Level.WARNING).text("CurrentLimitAlarmAtCellStackCharge")), //
		STATE_116(new Doc().level(Level.WARNING).text("CurrentLimitAlarmAtCellStackDischarge")), //
		STATE_117(new Doc().level(Level.WARNING).text("GeneralCellStackHighVoltageAlarm")), //
		STATE_118(new Doc().level(Level.WARNING).text("GeneralCellStackLowVoltageAlarm")), //
		STATE_119(new Doc().level(Level.WARNING).text("AbnormalCellStackVoltageChangeAlarm")), //
		STATE_120(new Doc().level(Level.WARNING).text("GeneralCellStackHighTemperatureAlarm")), //
		STATE_121(new Doc().level(Level.WARNING).text("GeneralCellStackLowTemperatureAlarm")), //
		STATE_122(new Doc().level(Level.WARNING).text("AbnormalCellStackTemperatureChangeAlarm")), //
		STATE_123(new Doc().level(Level.WARNING).text("SevereCellStackHighVoltageAlarm")), //
		STATE_124(new Doc().level(Level.WARNING).text("SevereCellStackLowVoltageAlarm")), //
		STATE_125(new Doc().level(Level.WARNING).text("SevereCellStackLowTemperatureAlarm")), //
		STATE_126(new Doc().level(Level.WARNING).text("SeverveOverCurrentAlarmAtCellStackDharge")), //
		STATE_127(new Doc().level(Level.WARNING).text("SeverveOverCurrentAlarmAtCellStackDischarge")), //
		STATE_128(new Doc().level(Level.WARNING).text("AbnormalCellStackCapacityAlarm")), //
		STATE_129(new Doc().level(Level.WARNING).text("TheParameterOfEEPROMInCellStackLoseEffectiveness")), //
		STATE_130(new Doc().level(Level.WARNING).text("IsolatingSwitchInConfluenceArkBreak")), //
		STATE_131(
				new Doc().level(Level.WARNING).text("TheCommunicationBetweenCellStackAndTemperatureOfCollectorBreak")), //
		STATE_132(new Doc().level(Level.WARNING).text("TheTemperatureOfCollectorFail")), //
		STATE_133(new Doc().level(Level.WARNING).text("HallSensorDoNotWorkAccurately")), //
		STATE_134(new Doc().level(Level.WARNING).text("TheCommunicationOfPCSBreak")), //
		STATE_135(new Doc().level(Level.WARNING).text("AdvancedChargingOrMainContactorCloseAbnormally")), //
		STATE_136(new Doc().level(Level.WARNING).text("AbnormalSampledVoltage")), //
		STATE_137(new Doc().level(Level.WARNING).text("AbnormalAdvancedContactorOrAbnormalRS485GalleryOfPCS")), //
		STATE_138(new Doc().level(Level.WARNING).text("AbnormalMainContactor")), //
		STATE_139(new Doc().level(Level.WARNING).text("GeneralCellStackLeakage")), //
		STATE_140(new Doc().level(Level.WARNING).text("SevereCellStackLeakage")), //
		STATE_141(new Doc().level(Level.WARNING).text("SmokeAlarm")), //
		STATE_142(new Doc().level(Level.WARNING).text("TheCommunicationWireToAmmeterBreak")), //
		STATE_143(new Doc().level(Level.WARNING).text("TheCommunicationWireToDredBreak")//
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

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(FeneconMiniEss.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(FeneconMiniEss.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						new DummyRegisterElement(109), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(FeneconMiniEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(FeneconMiniEss.ChannelId.BATTERY_POWER, new SignedWordElement(112))), //
				new FC3ReadRegistersTask(2007, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(2007),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(2107, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(2107),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(2207, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(2207),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(3000, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BECU1_CHARGE_CURRENT, new UnsignedWordElement(3000)), //
						m(FeneconMiniEss.ChannelId.BECU1_DISCHARGE_CURRENT, new UnsignedWordElement(3001)), //
						m(FeneconMiniEss.ChannelId.BECU1_VOLT, new UnsignedWordElement(3002)), //
						m(FeneconMiniEss.ChannelId.BECU1_CURRENT, new UnsignedWordElement(3003)), //
						m(FeneconMiniEss.ChannelId.BECU1_SOC, new UnsignedWordElement(3004))), //
				new FC3ReadRegistersTask(3005, Priority.LOW, //
						bm(new UnsignedWordElement(3005))//
								.m(FeneconMiniEss.ChannelId.STATE_1, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_2, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_3, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_4, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_5, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_6, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_7, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_8, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_9, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_10, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_11, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_12, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_13, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_14, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_15, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_16, 15)//
								.build(), //
						bm(new UnsignedWordElement(3006))//
								.m(FeneconMiniEss.ChannelId.STATE_17, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_18, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_19, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_20, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_21, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_22, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_23, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_24, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_25, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_26, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_27, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_28, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_29, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_30, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_31, 15)//
								.build(), //
						bm(new UnsignedWordElement(3007))//
								.m(FeneconMiniEss.ChannelId.STATE_32, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_33, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_34, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_35, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_36, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_37, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_38, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_39, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_40, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_41, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_42, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_43, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_44, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_45, 15)//
								.build(), //
						bm(new UnsignedWordElement(3008))//
								.m(FeneconMiniEss.ChannelId.STATE_46, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_47, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_48, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_49, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_50, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_51, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_52, 13)//
								.build(), //
						m(FeneconMiniEss.ChannelId.BECU1_VERSION, new UnsignedWordElement(3009)), //
						new DummyRegisterElement(3010, 3011), //
						m(FeneconMiniEss.ChannelId.BECU1_MIN_VOLT_NO, new UnsignedWordElement(3012)), //
						m(FeneconMiniEss.ChannelId.BECU1_MIN_VOLT, new UnsignedWordElement(3013)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_VOLT_NO, new UnsignedWordElement(3014)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_VOLT, new UnsignedWordElement(3015)), // ^
						m(FeneconMiniEss.ChannelId.BECU1_MIN_TEMP_NO, new UnsignedWordElement(3016)), //
						m(FeneconMiniEss.ChannelId.BECU1_MIN_TEMP, new UnsignedWordElement(3017)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_TEMP_NO, new UnsignedWordElement(3018)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_TEMP, new UnsignedWordElement(3019))), //

				new FC3ReadRegistersTask(3200, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BECU2_CHARGE_CURRENT, new UnsignedWordElement(3200)), //
						m(FeneconMiniEss.ChannelId.BECU2_DISCHARGE_CURRENT, new UnsignedWordElement(3201)), //
						m(FeneconMiniEss.ChannelId.BECU2_VOLT, new UnsignedWordElement(3202)), //
						m(FeneconMiniEss.ChannelId.BECU2_CURRENT, new UnsignedWordElement(3203)), //
						m(FeneconMiniEss.ChannelId.BECU2_SOC, new UnsignedWordElement(3204))), //
				new FC3ReadRegistersTask(3205, Priority.LOW, //
						bm(new UnsignedWordElement(3205))//
								.m(FeneconMiniEss.ChannelId.STATE_53, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_54, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_55, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_56, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_57, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_58, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_59, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_60, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_61, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_62, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_63, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_64, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_65, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_66, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_67, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_68, 15)//
								.build(), //
						bm(new UnsignedWordElement(3206))//
								.m(FeneconMiniEss.ChannelId.STATE_69, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_70, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_71, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_72, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_73, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_74, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_75, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_76, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_77, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_78, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_79, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_80, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_81, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_82, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_83, 15)//
								.build(), //
						bm(new UnsignedWordElement(3207))//
								.m(FeneconMiniEss.ChannelId.STATE_84, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_85, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_86, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_87, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_88, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_89, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_90, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_91, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_92, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_93, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_94, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_95, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_96, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_97, 15)//
								.build(), //
						bm(new UnsignedWordElement(3208))//
								.m(FeneconMiniEss.ChannelId.STATE_98, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_99, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_100, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_101, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_102, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_103, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_104, 13)//
								.build(), //
						m(FeneconMiniEss.ChannelId.BECU2_VERSION, new UnsignedWordElement(3209)), //
						new DummyRegisterElement(3210, 3211), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_VOLT_NO, new UnsignedWordElement(3212)), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_VOLT, new UnsignedWordElement(3213)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_VOLT_NO, new UnsignedWordElement(3214)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_VOLT, new UnsignedWordElement(3215)), // ^
						m(FeneconMiniEss.ChannelId.BECU2_MIN_TEMP_NO, new UnsignedWordElement(3216)), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_TEMP, new UnsignedWordElement(3217)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_TEMP_NO, new UnsignedWordElement(3218)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_TEMP, new UnsignedWordElement(3219))), //
				new FC3ReadRegistersTask(4000, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.SYSTEM_WORK_STATE, new UnsignedDoublewordElement(4000)), //
						m(FeneconMiniEss.ChannelId.SYSTEM_WORK_MODE_STATE, new UnsignedDoublewordElement(4002))), //
				new FC3ReadRegistersTask(4800, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BECU_NUM, new UnsignedWordElement(4800)), //
						// TODO BECU_WORK_STATE has been implemented with both registers(4801 and 4807)
						m(FeneconMiniEss.ChannelId.BECU_WORK_STATE, new UnsignedWordElement(4801)), //
						new DummyRegisterElement(4802), //
						m(FeneconMiniEss.ChannelId.BECU_CHARGE_CURRENT, new UnsignedWordElement(4803)), //
						m(FeneconMiniEss.ChannelId.BECU_DISCHARGE_CURRENT, new UnsignedWordElement(4804)), //
						m(FeneconMiniEss.ChannelId.BECU_VOLT, new UnsignedWordElement(4805)), //
						m(FeneconMiniEss.ChannelId.BECU_CURRENT, new UnsignedWordElement(4806))), //
				new FC3ReadRegistersTask(4808, Priority.LOW, //
						bm(new UnsignedWordElement(4808))//
								.m(FeneconMiniEss.ChannelId.STATE_105, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_106, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_107, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_108, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_109, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_110, 9)//
								.build(), //
						bm(new UnsignedWordElement(4809))//
								.m(FeneconMiniEss.ChannelId.STATE_111, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_112, 1)//
								.build(), //
						bm(new UnsignedWordElement(4810))//
								.m(FeneconMiniEss.ChannelId.STATE_113, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_114, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_115, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_116, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_117, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_118, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_119, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_120, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_121, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_122, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_123, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_124, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_125, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_126, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_127, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_128, 15)//
								.build(), //
						bm(new UnsignedWordElement(4811))//
								.m(FeneconMiniEss.ChannelId.STATE_129, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_130, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_131, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_132, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_133, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_134, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_135, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_136, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_137, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_138, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_139, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_140, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_141, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_142, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_143, 14)//
								.build(),

						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(4812))//
				), //

				new FC3ReadRegistersTask(30166, Priority.LOW, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(30166))), //
				new FC16WriteRegistersTask(9014, //
						m(FeneconMiniEss.ChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(FeneconMiniEss.ChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(FeneconMiniEss.ChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(FeneconMiniEss.ChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(FeneconMiniEss.ChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(FeneconMiniEss.ChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(FeneconMiniEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(FeneconMiniEss.ChannelId.PCS_MODE, new UnsignedWordElement(30559))), //
				new FC16WriteRegistersTask(30157, //
						m(FeneconMiniEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(FeneconMiniEss.ChannelId.PCS_MODE, new UnsignedWordElement(30158))));//

	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString(); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				SymmetricEss.getModbusSlaveNatureTable(), //
				AsymmetricEss.getModbusSlaveNatureTable(), //
				ModbusSlaveNatureTable.of(FeneconMiniEss.class, 300) //
						.build());
	}

	private final static ElementToChannelConverter UNSIGNED_POWER_CONVERTER = new ElementToChannelConverter( //
			// element -> channel
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Integer) value;
				if (intValue == 0) {
					return 0; // ignore '0'
				}
				return intValue - 10_000; // apply delta of 10_000
			}, //
				// channel -> element
			value -> value);
}