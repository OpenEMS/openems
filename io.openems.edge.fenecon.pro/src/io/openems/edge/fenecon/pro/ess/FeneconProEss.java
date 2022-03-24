package io.openems.edge.fenecon.pro.ess;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Fenecon.Pro.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class FeneconProEss extends AbstractOpenemsModbusComponent implements SymmetricEss, AsymmetricEss,
		ManagedAsymmetricEss, ManagedSymmetricEss, ModbusComponent, OpenemsComponent, ModbusSlave, EventHandler {

	protected static final int MAX_APPARENT_POWER = 9000;

	private static final int UNIT_ID = 4;

	private final Logger log = LoggerFactory.getLogger(FeneconProEss.class);
	private final MaxApparentPowerHandler maxApparentPowerHandler = new MaxApparentPowerHandler(this);

	private String modbusBridgeId;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public FeneconProEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ProChannelId.values() //
		);
		this._setMaxApparentPower(FeneconProEss.MAX_APPARENT_POWER);
		this._setCapacity(12_000);
		AsymmetricEss.initializePowerSumChannels(this);
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		this.getSetActivePowerL1Channel().setNextWriteValue(activePowerL1);
		this.getSetActivePowerL2Channel().setNextWriteValue(activePowerL2);
		this.getSetActivePowerL3Channel().setNextWriteValue(activePowerL3);
		this.getSetReactivePowerL1Channel().setNextWriteValue(reactivePowerL1);
		this.getSetReactivePowerL2Channel().setNextWriteValue(reactivePowerL2);
		this.getSetReactivePowerL3Channel().setNextWriteValue(reactivePowerL3);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.modbusBridgeId = config.modbus_id();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return this.modbusBridgeId;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.HIGH, //
						m(ProChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(ProChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						m(ProChannelId.WORK_MODE, new UnsignedWordElement(102)), //
						new DummyRegisterElement(103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(ProChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(109), new ElementToChannelConverter(
								// element -> channel
								value -> {
									// Set SoC to 100 % if battery is full and AllowedCharge is zero
									if (value == null) {
										return null;
									}
									int soc = (Integer) value;
									IntegerReadChannel allowedCharge = this
											.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
									IntegerReadChannel allowedDischarge = this
											.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
									if (soc > 95 && allowedCharge.value().orElse(-1) == 0
											&& allowedDischarge.value().orElse(0) != 0) {
										return 100;
									}
									return value;
								}, // channel -> element
								value -> value)), //
						m(ProChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.BATTERY_CURRENT, new SignedWordElement(111),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						m(new BitsWordElement(113, this) //
								.bit(0, ProChannelId.STATE_0) //
								.bit(1, ProChannelId.STATE_1) //
								.bit(2, ProChannelId.STATE_2) //
								.bit(3, ProChannelId.STATE_3) //
								.bit(4, ProChannelId.STATE_4) //
								.bit(5, ProChannelId.STATE_5) //
								.bit(6, ProChannelId.STATE_6)), //
						m(ProChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(ProChannelId.CURRENT_L1, new SignedWordElement(118),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.CURRENT_L2, new SignedWordElement(119),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.CURRENT_L3, new SignedWordElement(120),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.VOLTAGE_L1, new UnsignedWordElement(121),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.VOLTAGE_L2, new UnsignedWordElement(122),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.VOLTAGE_L3, new UnsignedWordElement(123),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(124)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(125)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(126)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(127)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(128)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(129)), //
						new DummyRegisterElement(130), //
						m(ProChannelId.FREQUENCY_L1, new UnsignedWordElement(131),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ProChannelId.FREQUENCY_L2, new UnsignedWordElement(132),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ProChannelId.FREQUENCY_L3, new UnsignedWordElement(133),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ProChannelId.SINGLE_PHASE_ALLOWED_APPARENT, new UnsignedWordElement(134)), //
						new DummyRegisterElement(135, 140), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(141),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(142)), //
						new DummyRegisterElement(143, 149)), //
				new FC3ReadRegistersTask(150, Priority.LOW, //
						m(new BitsWordElement(150, this) //
								.bit(0, ProChannelId.STATE_7) //
								.bit(1, ProChannelId.STATE_8) //
								.bit(2, ProChannelId.STATE_9) //
								.bit(3, ProChannelId.STATE_10) //
								.bit(4, ProChannelId.STATE_11) //
								.bit(5, ProChannelId.STATE_12) //
								.bit(6, ProChannelId.STATE_13) //
								.bit(7, ProChannelId.STATE_14) //
								.bit(8, ProChannelId.STATE_15) //
								.bit(9, ProChannelId.STATE_16) //
								.bit(10, ProChannelId.STATE_17)),
						m(new BitsWordElement(151, this) //
								.bit(0, ProChannelId.STATE_18)),
						m(new BitsWordElement(152, this) //
								.bit(0, ProChannelId.STATE_19) //
								.bit(1, ProChannelId.STATE_20) //
								.bit(2, ProChannelId.STATE_21) //
								.bit(3, ProChannelId.STATE_22) //
								.bit(4, ProChannelId.STATE_23) //
								.bit(5, ProChannelId.STATE_24) //
								.bit(6, ProChannelId.STATE_25) //
								.bit(7, ProChannelId.STATE_26) //
								.bit(8, ProChannelId.STATE_27) //
								.bit(9, ProChannelId.STATE_28) //
								.bit(10, ProChannelId.STATE_29) //
								.bit(11, ProChannelId.STATE_30) //
								.bit(12, ProChannelId.STATE_31) //
								.bit(13, ProChannelId.STATE_32) //
								.bit(14, ProChannelId.STATE_33) //
								.bit(15, ProChannelId.STATE_34)),
						m(new BitsWordElement(153, this) //
								.bit(0, ProChannelId.STATE_35) //
								.bit(1, ProChannelId.STATE_36) //
								.bit(2, ProChannelId.STATE_37) //
								.bit(3, ProChannelId.STATE_38) //
								.bit(4, ProChannelId.STATE_39) //
								.bit(5, ProChannelId.STATE_40) //
								.bit(6, ProChannelId.STATE_41) //
								.bit(7, ProChannelId.STATE_42) //
								.bit(8, ProChannelId.STATE_43) //
								.bit(9, ProChannelId.STATE_44) //
								.bit(10, ProChannelId.STATE_45) //
								.bit(11, ProChannelId.STATE_46) //
								.bit(12, ProChannelId.STATE_47) //
								.bit(13, ProChannelId.STATE_48) //
								.bit(14, ProChannelId.STATE_49) //
								.bit(15, ProChannelId.STATE_50)),
						m(new BitsWordElement(154, this) //
								.bit(0, ProChannelId.STATE_51) //
								.bit(1, ProChannelId.STATE_52) //
								.bit(2, ProChannelId.STATE_53) //
								.bit(3, ProChannelId.STATE_54) //
								.bit(4, ProChannelId.STATE_55) //
								.bit(5, ProChannelId.STATE_56) //
								.bit(6, ProChannelId.STATE_57) //
								.bit(7, ProChannelId.STATE_58) //
								.bit(8, ProChannelId.STATE_59) //
								.bit(9, ProChannelId.STATE_60) //
								.bit(10, ProChannelId.STATE_61) //
								.bit(11, ProChannelId.STATE_62) //
								.bit(12, ProChannelId.STATE_63)),
						m(new BitsWordElement(155, this) //
								.bit(0, ProChannelId.STATE_64) //
								.bit(1, ProChannelId.STATE_65) //
								.bit(2, ProChannelId.STATE_66) //
								.bit(3, ProChannelId.STATE_67) //
								.bit(4, ProChannelId.STATE_68) //
								.bit(5, ProChannelId.STATE_69) //
								.bit(6, ProChannelId.STATE_70) //
								.bit(7, ProChannelId.STATE_71) //
								.bit(8, ProChannelId.STATE_72) //
								.bit(9, ProChannelId.STATE_73) //
								.bit(10, ProChannelId.STATE_74)),
						m(new BitsWordElement(156, this) //
								.bit(0, ProChannelId.STATE_75)),
						m(new BitsWordElement(157, this) //
								.bit(0, ProChannelId.STATE_76) //
								.bit(1, ProChannelId.STATE_77) //
								.bit(2, ProChannelId.STATE_78) //
								.bit(3, ProChannelId.STATE_79) //
								.bit(4, ProChannelId.STATE_80) //
								.bit(5, ProChannelId.STATE_81) //
								.bit(6, ProChannelId.STATE_82) //
								.bit(7, ProChannelId.STATE_83) //
								.bit(8, ProChannelId.STATE_84) //
								.bit(9, ProChannelId.STATE_85) //
								.bit(10, ProChannelId.STATE_86) //
								.bit(11, ProChannelId.STATE_87) //
								.bit(12, ProChannelId.STATE_88) //
								.bit(13, ProChannelId.STATE_89) //
								.bit(14, ProChannelId.STATE_90) //
								.bit(15, ProChannelId.STATE_91)),
						m(new BitsWordElement(158, this) //
								.bit(0, ProChannelId.STATE_92) //
								.bit(1, ProChannelId.STATE_93) //
								.bit(2, ProChannelId.STATE_94) //
								.bit(3, ProChannelId.STATE_95) //
								.bit(4, ProChannelId.STATE_96) //
								.bit(5, ProChannelId.STATE_97) //
								.bit(6, ProChannelId.STATE_98) //
								.bit(7, ProChannelId.STATE_99) //
								.bit(8, ProChannelId.STATE_100) //
								.bit(9, ProChannelId.STATE_101) //
								.bit(10, ProChannelId.STATE_102) //
								.bit(11, ProChannelId.STATE_103) //
								.bit(12, ProChannelId.STATE_104) //
								.bit(13, ProChannelId.STATE_105) //
								.bit(14, ProChannelId.STATE_106) //
								.bit(15, ProChannelId.STATE_107)),
						m(new BitsWordElement(159, this) //
								.bit(0, ProChannelId.STATE_108) //
								.bit(1, ProChannelId.STATE_109) //
								.bit(2, ProChannelId.STATE_110) //
								.bit(3, ProChannelId.STATE_111) //
								.bit(4, ProChannelId.STATE_112) //
								.bit(5, ProChannelId.STATE_113) //
								.bit(6, ProChannelId.STATE_114) //
								.bit(7, ProChannelId.STATE_115) //
								.bit(8, ProChannelId.STATE_116) //
								.bit(9, ProChannelId.STATE_117) //
								.bit(10, ProChannelId.STATE_118) //
								.bit(11, ProChannelId.STATE_119) //
								.bit(12, ProChannelId.STATE_120)),
						m(new BitsWordElement(160, this) //
								.bit(0, ProChannelId.STATE_121) //
								.bit(1, ProChannelId.STATE_122) //
								.bit(2, ProChannelId.STATE_123) //
								.bit(3, ProChannelId.STATE_124) //
								.bit(4, ProChannelId.STATE_125) //
								.bit(5, ProChannelId.STATE_126) //
								.bit(6, ProChannelId.STATE_127) //
								.bit(7, ProChannelId.STATE_128) //
								.bit(8, ProChannelId.STATE_129) //
								.bit(9, ProChannelId.STATE_130) //
								.bit(10, ProChannelId.STATE_131)),
						m(new BitsWordElement(161, this) //
								.bit(0, ProChannelId.STATE_132)),
						m(new BitsWordElement(162, this) //
								.bit(0, ProChannelId.STATE_133) //
								.bit(1, ProChannelId.STATE_134) //
								.bit(2, ProChannelId.STATE_135) //
								.bit(3, ProChannelId.STATE_136) //
								.bit(4, ProChannelId.STATE_137) //
								.bit(5, ProChannelId.STATE_138) //
								.bit(6, ProChannelId.STATE_139) //
								.bit(7, ProChannelId.STATE_140) //
								.bit(8, ProChannelId.STATE_141) //
								.bit(9, ProChannelId.STATE_142) //
								.bit(10, ProChannelId.STATE_143) //
								.bit(11, ProChannelId.STATE_144) //
								.bit(12, ProChannelId.STATE_145) //
								.bit(13, ProChannelId.STATE_146) //
								.bit(14, ProChannelId.STATE_147) //
								.bit(15, ProChannelId.STATE_148)),
						m(new BitsWordElement(163, this) //
								.bit(0, ProChannelId.STATE_149) //
								.bit(1, ProChannelId.STATE_150) //
								.bit(2, ProChannelId.STATE_151) //
								.bit(3, ProChannelId.STATE_152) //
								.bit(4, ProChannelId.STATE_153) //
								.bit(5, ProChannelId.STATE_154) //
								.bit(6, ProChannelId.STATE_155) //
								.bit(7, ProChannelId.STATE_156) //
								.bit(8, ProChannelId.STATE_157) //
								.bit(9, ProChannelId.STATE_158) //
								.bit(10, ProChannelId.STATE_159) //
								.bit(11, ProChannelId.STATE_160) //
								.bit(12, ProChannelId.STATE_161) //
								.bit(13, ProChannelId.STATE_162) //
								.bit(14, ProChannelId.STATE_163) //
								.bit(15, ProChannelId.STATE_164)),
						m(new BitsWordElement(164, this) //
								.bit(0, ProChannelId.STATE_165) //
								.bit(1, ProChannelId.STATE_166) //
								.bit(2, ProChannelId.STATE_167) //
								.bit(3, ProChannelId.STATE_168) //
								.bit(4, ProChannelId.STATE_169) //
								.bit(5, ProChannelId.STATE_170) //
								.bit(6, ProChannelId.STATE_171) //
								.bit(7, ProChannelId.STATE_172) //
								.bit(8, ProChannelId.STATE_173) //
								.bit(9, ProChannelId.STATE_174) //
								.bit(10, ProChannelId.STATE_175) //
								.bit(11, ProChannelId.STATE_176) //
								.bit(12, ProChannelId.STATE_177))), //
				new FC16WriteRegistersTask(200, //
						m(ProChannelId.SET_WORK_STATE, new UnsignedWordElement(200))), //
				new FC16WriteRegistersTask(206, //
						m(ProChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(206)), //
						m(ProChannelId.SET_REACTIVE_POWER_L1, new SignedWordElement(207)), //
						m(ProChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(208)), //
						m(ProChannelId.SET_REACTIVE_POWER_L2, new SignedWordElement(209)), //
						m(ProChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(210)), //
						m(ProChannelId.SET_REACTIVE_POWER_L3, new SignedWordElement(211))), //

				new FC3ReadRegistersTask(3020, Priority.LOW, //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_1, new UnsignedWordElement(3020)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_2, new UnsignedWordElement(3021)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_3, new UnsignedWordElement(3022)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_4, new UnsignedWordElement(3023)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_5, new UnsignedWordElement(3024)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_6, new UnsignedWordElement(3025)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_7, new UnsignedWordElement(3026)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_8, new UnsignedWordElement(3027)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_9, new UnsignedWordElement(3028)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_10, new UnsignedWordElement(3029)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_11, new UnsignedWordElement(3030)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_12, new UnsignedWordElement(3031)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_13, new UnsignedWordElement(3032)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_14, new UnsignedWordElement(3033)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_15, new UnsignedWordElement(3034)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_16, new UnsignedWordElement(3035)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_1, new UnsignedWordElement(3036)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_2, new UnsignedWordElement(3037)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_3, new UnsignedWordElement(3038)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_4, new UnsignedWordElement(3039)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_5, new UnsignedWordElement(3040)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_6, new UnsignedWordElement(3041)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_7, new UnsignedWordElement(3042)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_8, new UnsignedWordElement(3043)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_9, new UnsignedWordElement(3044)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_10, new UnsignedWordElement(3045)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_11, new UnsignedWordElement(3046)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_12, new UnsignedWordElement(3047)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_13, new UnsignedWordElement(3048)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_14, new UnsignedWordElement(3049)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_15, new UnsignedWordElement(3050)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_16, new UnsignedWordElement(3051))), //
				new FC16WriteRegistersTask(9014, //
						m(ProChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(ProChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(ProChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(ProChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(ProChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(ProChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(ProChannelId.SETUP_MODE, new UnsignedWordElement(30558),
								new ChannelMetaInfoReadAndWrite(30157, 30558))), //
				new FC16WriteRegistersTask(30559, //
						m(ProChannelId.PCS_MODE, new UnsignedWordElement(30559),
								new ChannelMetaInfoReadAndWrite(30158, 30559))), //
				new FC3ReadRegistersTask(30157, Priority.LOW, //
						m(ProChannelId.SETUP_MODE, new UnsignedWordElement(30157),
								new ChannelMetaInfoReadAndWrite(30157, 30558)), //
						m(ProChannelId.PCS_MODE, new UnsignedWordElement(30158),
								new ChannelMetaInfoReadAndWrite(30158, 30559)))//

		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	private IntegerWriteChannel getSetActivePowerL1Channel() {
		return this.channel(ProChannelId.SET_ACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetActivePowerL2Channel() {
		return this.channel(ProChannelId.SET_ACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetActivePowerL3Channel() {
		return this.channel(ProChannelId.SET_ACTIVE_POWER_L3);
	}

	private IntegerWriteChannel getSetReactivePowerL1Channel() {
		return this.channel(ProChannelId.SET_REACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetReactivePowerL2Channel() {
		return this.channel(ProChannelId.SET_REACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetReactivePowerL3Channel() {
		return this.channel(ProChannelId.SET_REACTIVE_POWER_L3);
	}

	private EnumWriteChannel getPcsModeChannel() {
		return this.channel(ProChannelId.PCS_MODE);
	}

	private PcsMode getPcsMode() {
		return this.getPcsModeChannel().value().asEnum();
	}

	private EnumWriteChannel getSetupModeChannel() {
		return this.channel(ProChannelId.SETUP_MODE);
	}

	private SetupMode getSetupMode() {
		return this.getSetupModeChannel().value().asEnum();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.activateRemoteMode();
			this.maxApparentPowerHandler.calculateMaxApparentPower();
			break;
		}
	}

	/**
	 * Update Channels on TOPIC_CYCLE_BEFORE_PROCESS_IMAGE.
	 */
	private void updateChannels() {
		// Update Local-Mode Warning Channel
		EnumReadChannel controlModeChannel = this.channel(ProChannelId.CONTROL_MODE);
		ControlMode controlMode = controlModeChannel.getNextValue().asEnum();
		StateChannel localModeChannel = this.channel(ProChannelId.LOCAL_MODE);
		localModeChannel.setNextValue(controlMode == ControlMode.LOCAL);
	}

	/**
	 * Activates the Remote-Mode.
	 */
	private void activateRemoteMode() {
		try {
			if (this.getPcsMode() != PcsMode.REMOTE) {
				// If Mode is not "Remote"
				this.logWarn(this.log, "PCS-Mode is not 'Remote'. It's [" + this.getPcsMode() + "]");
				if (this.getSetupMode() == SetupMode.OFF) {
					// Activate SetupMode
					this.logInfo(this.log, "Activating Setup-Mode");
					this.getSetupModeChannel().setNextWriteValue(SetupMode.ON);
				} else {
					// Set Mode to "Remote"
					this.logInfo(this.log, "Setting PCS-Mode to 'Remote'");
					this.getPcsModeChannel().setNextWriteValue(PcsMode.REMOTE);
				}
			} else // If Mode is "Remote" and SetupMode is active
			if (this.getSetupMode() == SetupMode.ON) {
				// Deactivate SetupMode
				this.logInfo(this.log, "Deactivating Setup-Mode");
				this.getSetupModeChannel().setNextWriteValue(SetupMode.OFF);
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to activate Remote-Mode: " + e.getMessage());
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(FeneconProEss.class, accessMode, 300) //
						.build());
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
}
