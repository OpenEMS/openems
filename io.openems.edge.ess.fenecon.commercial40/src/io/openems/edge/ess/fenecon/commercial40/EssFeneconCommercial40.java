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
import io.openems.edge.bridge.modbus.channel.ModbusIntegerReadChannel;
import io.openems.edge.bridge.modbus.channel.ModbusLongReadChannel;
import io.openems.edge.bridge.modbus.protocol.DummyElement;
import io.openems.edge.bridge.modbus.protocol.ModbusProtocol;
import io.openems.edge.bridge.modbus.protocol.RegisterRange;
import io.openems.edge.bridge.modbus.protocol.SignedWordElement;
import io.openems.edge.bridge.modbus.protocol.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.protocol.UnsignedWordElement;
import io.openems.edge.bridge.modbus.protocol.WordOrder;
import io.openems.edge.bridge.modbus.protocol.WriteRegisterRange;
import io.openems.edge.common.channel.BooleanReadChannel;
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
						return new ModbusIntegerReadChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(EssSymmetricReadonly.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CHARGE_ACTIVE_POWER:
					case DISCHARGE_ACTIVE_POWER:
					case CHARGE_REACTIVE_POWER:
					case DISCHARGE_REACTIVE_POWER:
						return new ModbusIntegerReadChannel(this, channelId);
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
					case SET_ACTIVE_POWER:
					case SET_WORK_STATE:
					case TRANSFORMER_TEMPERATURE_L2:
						return new ModbusIntegerReadChannel(this, channelId);
					case AC_CHARGE_ENERGY:
					case AC_DISCHARGE_ENERGY:
						return new ModbusLongReadChannel(this, channelId);
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
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
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
		STATE_124(new Doc().level(Level.WARNING).text("SyncSignalSeveralTimesCaputureFault")); //

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
		return new ModbusProtocol( //
				new RegisterRange(0x0101, //
						m(EssFeneconCommercial40.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x0101)),
						m(EssFeneconCommercial40.ChannelId.CONTROL_MODE, new UnsignedWordElement(0x0102)),
						new DummyElement(0x0103), // WorkMode: RemoteDispatch
						m(EssFeneconCommercial40.ChannelId.BATTERY_MAINTENANCE_STATE, new UnsignedWordElement(0x0104)),
						m(EssFeneconCommercial40.ChannelId.INVERTER_STATE, new UnsignedWordElement(0x0105)),
						m(EssFeneconCommercial40.ChannelId.GRID_MODE, new UnsignedWordElement(0x0106)),
						new DummyElement(0x0107), //
						m(EssFeneconCommercial40.ChannelId.PROTOCOL_VERSION, new UnsignedWordElement(0x0108)),
						m(EssFeneconCommercial40.ChannelId.SYSTEM_MANUFACTURER, new UnsignedWordElement(0x0109)),
						m(EssFeneconCommercial40.ChannelId.SYSTEM_TYPE, new UnsignedWordElement(0x010A)),
						new DummyElement(0x010B, 0x010F), //
						bm(new UnsignedWordElement(0x0110)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_0, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_1, 6).build(), //
						bm(new UnsignedWordElement(0x0111)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_2, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_3, 12).build(), //
						new DummyElement(0x0112, 0x0124), //
						bm(new UnsignedWordElement(0x0125)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_4, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_5, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_6, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_7, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_8, 8) //
								.m(EssFeneconCommercial40.ChannelId.STATE_9, 9).build(), //
						bm(new UnsignedWordElement(0x0126)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_10, 3).build(), //
						new DummyElement(0x0127, 0x014F), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_SWITCH_STATE,
								new UnsignedWordElement(0x0150))), //
				new RegisterRange(0x0180, //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_23, 12).build(), //
						new DummyElement(0x0181), //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_39, 15).build(), //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_55, 15).build(), //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_71, 15).build(), //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_85, 13).build(), //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_101, 15).build(), //
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
								.m(EssFeneconCommercial40.ChannelId.STATE_116, 14).build(), //
						bm(new UnsignedWordElement(0x0188)) //
								.m(EssFeneconCommercial40.ChannelId.STATE_117, 0) //
								.m(EssFeneconCommercial40.ChannelId.STATE_118, 1) //
								.m(EssFeneconCommercial40.ChannelId.STATE_119, 2) //
								.m(EssFeneconCommercial40.ChannelId.STATE_120, 3) //
								.m(EssFeneconCommercial40.ChannelId.STATE_121, 4) //
								.m(EssFeneconCommercial40.ChannelId.STATE_122, 5) //
								.m(EssFeneconCommercial40.ChannelId.STATE_123, 6) //
								.m(EssFeneconCommercial40.ChannelId.STATE_124, 14).build() //
				), new RegisterRange(0x0200, //
						m(EssFeneconCommercial40.ChannelId.BATTERY_VOLTAGE,
								new SignedWordElement(0x0200).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_CURRENT,
								new SignedWordElement(0x0201).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_POWER, new SignedWordElement(0x0202).scaleFactor(2)), //
						new DummyElement(0x0203, 0x0207),
						m(EssFeneconCommercial40.ChannelId.AC_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0x0208).scaleFactor(2).wordOrder(WordOrder.LSWMSW)), //
						m(EssFeneconCommercial40.ChannelId.AC_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0x020A).scaleFactor(2).wordOrder(WordOrder.LSWMSW)), //
						new DummyElement(0x020C, 0x020F), //
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
						new DummyElement(0x0216, 0x218), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x0219).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x021A).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x021B).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.FREQUENCY, new UnsignedWordElement(0x021C).scaleFactor(1))), //
				new RegisterRange(0x0222, //
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
						new DummyElement(0x0229, 0x022F), //
						m(EssFeneconCommercial40.ChannelId.ALLOWED_CHARGE,
								new SignedWordElement(0x0230).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.ALLOWED_DISCHARGE,
								new UnsignedWordElement(0x0231).scaleFactor(2)), //
						m(EssFeneconCommercial40.ChannelId.ALLOWED_APPARENT,
								new UnsignedWordElement(0x0232).scaleFactor(2)), //
						new DummyElement(0x0233, 0x23F),
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L1, new SignedWordElement(0x0240)), //
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L2, new SignedWordElement(0x0241)), //
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L3, new SignedWordElement(0x0242)), //
						new DummyElement(0x0243, 0x0248), //
						m(EssFeneconCommercial40.ChannelId.TRANSFORMER_TEMPERATURE_L2, new SignedWordElement(0x0249))), //
				new WriteRegisterRange(0x0500, //
						m(EssFeneconCommercial40.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x0500)), //
						m(EssFeneconCommercial40.ChannelId.SET_ACTIVE_POWER,
								new SignedWordElement(0x0501).scaleFactor(2)) //
				//
				), new RegisterRange(0x1402, //
						m(Ess.ChannelId.SOC, new UnsignedWordElement(0x1402))));

		// new WriteableModbusRegisterRange(0x0501, //
		// new SignedWordElement(0x0501, //
		// setActivePower = new ModbusWriteLongChannel("SetActivePower", this).unit("W")
		// .multiplier(2).minWriteChannel(allowedCharge)
		// .maxWriteChannel(allowedDischarge)),
		// new SignedWordElement(0x0502, //
		// setReactivePower = new ModbusWriteLongChannel("SetReactivePower",
		// this).unit("var")
		// .multiplier(2).minWriteChannel(allowedCharge)
		// .maxWriteChannel(allowedDischarge))),
		// new ModbusRegisterRange(0x1402, //
		// new UnsignedWordElement(0x1402,
		// soc = new ModbusReadLongChannel("Soc", this).unit("%").interval(0, 100)),
		// new UnsignedWordElement(0x1403,
		// soh = new ModbusReadLongChannel("Soh", this).unit("%").interval(0, 100)),
		// new UnsignedWordElement(0x1404,
		// batteryCellAverageTemperature = new ModbusReadLongChannel(
		// "BatteryCellAverageTemperature", this).unit("°C"))),
		// new ModbusRegisterRange(0x1500, //
		// new UnsignedWordElement(0x1500,
		// batteryCell1Voltage = new ModbusReadLongChannel("Cell1Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1501,
		// batteryCell2Voltage = new ModbusReadLongChannel("Cell2Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1502,
		// batteryCell3Voltage = new ModbusReadLongChannel("Cell3Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1503,
		// batteryCell4Voltage = new ModbusReadLongChannel("Cell4Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1504,
		// batteryCell5Voltage = new ModbusReadLongChannel("Cell5Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1505,
		// batteryCell6Voltage = new ModbusReadLongChannel("Cell6Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1506,
		// batteryCell7Voltage = new ModbusReadLongChannel("Cell7Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1507,
		// batteryCell8Voltage = new ModbusReadLongChannel("Cell8Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1508,
		// batteryCell9Voltage = new ModbusReadLongChannel("Cell9Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1509,
		// batteryCell10Voltage = new ModbusReadLongChannel("Cell10Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x150A,
		// batteryCell11Voltage = new ModbusReadLongChannel("Cell11Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x150B,
		// batteryCell12Voltage = new ModbusReadLongChannel("Cell12Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x150C,
		// batteryCell13Voltage = new ModbusReadLongChannel("Cell13Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x150D,
		// batteryCell14Voltage = new ModbusReadLongChannel("Cell14Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x150E,
		// batteryCell15Voltage = new ModbusReadLongChannel("Cell15Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x150F,
		// batteryCell16Voltage = new ModbusReadLongChannel("Cell16Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1510,
		// batteryCell17Voltage = new ModbusReadLongChannel("Cell17Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1511,
		// batteryCell18Voltage = new ModbusReadLongChannel("Cell18Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1512,
		// batteryCell19Voltage = new ModbusReadLongChannel("Cell19Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1513,
		// batteryCell20Voltage = new ModbusReadLongChannel("Cell20Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1514,
		// batteryCell21Voltage = new ModbusReadLongChannel("Cell21Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1515,
		// batteryCell22Voltage = new ModbusReadLongChannel("Cell22Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1516,
		// batteryCell23Voltage = new ModbusReadLongChannel("Cell23Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1517,
		// batteryCell24Voltage = new ModbusReadLongChannel("Cell24Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1518,
		// batteryCell25Voltage = new ModbusReadLongChannel("Cell25Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1519,
		// batteryCell26Voltage = new ModbusReadLongChannel("Cell26Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x151A,
		// batteryCell27Voltage = new ModbusReadLongChannel("Cell27Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x151B,
		// batteryCell28Voltage = new ModbusReadLongChannel("Cell28Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x151C,
		// batteryCell29Voltage = new ModbusReadLongChannel("Cell29Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x151D,
		// batteryCell30Voltage = new ModbusReadLongChannel("Cell30Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x151E,
		// batteryCell31Voltage = new ModbusReadLongChannel("Cell31Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x151F,
		// batteryCell32Voltage = new ModbusReadLongChannel("Cell32Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1520,
		// batteryCell33Voltage = new ModbusReadLongChannel("Cell33Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1521,
		// batteryCell34Voltage = new ModbusReadLongChannel("Cell34Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1522,
		// batteryCell35Voltage = new ModbusReadLongChannel("Cell35Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1523,
		// batteryCell36Voltage = new ModbusReadLongChannel("Cell36Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1524,
		// batteryCell37Voltage = new ModbusReadLongChannel("Cell37Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1525,
		// batteryCell38Voltage = new ModbusReadLongChannel("Cell38Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1526,
		// batteryCell39Voltage = new ModbusReadLongChannel("Cell39Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1527,
		// batteryCell40Voltage = new ModbusReadLongChannel("Cell40Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1528,
		// batteryCell41Voltage = new ModbusReadLongChannel("Cell41Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1529,
		// batteryCell42Voltage = new ModbusReadLongChannel("Cell42Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x152A,
		// batteryCell43Voltage = new ModbusReadLongChannel("Cell43Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x152B,
		// batteryCell44Voltage = new ModbusReadLongChannel("Cell44Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x152C,
		// batteryCell45Voltage = new ModbusReadLongChannel("Cell45Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x152D,
		// batteryCell46Voltage = new ModbusReadLongChannel("Cell46Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x152E,
		// batteryCell47Voltage = new ModbusReadLongChannel("Cell47Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x152F,
		// batteryCell48Voltage = new ModbusReadLongChannel("Cell48Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1530,
		// batteryCell49Voltage = new ModbusReadLongChannel("Cell49Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1531,
		// batteryCell50Voltage = new ModbusReadLongChannel("Cell50Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1532,
		// batteryCell51Voltage = new ModbusReadLongChannel("Cell51Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1533,
		// batteryCell52Voltage = new ModbusReadLongChannel("Cell52Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1534,
		// batteryCell53Voltage = new ModbusReadLongChannel("Cell53Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1535,
		// batteryCell54Voltage = new ModbusReadLongChannel("Cell54Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1536,
		// batteryCell55Voltage = new ModbusReadLongChannel("Cell55Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1537,
		// batteryCell56Voltage = new ModbusReadLongChannel("Cell56Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1538,
		// batteryCell57Voltage = new ModbusReadLongChannel("Cell57Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1539,
		// batteryCell58Voltage = new ModbusReadLongChannel("Cell58Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x153A,
		// batteryCell59Voltage = new ModbusReadLongChannel("Cell59Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x153B,
		// batteryCell60Voltage = new ModbusReadLongChannel("Cell60Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x153C,
		// batteryCell61Voltage = new ModbusReadLongChannel("Cell61Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x153D,
		// batteryCell62Voltage = new ModbusReadLongChannel("Cell62Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x153E,
		// batteryCell63Voltage = new ModbusReadLongChannel("Cell63Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x153F,
		// batteryCell64Voltage = new ModbusReadLongChannel("Cell64Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1540,
		// batteryCell65Voltage = new ModbusReadLongChannel("Cell65Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1541,
		// batteryCell66Voltage = new ModbusReadLongChannel("Cell66Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1542,
		// batteryCell67Voltage = new ModbusReadLongChannel("Cell67Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1543,
		// batteryCell68Voltage = new ModbusReadLongChannel("Cell68Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1544,
		// batteryCell69Voltage = new ModbusReadLongChannel("Cell69Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1545,
		// batteryCell70Voltage = new ModbusReadLongChannel("Cell70Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1546,
		// batteryCell71Voltage = new ModbusReadLongChannel("Cell71Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1547,
		// batteryCell72Voltage = new ModbusReadLongChannel("Cell72Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1548,
		// batteryCell73Voltage = new ModbusReadLongChannel("Cell73Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1549,
		// batteryCell74Voltage = new ModbusReadLongChannel("Cell74Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x154A,
		// batteryCell75Voltage = new ModbusReadLongChannel("Cell75Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x154B,
		// batteryCell76Voltage = new ModbusReadLongChannel("Cell76Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x154C,
		// batteryCell77Voltage = new ModbusReadLongChannel("Cell77Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x154D,
		// batteryCell78Voltage = new ModbusReadLongChannel("Cell78Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x154E,
		// batteryCell79Voltage = new ModbusReadLongChannel("Cell79Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x154F,
		// batteryCell80Voltage = new ModbusReadLongChannel("Cell80Voltage",
		// this).unit("mV")
		// )),
		// new ModbusRegisterRange(0x1550, //
		// new UnsignedWordElement(0x1550,
		// batteryCell81Voltage = new ModbusReadLongChannel("Cell81Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1551,
		// batteryCell82Voltage = new ModbusReadLongChannel("Cell82Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1552,
		// batteryCell83Voltage = new ModbusReadLongChannel("Cell83Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1553,
		// batteryCell84Voltage = new ModbusReadLongChannel("Cell84Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1554,
		// batteryCell85Voltage = new ModbusReadLongChannel("Cell85Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1555,
		// batteryCell86Voltage = new ModbusReadLongChannel("Cell86Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1556,
		// batteryCell87Voltage = new ModbusReadLongChannel("Cell87Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1557,
		// batteryCell88Voltage = new ModbusReadLongChannel("Cell88Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1558,
		// batteryCell89Voltage = new ModbusReadLongChannel("Cell89Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1559,
		// batteryCell90Voltage = new ModbusReadLongChannel("Cell90Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x155A,
		// batteryCell91Voltage = new ModbusReadLongChannel("Cell91Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x155B,
		// batteryCell92Voltage = new ModbusReadLongChannel("Cell92Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x155C,
		// batteryCell93Voltage = new ModbusReadLongChannel("Cell93Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x155D,
		// batteryCell94Voltage = new ModbusReadLongChannel("Cell94Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x155E,
		// batteryCell95Voltage = new ModbusReadLongChannel("Cell95Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x155F,
		// batteryCell96Voltage = new ModbusReadLongChannel("Cell96Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1560,
		// batteryCell97Voltage = new ModbusReadLongChannel("Cell97Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1561,
		// batteryCell98Voltage = new ModbusReadLongChannel("Cell98Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1562,
		// batteryCell99Voltage = new ModbusReadLongChannel("Cell99Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1563,
		// batteryCell100Voltage = new ModbusReadLongChannel("Cell100Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1564,
		// batteryCell101Voltage = new ModbusReadLongChannel("Cell101Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1565,
		// batteryCell102Voltage = new ModbusReadLongChannel("Cell102Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1566,
		// batteryCell103Voltage = new ModbusReadLongChannel("Cell103Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1567,
		// batteryCell104Voltage = new ModbusReadLongChannel("Cell104Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1568,
		// batteryCell105Voltage = new ModbusReadLongChannel("Cell105Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1569,
		// batteryCell106Voltage = new ModbusReadLongChannel("Cell106Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x156A,
		// batteryCell107Voltage = new ModbusReadLongChannel("Cell107Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x156B,
		// batteryCell108Voltage = new ModbusReadLongChannel("Cell108Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x156C,
		// batteryCell109Voltage = new ModbusReadLongChannel("Cell109Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x156D,
		// batteryCell110Voltage = new ModbusReadLongChannel("Cell110Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x156E,
		// batteryCell111Voltage = new ModbusReadLongChannel("Cell111Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x156F,
		// batteryCell112Voltage = new ModbusReadLongChannel("Cell112Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1570,
		// batteryCell113Voltage = new ModbusReadLongChannel("Cell113Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1571,
		// batteryCell114Voltage = new ModbusReadLongChannel("Cell114Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1572,
		// batteryCell115Voltage = new ModbusReadLongChannel("Cell115Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1573,
		// batteryCell116Voltage = new ModbusReadLongChannel("Cell116Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1574,
		// batteryCell117Voltage = new ModbusReadLongChannel("Cell117Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1575,
		// batteryCell118Voltage = new ModbusReadLongChannel("Cell18Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1576,
		// batteryCell119Voltage = new ModbusReadLongChannel("Cell119Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1577,
		// batteryCell120Voltage = new ModbusReadLongChannel("Cell120Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1578,
		// batteryCell121Voltage = new ModbusReadLongChannel("Cell121Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1579,
		// batteryCell122Voltage = new ModbusReadLongChannel("Cell122Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x157A,
		// batteryCell123Voltage = new ModbusReadLongChannel("Cell123Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x157B,
		// batteryCell124Voltage = new ModbusReadLongChannel("Cell124Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x157C,
		// batteryCell125Voltage = new ModbusReadLongChannel("Cell125Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x157D,
		// batteryCell126Voltage = new ModbusReadLongChannel("Cell126Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x157E,
		// batteryCell127Voltage = new ModbusReadLongChannel("Cell127Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x157F,
		// batteryCell128Voltage = new ModbusReadLongChannel("Cell128Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1580,
		// batteryCell129Voltage = new ModbusReadLongChannel("Cell129Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1581,
		// batteryCell130Voltage = new ModbusReadLongChannel("Cell130Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1582,
		// batteryCell131Voltage = new ModbusReadLongChannel("Cell131Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1583,
		// batteryCell132Voltage = new ModbusReadLongChannel("Cell132Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1584,
		// batteryCell133Voltage = new ModbusReadLongChannel("Cell133Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1585,
		// batteryCell134Voltage = new ModbusReadLongChannel("Cell134Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1586,
		// batteryCell135Voltage = new ModbusReadLongChannel("Cell135Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1587,
		// batteryCell136Voltage = new ModbusReadLongChannel("Cell136Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1588,
		// batteryCell137Voltage = new ModbusReadLongChannel("Cell137Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1589,
		// batteryCell138Voltage = new ModbusReadLongChannel("Cell138Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x158A,
		// batteryCell139Voltage = new ModbusReadLongChannel("Cell139Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x158B,
		// batteryCell140Voltage = new ModbusReadLongChannel("Cell140Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x158C,
		// batteryCell141Voltage = new ModbusReadLongChannel("Cell141Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x158D,
		// batteryCell142Voltage = new ModbusReadLongChannel("Cell142Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x158E,
		// batteryCell143Voltage = new ModbusReadLongChannel("Cell143Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x158F,
		// batteryCell144Voltage = new ModbusReadLongChannel("Cell144Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1590,
		// batteryCell145Voltage = new ModbusReadLongChannel("Cell145Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1591,
		// batteryCell146Voltage = new ModbusReadLongChannel("Cell146Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1592,
		// batteryCell147Voltage = new ModbusReadLongChannel("Cell147Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1593,
		// batteryCell148Voltage = new ModbusReadLongChannel("Cell148Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1594,
		// batteryCell149Voltage = new ModbusReadLongChannel("Cell149Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1595,
		// batteryCell150Voltage = new ModbusReadLongChannel("Cell150Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1596,
		// batteryCell151Voltage = new ModbusReadLongChannel("Cell151Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1597,
		// batteryCell152Voltage = new ModbusReadLongChannel("Cell152Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1598,
		// batteryCell153Voltage = new ModbusReadLongChannel("Cell153Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x1599,
		// batteryCell154Voltage = new ModbusReadLongChannel("Cell154Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x159A,
		// batteryCell155Voltage = new ModbusReadLongChannel("Cell155Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x159B,
		// batteryCell156Voltage = new ModbusReadLongChannel("Cell156Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x159C,
		// batteryCell157Voltage = new ModbusReadLongChannel("Cell157Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x159D,
		// batteryCell158Voltage = new ModbusReadLongChannel("Cell158Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x159E,
		// batteryCell159Voltage = new ModbusReadLongChannel("Cell159Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x159F,
		// batteryCell160Voltage = new ModbusReadLongChannel("Cell160Voltage",
		// this).unit("mV")
		// )),//
		// new ModbusRegisterRange(0x15A0, //
		// new UnsignedWordElement(0x15A0,
		// batteryCell161Voltage = new ModbusReadLongChannel("Cell161Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A1,
		// batteryCell162Voltage = new ModbusReadLongChannel("Cell162Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A2,
		// batteryCell163Voltage = new ModbusReadLongChannel("Cell163Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A3,
		// batteryCell164Voltage = new ModbusReadLongChannel("Cell164Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A4,
		// batteryCell165Voltage = new ModbusReadLongChannel("Cell165Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A5,
		// batteryCell166Voltage = new ModbusReadLongChannel("Cell166Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A6,
		// batteryCell167Voltage = new ModbusReadLongChannel("Cell167Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A7,
		// batteryCell168Voltage = new ModbusReadLongChannel("Cell168Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A8,
		// batteryCell169Voltage = new ModbusReadLongChannel("Cell169Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15A9,
		// batteryCell170Voltage = new ModbusReadLongChannel("Cell170Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15AA,
		// batteryCell171Voltage = new ModbusReadLongChannel("Cell171Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15AB,
		// batteryCell172Voltage = new ModbusReadLongChannel("Cell172Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15AC,
		// batteryCell173Voltage = new ModbusReadLongChannel("Cell173Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15AD,
		// batteryCell174Voltage = new ModbusReadLongChannel("Cell174Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15AE,
		// batteryCell175Voltage = new ModbusReadLongChannel("Cell175Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15AF,
		// batteryCell176Voltage = new ModbusReadLongChannel("Cell176Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B0,
		// batteryCell177Voltage = new ModbusReadLongChannel("Cell177Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B1,
		// batteryCell178Voltage = new ModbusReadLongChannel("Cell178Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B2,
		// batteryCell179Voltage = new ModbusReadLongChannel("Cell179Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B3,
		// batteryCell180Voltage = new ModbusReadLongChannel("Cell180Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B4,
		// batteryCell181Voltage = new ModbusReadLongChannel("Cell181Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B5,
		// batteryCell182Voltage = new ModbusReadLongChannel("Cell182Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B6,
		// batteryCell183Voltage = new ModbusReadLongChannel("Cell183Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B7,
		// batteryCell184Voltage = new ModbusReadLongChannel("Cell184Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B8,
		// batteryCell185Voltage = new ModbusReadLongChannel("Cell185Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15B9,
		// batteryCell186Voltage = new ModbusReadLongChannel("Cell186Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15BA,
		// batteryCell187Voltage = new ModbusReadLongChannel("Cell187Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15BB,
		// batteryCell188Voltage = new ModbusReadLongChannel("Cell188Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15BC,
		// batteryCell189Voltage = new ModbusReadLongChannel("Cell189Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15BD,
		// batteryCell190Voltage = new ModbusReadLongChannel("Cell190Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15BE,
		// batteryCell191Voltage = new ModbusReadLongChannel("Cell191Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15BF,
		// batteryCell192Voltage = new ModbusReadLongChannel("Cell192Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C0,
		// batteryCell193Voltage = new ModbusReadLongChannel("Cell193Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C1,
		// batteryCell194Voltage = new ModbusReadLongChannel("Cell194Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C2,
		// batteryCell195Voltage = new ModbusReadLongChannel("Cell195Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C3,
		// batteryCell196Voltage = new ModbusReadLongChannel("Cell196Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C4,
		// batteryCell197Voltage = new ModbusReadLongChannel("Cell197Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C5,
		// batteryCell198Voltage = new ModbusReadLongChannel("Cell198Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C6,
		// batteryCell199Voltage = new ModbusReadLongChannel("Cell199Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C7,
		// batteryCell200Voltage = new ModbusReadLongChannel("Cell200Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C8,
		// batteryCell201Voltage = new ModbusReadLongChannel("Cell201Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15C9,
		// batteryCell202Voltage = new ModbusReadLongChannel("Cell202Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15CA,
		// batteryCell203Voltage = new ModbusReadLongChannel("Cell203Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15CB,
		// batteryCell204Voltage = new ModbusReadLongChannel("Cell204Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15CC,
		// batteryCell205Voltage = new ModbusReadLongChannel("Cell205Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15CD,
		// batteryCell206Voltage = new ModbusReadLongChannel("Cell206Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15CE,
		// batteryCell207Voltage = new ModbusReadLongChannel("Cell207Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15CF,
		// batteryCell208Voltage = new ModbusReadLongChannel("Cell208Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D0,
		// batteryCell209Voltage = new ModbusReadLongChannel("Cell209Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D1,
		// batteryCell210Voltage = new ModbusReadLongChannel("Cell210Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D2,
		// batteryCell211Voltage = new ModbusReadLongChannel("Cell211Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D3,
		// batteryCell212Voltage = new ModbusReadLongChannel("Cell212Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D4,
		// batteryCell213Voltage = new ModbusReadLongChannel("Cell213Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D5,
		// batteryCell214Voltage = new ModbusReadLongChannel("Cell214Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D6,
		// batteryCell215Voltage = new ModbusReadLongChannel("Cell215Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D7,
		// batteryCell216Voltage = new ModbusReadLongChannel("Cell216Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D8,
		// batteryCell217Voltage = new ModbusReadLongChannel("Cell217Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15D9,
		// batteryCell218Voltage = new ModbusReadLongChannel("Cell218Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15DA,
		// batteryCell219Voltage = new ModbusReadLongChannel("Cell219Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15DB,
		// batteryCell220Voltage = new ModbusReadLongChannel("Cell220Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15DC,
		// batteryCell221Voltage = new ModbusReadLongChannel("Cell221Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15DD,
		// batteryCell222Voltage = new ModbusReadLongChannel("Cell222Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15DE,
		// batteryCell223Voltage = new ModbusReadLongChannel("Cell223Voltage",
		// this).unit("mV")
		// ),//
		// new UnsignedWordElement(0x15DF,
		// batteryCell224Voltage = new ModbusReadLongChannel("Cell224Voltage",
		// this).unit("mV")
		// )));
	}

}
