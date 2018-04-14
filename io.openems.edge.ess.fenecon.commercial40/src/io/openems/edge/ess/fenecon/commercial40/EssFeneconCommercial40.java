package io.openems.edge.ess.fenecon.commercial40;

import java.util.Arrays;
import java.util.stream.Stream;

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
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.Priority;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadHoldingRegisterTask;
import io.openems.edge.bridge.modbus.api.task._TODO_WriteRegisterTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Fenecon.Commercial40", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class EssFeneconCommercial40 extends AbstractOpenemsModbusComponent
		implements EssSymmetricReadonly, OpenemsComponent {

	private final static int UNIT_ID = 100;

	public EssFeneconCommercial40() {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(Ess.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(EssSymmetricReadonly.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CHARGE_ACTIVE_POWER:
					case DISCHARGE_ACTIVE_POWER:
					case CHARGE_REACTIVE_POWER:
					case DISCHARGE_REACTIVE_POWER:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(EssFeneconCommercial40.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SYSTEM_STATE:
					case CONTROL_MODE:
					case BATTERY_MAINTENANCE_STATE:
					case INVERTER_STATE:
					case GRID_MODE:
					case PROTOCOL_VERSION:
					case SYSTEM_MANUFACTURER:
					case SYSTEM_TYPE:
					case BATTERY_STRING_SWITCH_STATE:
					case BATTERY_VOLTAGE:
					case BATTERY_CURRENT:
					case BATTERY_POWER:
					case GRID_ACTIVE_POWER:
					case APPARENT_POWER:
					case CURRENT_L1:
					case CURRENT_L2:
					case CURRENT_L3:
					case FREQUENCY:
					case VOLTAGE_L1:
					case VOLTAGE_L2:
					case VOLTAGE_L3:
					case ALLOWED_APPARENT:
					case ALLOWED_CHARGE:
					case ALLOWED_DISCHARGE:
					case INVERTER_CURRENT_L1:
					case INVERTER_CURRENT_L2:
					case INVERTER_CURRENT_L3:
					case INVERTER_VOLTAGE_L1:
					case INVERTER_VOLTAGE_L2:
					case INVERTER_VOLTAGE_L3:
					case IPM_TEMPERATURE_L1:
					case IPM_TEMPERATURE_L2:
					case IPM_TEMPERATURE_L3:
					case SET_WORK_STATE:
					case TRANSFORMER_TEMPERATURE_L2:
					case BMS_DCDC_WORK_MODE:
					case BMS_DCDC_WORK_STATE:
						return new IntegerReadChannel(this, channelId);
					case AC_CHARGE_ENERGY:
					case AC_DISCHARGE_ENERGY:
						return new LongReadChannel(this, channelId);
					case SET_CHARGE_ACTIVE_POWER:
					case SET_DISCHARGE_ACTIVE_POWER:
					case SET_CHARGE_REACTIVE_POWER:
					case SET_DISCHARGE_REACTIVE_POWER:
					case SET_PV_POWER_LIMIT:
						return new IntegerWriteChannel(this, channelId);
					case STATE_0:
					case STATE_1:
					case STATE_2:
					case STATE_3:
					case STATE_4:
					case STATE_5:
					case STATE_6:
					case STATE_7:
					case STATE_8:
					case STATE_9:
					case STATE_10:
					case STATE_11:
					case STATE_12:
					case STATE_13:
					case STATE_14:
					case STATE_15:
					case STATE_16:
					case STATE_17:
					case STATE_18:
					case STATE_19:
					case STATE_20:
					case STATE_21:
					case STATE_22:
					case STATE_23:
					case STATE_24:
					case STATE_25:
					case STATE_26:
					case STATE_27:
					case STATE_28:
					case STATE_29:
					case STATE_30:
					case STATE_31:
					case STATE_32:
					case STATE_33:
					case STATE_34:
					case STATE_35:
					case STATE_36:
					case STATE_37:
					case STATE_38:
					case STATE_39:
					case STATE_40:
					case STATE_41:
					case STATE_42:
					case STATE_43:
					case STATE_44:
					case STATE_45:
					case STATE_46:
					case STATE_47:
					case STATE_48:
					case STATE_49:
					case STATE_50:
					case STATE_51:
					case STATE_52:
					case STATE_53:
					case STATE_54:
					case STATE_55:
					case STATE_56:
					case STATE_57:
					case STATE_58:
					case STATE_59:
					case STATE_60:
					case STATE_61:
					case STATE_62:
					case STATE_63:
					case STATE_64:
					case STATE_65:
					case STATE_66:
					case STATE_67:
					case STATE_68:
					case STATE_69:
					case STATE_70:
					case STATE_71:
					case STATE_72:
					case STATE_73:
					case STATE_74:
					case STATE_75:
					case STATE_76:
					case STATE_77:
					case STATE_78:
					case STATE_79:
					case STATE_80:
					case STATE_81:
					case STATE_82:
					case STATE_83:
					case STATE_84:
					case STATE_85:
					case STATE_86:
					case STATE_87:
					case STATE_88:
					case STATE_89:
					case STATE_90:
					case STATE_91:
					case STATE_92:
					case STATE_93:
					case STATE_94:
					case STATE_95:
					case STATE_96:
					case STATE_97:
					case STATE_98:
					case STATE_99:
					case STATE_100:
					case STATE_101:
					case STATE_102:
					case STATE_103:
					case STATE_104:
					case STATE_105:
					case STATE_106:
					case STATE_107:
					case STATE_108:
					case STATE_109:
					case STATE_110:
					case STATE_111:
					case STATE_112:
					case STATE_113:
					case STATE_114:
					case STATE_115:
					case STATE_116:
					case STATE_117:
					case STATE_118:
					case STATE_119:
					case STATE_120:
					case STATE_121:
					case STATE_122:
					case STATE_123:
					case STATE_124:
					case STATE_125:
					case STATE_126:
					case STATE_127:
					case STATE_128:
					case STATE_129:
					case STATE_130:
					case STATE_131:
					case STATE_132:
					case STATE_133:
					case STATE_134:
					case STATE_135:
					case STATE_136:
					case STATE_137:
					case STATE_138:
					case STATE_139:
					case STATE_140:
					case STATE_141:
					case STATE_142:
					case STATE_143:
					case STATE_144:
					case STATE_145:
					case STATE_146:
					case STATE_147:
					case STATE_148:
					case STATE_149:
						return new BooleanReadChannel(this, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setModbus(BridgeModbusTcp modbus) {
		super.setModbus(modbus);
	}

	protected void unsetModbus(BridgeModbusTcp modbus) {
		super.unsetModbus(modbus);
	}

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled(), UNIT_ID);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYSTEM_STATE(new Doc() //
				.option(2, "Stop") //
				.option(4, "PV-Charge") //
				.option(8, "Standby") //
				.option(16, "Start") //
				.option(32, "Fault") //
				.option(64, "Debug")), //
		CONTROL_MODE(new Doc() //
				.option(1, "Remote") //
				.option(2, "Local")), //
		BATTERY_MAINTENANCE_STATE(new Doc() //
				.option(0, "Off") //
				.option(1, "On")), //
		INVERTER_STATE(new Doc() //
				.option(0, "Init") //
				.option(2, "Fault") //
				.option(4, "Stop") //
				.option(8, "Standby") //
				.option(16, "Grid-Monitor") // ,
				.option(32, "Ready") //
				.option(64, "Start") //
				.option(128, "Debug")), //
		GRID_MODE(new Doc() //
				.option(1, "Off-Grid") //
				.option(2, "On-Grid")), //
		PROTOCOL_VERSION(new Doc()), //
		SYSTEM_MANUFACTURER(new Doc() //
				.option(1, "BYD")), //
		SYSTEM_TYPE(new Doc() //
				.option(1, "CESS")), //
		BATTERY_STRING_SWITCH_STATE(new Doc() //
				.option(1, "Main contactor") //
				.option(2, "Precharge contactor") //
				.option(4, "FAN contactor") //
				.option(8, "BMU power supply relay") //
				.option(16, "Middle relay")), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		AC_CHARGE_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		AC_DISCHARGE_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		GRID_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		APPARENT_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		CURRENT_L1(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(new Doc().unit(Unit.MILLIAMPERE)), //
		VOLTAGE_L1(new Doc().unit(Unit.MILLIVOLT)), //
		VOLTAGE_L2(new Doc().unit(Unit.MILLIVOLT)), //
		VOLTAGE_L3(new Doc().unit(Unit.MILLIVOLT)), //
		FREQUENCY(new Doc().unit(Unit.MILLIHERTZ)), //
		INVERTER_VOLTAGE_L1(new Doc().unit(Unit.MILLIVOLT)), //
		INVERTER_VOLTAGE_L2(new Doc().unit(Unit.MILLIVOLT)), //
		INVERTER_VOLTAGE_L3(new Doc().unit(Unit.MILLIVOLT)), //
		INVERTER_CURRENT_L1(new Doc().unit(Unit.MILLIAMPERE)), //
		INVERTER_CURRENT_L2(new Doc().unit(Unit.MILLIAMPERE)), //
		INVERTER_CURRENT_L3(new Doc().unit(Unit.MILLIAMPERE)), //
		ALLOWED_CHARGE(new Doc().unit(Unit.WATT)), //
		ALLOWED_DISCHARGE(new Doc().unit(Unit.WATT)), //
		ALLOWED_APPARENT(new Doc().unit(Unit.VOLT_AMPERE)), //
		IPM_TEMPERATURE_L1(new Doc().unit(Unit.DEGREE_CELCIUS)), //
		IPM_TEMPERATURE_L2(new Doc().unit(Unit.DEGREE_CELCIUS)), //
		IPM_TEMPERATURE_L3(new Doc().unit(Unit.DEGREE_CELCIUS)), //
		TRANSFORMER_TEMPERATURE_L2(new Doc().unit(Unit.DEGREE_CELCIUS)), //
		SET_WORK_STATE(new Doc() //
				.option(4, "Stop") //
				.option(32, "Standby") //
				.option(64, "Start")), //
		SET_CHARGE_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_DISCHARGE_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_CHARGE_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		SET_DISCHARGE_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		SET_PV_POWER_LIMIT(new Doc().unit(Unit.WATT)), //
		BMS_DCDC_WORK_STATE(new Doc() //
				.option(2, "Initial") //
				.option(4, "Stop") //
				.option(8, "Ready") //
				.option(16, "Running") //
				.option(32, "Fault") //
				.option(64, "Debug") //
				.option(128, "Locked")), //
		BMS_DCDC_WORK_MODE(new Doc() //
				.option(128, "Constant Current") //
				.option(256, "Constant Voltage") //
				.option(512, "Boost MPPT")), //
		STATE_0(new Doc().level(Level.WARNING).text("Emergency Stop")), //
		STATE_1(new Doc().level(Level.WARNING).text("Key Manual Stop")), //
		STATE_2(new Doc().level(Level.WARNING).text("Transformer Phase B Temperature Sensor Invalidation")), //
		STATE_3(new Doc().level(Level.WARNING).text("SD Memory Card Invalidation")), //
		STATE_4(new Doc().level(Level.WARNING).text("Inverter Communication Abnormity")), //
		STATE_5(new Doc().level(Level.WARNING).text("Battery Stack Communication Abnormity")), //
		STATE_6(new Doc().level(Level.WARNING).text("Multifunctional Ammeter Communication Abnormity")), //
		STATE_7(new Doc().level(Level.WARNING).text("Remote Communication Abnormity")), //
		STATE_8(new Doc().level(Level.WARNING).text("PVDC1 Communication Abnormity")), //
		STATE_9(new Doc().level(Level.WARNING).text("PVDC2 Communication Abnormity")), //
		STATE_10(new Doc().level(Level.WARNING).text("Transformer Severe Overtemperature")), //
		STATE_11(new Doc().level(Level.FAULT).text("DC Precharge Contactor Close Unsuccessfully")), //
		STATE_12(new Doc().level(Level.FAULT).text("AC Precharge Contactor Close Unsuccessfully")), //
		STATE_13(new Doc().level(Level.FAULT).text("AC Main Contactor Close Unsuccessfully")), //
		STATE_14(new Doc().level(Level.FAULT).text("DC Electrical Breaker1 Close Unsuccessfully")), //
		STATE_15(new Doc().level(Level.FAULT).text("DC Main Contactor Close Unsuccessfully")), //
		STATE_16(new Doc().level(Level.FAULT).text("AC Breaker Trip")), //
		STATE_17(new Doc().level(Level.FAULT).text("AC Main Contactor Open When Running")), //
		STATE_18(new Doc().level(Level.FAULT).text("DC Main Contactor Open When Running")), //
		STATE_19(new Doc().level(Level.FAULT).text("AC Main Contactor Open Unsuccessfully")), //
		STATE_20(new Doc().level(Level.FAULT).text("DC Electrical Breaker1 Open Unsuccessfully")), //
		STATE_21(new Doc().level(Level.FAULT).text("DC Main Contactor Open Unsuccessfully")), //
		STATE_22(new Doc().level(Level.FAULT).text("Hardware PDP Fault")), //
		STATE_23(new Doc().level(Level.FAULT).text("Master Stop Suddenly")), //
		STATE_24(new Doc().level(Level.FAULT).text("DCShortCircuitProtection")), //
		STATE_25(new Doc().level(Level.FAULT).text("DCOvervoltageProtection")), //
		STATE_26(new Doc().level(Level.FAULT).text("DCUndervoltageProtection")), //
		STATE_27(new Doc().level(Level.FAULT).text("DCInverseNoConnectionProtection")), //
		STATE_28(new Doc().level(Level.FAULT).text("DCDisconnectionProtection")), //
		STATE_29(new Doc().level(Level.FAULT).text("CommutingVoltageAbnormityProtection")), //
		STATE_30(new Doc().level(Level.FAULT).text("DCOvercurrentProtection")), //
		STATE_31(new Doc().level(Level.FAULT).text("Phase1PeakCurrentOverLimitProtection")), //
		STATE_32(new Doc().level(Level.FAULT).text("Phase2PeakCurrentOverLimitProtection")), //
		STATE_33(new Doc().level(Level.FAULT).text("Phase3PeakCurrentOverLimitProtection")), //
		STATE_34(new Doc().level(Level.FAULT).text("Phase1GridVoltageSamplingInvalidation")), //
		STATE_35(new Doc().level(Level.FAULT).text("Phase2VirtualCurrentOverLimitProtection")), //
		STATE_36(new Doc().level(Level.FAULT).text("Phase3VirtualCurrentOverLimitProtection")), //
		STATE_37(new Doc().level(Level.FAULT).text("Phase1GridVoltageSamplingInvalidation2")), //
		STATE_38(new Doc().level(Level.FAULT).text("Phase2ridVoltageSamplingInvalidation")), //
		STATE_39(new Doc().level(Level.FAULT).text("Phase3GridVoltageSamplingInvalidation")), //
		STATE_40(new Doc().level(Level.FAULT).text("Phase1InvertVoltageSamplingInvalidation")), //
		STATE_41(new Doc().level(Level.FAULT).text("Phase2InvertVoltageSamplingInvalidation")), //
		STATE_42(new Doc().level(Level.FAULT).text("Phase3InvertVoltageSamplingInvalidation")), //
		STATE_43(new Doc().level(Level.FAULT).text("ACCurrentSamplingInvalidation")), //
		STATE_44(new Doc().level(Level.FAULT).text("DCCurrentSamplingInvalidation")), //
		STATE_45(new Doc().level(Level.FAULT).text("Phase1OvertemperatureProtection")), //
		STATE_46(new Doc().level(Level.FAULT).text("Phase2OvertemperatureProtection")), //
		STATE_47(new Doc().level(Level.FAULT).text("Phase3OvertemperatureProtection")), //
		STATE_48(new Doc().level(Level.FAULT).text("Phase1TemperatureSamplingInvalidation")), //
		STATE_49(new Doc().level(Level.FAULT).text("Phase2TemperatureSamplingInvalidation")), //
		STATE_50(new Doc().level(Level.FAULT).text("Phase3TemperatureSamplingInvalidation")), //
		STATE_51(new Doc().level(Level.FAULT).text("Phase1PrechargeUnmetProtection")), //
		STATE_52(new Doc().level(Level.FAULT).text("Phase2PrechargeUnmetProtection")), //
		STATE_53(new Doc().level(Level.FAULT).text("Phase3PrechargeUnmetProtection")), //
		STATE_54(new Doc().level(Level.FAULT).text("UnadaptablePhaseSequenceErrorProtection")), //
		STATE_55(new Doc().level(Level.FAULT).text("DSPProtection")), //
		STATE_56(new Doc().level(Level.FAULT).text("Phase1GridVoltageSevereOvervoltageProtection")), //
		STATE_57(new Doc().level(Level.FAULT).text("Phase1GridVoltageGeneralOvervoltageProtection")), //
		STATE_58(new Doc().level(Level.FAULT).text("Phase2GridVoltageSevereOvervoltageProtection")), //
		STATE_59(new Doc().level(Level.FAULT).text("Phase2GridVoltageGeneralOvervoltageProtection")), //
		STATE_60(new Doc().level(Level.FAULT).text("Phase3GridVoltageSevereOvervoltageProtection")), //
		STATE_61(new Doc().level(Level.FAULT).text("Phase3GridVoltageGeneralOvervoltageProtection")), //
		STATE_62(new Doc().level(Level.FAULT).text("Phase1GridVoltageSevereUndervoltageProtection")), //
		STATE_63(new Doc().level(Level.FAULT).text("Phase1GridVoltageGeneralUndervoltageProtection")), //
		STATE_64(new Doc().level(Level.FAULT).text("Phase2GridVoltageSevereUndervoltageProtection")), //
		STATE_65(new Doc().level(Level.FAULT).text("Phase2GridVoltageGeneralUndervoltageProtection")), //
		STATE_66(new Doc().level(Level.FAULT).text("Phase3GridVoltageSevereUndervoltageProtection")), //
		STATE_67(new Doc().level(Level.FAULT).text("Phase3GridVoltageGeneralUndervoltageProtection")), //
		STATE_68(new Doc().level(Level.FAULT).text("SevereOverfrequncyProtection")), //
		STATE_69(new Doc().level(Level.FAULT).text("GeneralOverfrequncyProtection")), //
		STATE_70(new Doc().level(Level.FAULT).text("SevereUnderfrequncyProtection")), //
		STATE_71(new Doc().level(Level.FAULT).text("GeneralsUnderfrequncyProtection")), //
		STATE_72(new Doc().level(Level.FAULT).text("Phase1Gridloss")), //
		STATE_73(new Doc().level(Level.FAULT).text("Phase2Gridloss")), //
		STATE_74(new Doc().level(Level.FAULT).text("Phase3Gridloss")), //
		STATE_75(new Doc().level(Level.FAULT).text("IslandingProtection")), //
		STATE_76(new Doc().level(Level.FAULT).text("Phase1UnderVoltageRideThrough")), //
		STATE_77(new Doc().level(Level.FAULT).text("Phase2UnderVoltageRideThrough")), //
		STATE_78(new Doc().level(Level.FAULT).text("Phase3UnderVoltageRideThrough")), //
		STATE_79(new Doc().level(Level.FAULT).text("Phase1InverterVoltageSevereOvervoltageProtection")), //
		STATE_80(new Doc().level(Level.FAULT).text("Phase1InverterVoltageGeneralOvervoltageProtection")), //
		STATE_81(new Doc().level(Level.FAULT).text("Phase2InverterVoltageSevereOvervoltageProtection")), //
		STATE_82(new Doc().level(Level.FAULT).text("Phase2InverterVoltageGeneralOvervoltageProtection")), //
		STATE_83(new Doc().level(Level.FAULT).text("Phase3InverterVoltageSevereOvervoltageProtection")), //
		STATE_84(new Doc().level(Level.FAULT).text("Phase3InverterVoltageGeneralOvervoltageProtection")), //
		STATE_85(new Doc().level(Level.FAULT).text("InverterPeakVoltageHighProtectionCauseByACDisconnect")), //
		STATE_86(new Doc().level(Level.WARNING).text("DCPrechargeContactorInspectionAbnormity")), //
		STATE_87(new Doc().level(Level.WARNING).text("DCBreaker1InspectionAbnormity")), //
		STATE_88(new Doc().level(Level.WARNING).text("DCBreaker2InspectionAbnormity")), //
		STATE_89(new Doc().level(Level.WARNING).text("ACPrechargeContactorInspectionAbnormity")), //
		STATE_90(new Doc().level(Level.WARNING).text("ACMainontactorInspectionAbnormity")), //
		STATE_91(new Doc().level(Level.WARNING).text("ACBreakerInspectionAbnormity")), //
		STATE_92(new Doc().level(Level.WARNING).text("DCBreaker1CloseUnsuccessfully")), //
		STATE_93(new Doc().level(Level.WARNING).text("DCBreaker2CloseUnsuccessfully")), //
		STATE_94(new Doc().level(Level.WARNING).text("ControlSignalCloseAbnormallyInspectedBySystem")), //
		STATE_95(new Doc().level(Level.WARNING).text("ControlSignalOpenAbnormallyInspectedBySystem")), //
		STATE_96(new Doc().level(Level.WARNING).text("NeutralWireContactorCloseUnsuccessfully")), //
		STATE_97(new Doc().level(Level.WARNING).text("NeutralWireContactorOpenUnsuccessfully")), //
		STATE_98(new Doc().level(Level.WARNING).text("WorkDoorOpen")), //
		STATE_99(new Doc().level(Level.WARNING).text("Emergency1Stop")), //
		STATE_100(new Doc().level(Level.WARNING).text("ACBreakerCloseUnsuccessfully")), //
		STATE_101(new Doc().level(Level.WARNING).text("ControlSwitchStop")), //
		STATE_102(new Doc().level(Level.WARNING).text("GeneralOverload")), //
		STATE_103(new Doc().level(Level.WARNING).text("SevereOverload")), //
		STATE_104(new Doc().level(Level.WARNING).text("BatteryCurrentOverLimit")), //
		STATE_105(new Doc().level(Level.WARNING).text("PowerDecreaseCausedByOvertemperature")), //
		STATE_106(new Doc().level(Level.WARNING).text("InverterGeneralOvertemperature")), //
		STATE_107(new Doc().level(Level.WARNING).text("ACThreePhaseCurrentUnbalance")), //
		STATE_108(new Doc().level(Level.WARNING).text("RestoreFactorySettingUnsuccessfully")), //
		STATE_109(new Doc().level(Level.WARNING).text("PoleBoardInvalidation")), //
		STATE_110(new Doc().level(Level.WARNING).text("SelfInspectionFailed")), //
		STATE_111(new Doc().level(Level.WARNING).text("ReceiveBMSFaultAndStop")), //
		STATE_112(new Doc().level(Level.WARNING).text("RefrigerationEquipmentinvalidation")), //
		STATE_113(new Doc().level(Level.WARNING).text("LargeTemperatureDifferenceAmongIGBTThreePhases")), //
		STATE_114(new Doc().level(Level.WARNING).text("EEPROMParametersOverRange")), //
		STATE_115(new Doc().level(Level.WARNING).text("EEPROMParametersBackupFailed")), //
		STATE_116(new Doc().level(Level.WARNING).text("DCBreakerCloseunsuccessfully")), //
		STATE_117(new Doc().level(Level.WARNING).text("CommunicationBetweenInverterAndBSMUDisconnected")), //
		STATE_118(new Doc().level(Level.WARNING).text("CommunicationBetweenInverterAndMasterDisconnected")), //
		STATE_119(new Doc().level(Level.WARNING).text("CommunicationBetweenInverterAndUCDisconnected")), //
		STATE_120(new Doc().level(Level.WARNING).text("BMSStartOvertimeControlledByPCS")), //
		STATE_121(new Doc().level(Level.WARNING).text("BMSStopOvertimeControlledByPCS")), //
		STATE_122(new Doc().level(Level.WARNING).text("SyncSignalInvalidation")), //
		STATE_123(new Doc().level(Level.WARNING).text("SyncSignalContinuousCaputureFault")), //
		STATE_124(new Doc().level(Level.WARNING).text("SyncSignalSeveralTimesCaputureFault")), //
		STATE_125(new Doc().level(Level.WARNING).text("CurrentSamplingChannelAbnormityOnHighVoltageSide")), //
		STATE_126(new Doc().level(Level.WARNING).text("CurrentSamplingChannelAbnormityOnLowVoltageSide")), //
		STATE_127(new Doc().level(Level.WARNING).text("EEPROMParametersOverRange")), //
		STATE_128(new Doc().level(Level.WARNING).text("UpdateEEPROMFailed")), //
		STATE_129(new Doc().level(Level.WARNING).text("ReadEEPROMFailed")), //
		STATE_130(new Doc().level(Level.WARNING).text("CurrentSamplingChannelAbnormityBeforeInductance")), //
		STATE_131(new Doc().level(Level.WARNING).text("ReactorPowerDecreaseCausedByOvertemperature")), //
		STATE_132(new Doc().level(Level.WARNING).text("IGBTPowerDecreaseCausedByOvertemperature")), //
		STATE_133(new Doc().level(Level.WARNING).text("TemperatureChanel3PowerDecreaseCausedByOvertemperature")), //
		STATE_134(new Doc().level(Level.WARNING).text("TemperatureChanel4PowerDecreaseCausedByOvertemperature")), //
		STATE_135(new Doc().level(Level.WARNING).text("TemperatureChanel5PowerDecreaseCausedByOvertemperature")), //
		STATE_136(new Doc().level(Level.WARNING).text("TemperatureChanel6PowerDecreaseCausedByOvertemperature")), //
		STATE_137(new Doc().level(Level.WARNING).text("TemperatureChanel7PowerDecreaseCausedByOvertemperature")), //
		STATE_138(new Doc().level(Level.WARNING).text("TemperatureChanel8PowerDecreaseCausedByOvertemperature")), //
		STATE_139(new Doc().level(Level.WARNING).text("Fan1StopFailed")), //
		STATE_140(new Doc().level(Level.WARNING).text("Fan2StopFailed")), //
		STATE_141(new Doc().level(Level.WARNING).text("Fan3StopFailed")), //
		STATE_142(new Doc().level(Level.WARNING).text("Fan4StopFailed")), //
		STATE_143(new Doc().level(Level.WARNING).text("Fan1StartupFailed")), //
		STATE_144(new Doc().level(Level.WARNING).text("Fan2StartupFailed")), //
		STATE_145(new Doc().level(Level.WARNING).text("Fan3StartupFailed")), //
		STATE_146(new Doc().level(Level.WARNING).text("Fan4StartupFailed")), //
		STATE_147(new Doc().level(Level.WARNING).text("HighVoltageSideOvervoltage")), //
		STATE_148(new Doc().level(Level.WARNING).text("HighVoltageSideUndervoltage")), //
		STATE_149(new Doc().level(Level.WARNING).text("HighVoltageSideVoltageChangeUnconventionally")); //

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
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadHoldingRegisterTask(0x0101, //
						m(EssFeneconCommercial40.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x0101)),
						m(EssFeneconCommercial40.ChannelId.CONTROL_MODE, new UnsignedWordElement(0x0102)),
						new DummyRegisterElement(0x0103), // WorkMode: RemoteDispatch
						m(EssFeneconCommercial40.ChannelId.BATTERY_MAINTENANCE_STATE, new UnsignedWordElement(0x0104)),
						m(EssFeneconCommercial40.ChannelId.INVERTER_STATE, new UnsignedWordElement(0x0105)),
						m(EssFeneconCommercial40.ChannelId.GRID_MODE, new UnsignedWordElement(0x0106)),
						new DummyRegisterElement(0x0107), //
						m(EssFeneconCommercial40.ChannelId.PROTOCOL_VERSION, new UnsignedWordElement(0x0108)),
						m(EssFeneconCommercial40.ChannelId.SYSTEM_MANUFACTURER, new UnsignedWordElement(0x0109)),
						m(EssFeneconCommercial40.ChannelId.SYSTEM_TYPE, new UnsignedWordElement(0x010A)),
						new DummyRegisterElement(0x010B, 0x010F), //
						bm(new UnsignedWordElement(0x0110)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_0, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_1, 6) //
								.build(), //
						bm(new UnsignedWordElement(0x0111)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_2, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_3, 12) //
								.build(), //
						new DummyRegisterElement(0x0112, 0x0124), //
						bm(new UnsignedWordElement(0x0125)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_4, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_5, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_6, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_7, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_8, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_9, 9) //
								.build(), //
						bm(new UnsignedWordElement(0x0126)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_10, 3) //
								.build(), //
						new DummyRegisterElement(0x0127, 0x014F), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_SWITCH_STATE,
								new UnsignedWordElement(0x0150))), //
				new FC3ReadHoldingRegisterTask(0x0180, //
						bm(new UnsignedWordElement(0x0180)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_11, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_12, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_13, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_14, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_15, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_16, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_17, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_18, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_19, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_20, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_21, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_22, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_23, 12) //
								.build(), //
						new DummyRegisterElement(0x0181), //
						bm(new UnsignedWordElement(0x0182)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_24, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_25, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_26, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_27, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_28, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_29, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_30, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_31, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_32, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_33, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_34, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_35, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_36, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_37, 13) //
								.m(EssFeneconCommercial40.ChannelId.STATE_38, 14) //
								.m(EssFeneconCommercial40.ChannelId.STATE_39, 15) //
								.build(), //
						bm(new UnsignedWordElement(0x0183)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_40, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_41, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_42, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_43, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_44, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_45, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_46, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_47, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_48, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_49, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_50, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_51, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_52, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_53, 13) //
								.m(EssFeneconCommercial40.ChannelId.STATE_54, 14) //
								.m(EssFeneconCommercial40.ChannelId.STATE_55, 15) //
								.build(), //
						bm(new UnsignedWordElement(0x0184)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_56, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_57, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_58, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_59, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_60, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_61, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_62, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_63, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_64, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_65, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_66, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_67, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_68, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_69, 13) //
								.m(EssFeneconCommercial40.ChannelId.STATE_70, 14) //
								.m(EssFeneconCommercial40.ChannelId.STATE_71, 15) //
								.build(), //
						bm(new UnsignedWordElement(0x0185)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_72, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_73, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_74, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_75, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_76, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_77, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_78, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_79, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_80, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_81, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_82, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_83, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_84, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_85, 13) //
								.build(), //
						bm(new UnsignedWordElement(0x0186)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_86, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_87, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_88, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_89, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_90, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_91, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_92, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_93, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_94, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_95, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_96, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_97, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_98, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_99, 13) //
								.m(EssFeneconCommercial40.ChannelId.STATE_100, 14) //
								.m(EssFeneconCommercial40.ChannelId.STATE_101, 15) //
								.build(), //
						bm(new UnsignedWordElement(0x0187)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_102, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_103, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_104, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_105, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_106, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_107, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_108, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_109, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_110, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_111, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_112, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_113, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_114, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_115, 13) //
								.m(EssFeneconCommercial40.ChannelId.STATE_116, 14) //
								.build(), //
						bm(new UnsignedWordElement(0x0188)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_117, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_118, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_119, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_120, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_121, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_122, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_123, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_124, 14) //
								.build() //
				), new FC3ReadHoldingRegisterTask(0x0200, //
						m(EssFeneconCommercial40.ChannelId.BATTERY_VOLTAGE,
								new SignedWordElement(0x0200).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_CURRENT,
								new SignedWordElement(0x0201).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_POWER, new SignedWordElement(0x0202).scaleFactor(2)), //
						new DummyRegisterElement(0x0203, 0x0207),
						m(EssFeneconCommercial40.ChannelId.AC_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0x0208).scaleFactor(2).wordOrder(WordOrder.LSWMSW)), //
						m(EssFeneconCommercial40.ChannelId.AC_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0x020A).scaleFactor(2).wordOrder(WordOrder.LSWMSW)), //
						new DummyRegisterElement(0x020C, 0x020F), //
						m(EssFeneconCommercial40.ChannelId.GRID_ACTIVE_POWER,
								new SignedWordElement(0x0210).scaleFactor(2)), //
						cm(new SignedWordElement(0x0211).scaleFactor(2)) //
								.m(EssSymmetricReadonly.ChannelId.CHARGE_REACTIVE_POWER,
										ElementToChannelConverter.CONVERT_NEGATIVE_AND_INVERT) //
								.m(EssSymmetricReadonly.ChannelId.DISCHARGE_REACTIVE_POWER,
										ElementToChannelConverter.CONVERT_POSITIVE) //
								.build(), //
						m(EssFeneconCommercial40.ChannelId.APPARENT_POWER,
								new UnsignedWordElement(0x0212).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.CURRENT_L1, new SignedWordElement(0x0213).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.CURRENT_L2, new SignedWordElement(0x0214).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.CURRENT_L3, new SignedWordElement(0x0215).scaleFactor(2)), //
						new DummyRegisterElement(0x0216, 0x218), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x0219).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x021A).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x021B).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.FREQUENCY, new UnsignedWordElement(0x021C).scaleFactor(1))), //
				new FC3ReadHoldingRegisterTask(0x0222, //
						m(EssFeneconCommercial40.ChannelId.INVERTER_VOLTAGE_L1,
								new UnsignedWordElement(0x0222).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_VOLTAGE_L2,
								new UnsignedWordElement(0x0223).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_VOLTAGE_L3,
								new UnsignedWordElement(0x0224).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_CURRENT_L1,
								new SignedWordElement(0x0225).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_CURRENT_L2,
								new SignedWordElement(0x0226).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_CURRENT_L3,
								new SignedWordElement(0x0227).scaleFactor(2)), //
						cm(new SignedWordElement(0x0228).scaleFactor(2)) //
								.m(EssSymmetricReadonly.ChannelId.CHARGE_ACTIVE_POWER,
										ElementToChannelConverter.CONVERT_NEGATIVE_AND_INVERT) //
								.m(EssSymmetricReadonly.ChannelId.DISCHARGE_ACTIVE_POWER,
										ElementToChannelConverter.CONVERT_POSITIVE) //
								.build(), //
						new DummyRegisterElement(0x0229, 0x022F), //
						m(EssFeneconCommercial40.ChannelId.ALLOWED_CHARGE,
								new SignedWordElement(0x0230).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.ALLOWED_DISCHARGE,
								new UnsignedWordElement(0x0231).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.ALLOWED_APPARENT,
								new UnsignedWordElement(0x0232).scaleFactor(2)), //
						new DummyRegisterElement(0x0233, 0x23F),
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L1, new SignedWordElement(0x0240)), //
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L2, new SignedWordElement(0x0241)), //
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L3, new SignedWordElement(0x0242)), //
						new DummyRegisterElement(0x0243, 0x0248), //
						m(EssFeneconCommercial40.ChannelId.TRANSFORMER_TEMPERATURE_L2, new SignedWordElement(0x0249))), //
				new _TODO_WriteRegisterTask(0x0500, //
						m(EssFeneconCommercial40.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x0500)), //
						cm(new SignedWordElement(0x0501).scaleFactor(2)) //
								.m(EssFeneconCommercial40.ChannelId.SET_CHARGE_ACTIVE_POWER,
										ElementToChannelConverter.CONVERT_NEGATIVE_AND_INVERT) //
								.m(EssFeneconCommercial40.ChannelId.SET_DISCHARGE_ACTIVE_POWER,
										ElementToChannelConverter.CONVERT_POSITIVE) //
								.build(), //
						cm(new SignedWordElement(0x0502).scaleFactor(2)) //
								.m(EssFeneconCommercial40.ChannelId.SET_CHARGE_REACTIVE_POWER,
										ElementToChannelConverter.CONVERT_NEGATIVE_AND_INVERT) //
								.m(EssFeneconCommercial40.ChannelId.SET_DISCHARGE_REACTIVE_POWER,
										ElementToChannelConverter.CONVERT_POSITIVE) //
								.build(), //
						m(EssFeneconCommercial40.ChannelId.SET_PV_POWER_LIMIT,
								new UnsignedWordElement(0x0503).scaleFactor(2))), //
				new FC3ReadHoldingRegisterTask(0xA000, //
						m(EssFeneconCommercial40.ChannelId.BMS_DCDC_WORK_STATE, new UnsignedWordElement(0xA000)), //
						m(EssFeneconCommercial40.ChannelId.BMS_DCDC_WORK_MODE, new UnsignedWordElement(0xA001))), //
				new FC3ReadHoldingRegisterTask(0xA100, //
						bm(new UnsignedWordElement(0xA100)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_125, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_126, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_127, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_128, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_129, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_130, 9) //
								.build(), //
						bm(new UnsignedWordElement(0xA101)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_131, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_132, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_133, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_134, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_135, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_136, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_137, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_138, 7) //
								.m(EssFeneconCommercial40.ChannelId.STATE_139, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_140, 9) //
								.m(EssFeneconCommercial40.ChannelId.STATE_141, 10) //
								.m(EssFeneconCommercial40.ChannelId.STATE_142, 11) //
								.m(EssFeneconCommercial40.ChannelId.STATE_143, 12) //
								.m(EssFeneconCommercial40.ChannelId.STATE_144, 13) //
								.m(EssFeneconCommercial40.ChannelId.STATE_145, 14) //
								.m(EssFeneconCommercial40.ChannelId.STATE_146, 15) //
								.build(), //
						bm(new UnsignedWordElement(0xA102)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_147, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_148, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_149, 2) //
								.build()), //
				new FC3ReadHoldingRegisterTask(0x1402, //
						m(Ess.ChannelId.SOC, new UnsignedWordElement(0x1402).priority(Priority.HIGH))));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().format();
	}
}
