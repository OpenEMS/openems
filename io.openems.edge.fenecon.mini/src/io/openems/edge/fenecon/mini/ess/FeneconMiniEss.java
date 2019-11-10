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

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelOffsetConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SinglePhase;
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

	private SinglePhase phase;

	public FeneconMiniEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				EssChannelId.values() //
		);
		this.getCapacity().setNextValue(3_000);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), FeneconMiniConstants.UNIT_ID, this.cm,
				"Modbus", config.modbus_id());
		this.phase = config.phase();
		SinglePhaseEss.initializeCopyPhaseChannel(this, config.phase());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.LOW, //
						m(EssChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(EssChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(EssChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						new DummyRegisterElement(109), //
						m(EssChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(EssChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(EssChannelId.BATTERY_POWER, new SignedWordElement(112))), //
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
						m(EssChannelId.BECU1_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(3000),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssChannelId.BECU1_DISCHARGE_CURRENT_LIMIT, new UnsignedWordElement(3001), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssChannelId.BECU1_TOTAL_VOLTAGE, new UnsignedWordElement(3002),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssChannelId.BECU1_TOTAL_CURRENT, new UnsignedWordElement(3003)), //
						m(EssChannelId.BECU1_SOC, new UnsignedWordElement(3004)), //
						m(new BitsWordElement(3005, this) //
								.bit(0, EssChannelId.STATE_1) //
								.bit(1, EssChannelId.STATE_2) //
								.bit(2, EssChannelId.STATE_3) //
								.bit(3, EssChannelId.STATE_4) //
								.bit(4, EssChannelId.STATE_5) //
								.bit(5, EssChannelId.STATE_6) //
								.bit(6, EssChannelId.STATE_7) //
								.bit(7, EssChannelId.STATE_8) //
								.bit(8, EssChannelId.STATE_9) //
								.bit(9, EssChannelId.STATE_10) //
								.bit(10, EssChannelId.STATE_11) //
								.bit(11, EssChannelId.STATE_12) //
								.bit(12, EssChannelId.STATE_13) //
								.bit(13, EssChannelId.STATE_14) //
								.bit(14, EssChannelId.STATE_15) //
								.bit(15, EssChannelId.STATE_16)), //
						m(new BitsWordElement(3006, this) //
								.bit(0, EssChannelId.STATE_17) //
								.bit(1, EssChannelId.STATE_18) //
								.bit(2, EssChannelId.STATE_19) //
								.bit(4, EssChannelId.STATE_20) //
								.bit(5, EssChannelId.STATE_21) //
								.bit(6, EssChannelId.STATE_22) //
								.bit(7, EssChannelId.STATE_23) //
								.bit(8, EssChannelId.STATE_24) //
								.bit(9, EssChannelId.STATE_25) //
								.bit(10, EssChannelId.STATE_26) //
								.bit(11, EssChannelId.STATE_27) //
								.bit(12, EssChannelId.STATE_28) //
								.bit(13, EssChannelId.STATE_29) //
								.bit(14, EssChannelId.STATE_30) //
								.bit(15, EssChannelId.STATE_31)), //
						m(new BitsWordElement(3007, this) //
								.bit(0, EssChannelId.STATE_32) //
								.bit(1, EssChannelId.STATE_33) //
								.bit(2, EssChannelId.STATE_34) //
								.bit(3, EssChannelId.STATE_35) //
								.bit(4, EssChannelId.STATE_36) //
								.bit(5, EssChannelId.STATE_37) //
								.bit(6, EssChannelId.STATE_38) //
								.bit(7, EssChannelId.STATE_39) //
								.bit(8, EssChannelId.STATE_40) //
								.bit(9, EssChannelId.STATE_41) //
								.bit(10, EssChannelId.STATE_42) //
								.bit(13, EssChannelId.STATE_43) //
								.bit(14, EssChannelId.STATE_44) //
								.bit(15, EssChannelId.STATE_45)), //
						m(new BitsWordElement(3008, this) //
								.bit(0, EssChannelId.STATE_46) //
								.bit(1, EssChannelId.STATE_47) //
								.bit(2, EssChannelId.STATE_48) //
								.bit(9, EssChannelId.STATE_49) //
								.bit(10, EssChannelId.STATE_50) //
								.bit(12, EssChannelId.STATE_51) //
								.bit(13, EssChannelId.STATE_52)), //
						m(EssChannelId.BECU1_VERSION, new UnsignedWordElement(3009)), //
						m(EssChannelId.BECU1_NOMINAL_CAPACITY, new UnsignedWordElement(3010)), //
						m(EssChannelId.BECU1_CURRENT_CAPACITY, new UnsignedWordElement(3011)), //
						m(EssChannelId.BECU1_MINIMUM_VOLTAGE_NO, new UnsignedWordElement(3012)), //
						m(EssChannelId.BECU1_MINIMUM_VOLTAGE, new UnsignedWordElement(3013)), //
						m(EssChannelId.BECU1_MAXIMUM_VOLTAGE_NO, new UnsignedWordElement(3014)), //
						m(EssChannelId.BECU1_MAXIMUM_VOLTAGE, new UnsignedWordElement(3015)), // ^
						m(EssChannelId.BECU1_MINIMUM_TEMPERATURE_NO, new UnsignedWordElement(3016)), //
						m(EssChannelId.BECU1_MINIMUM_TEMPERATURE, new UnsignedWordElement(3017),
								new ElementToChannelOffsetConverter(-40)), //
						m(EssChannelId.BECU1_MAXIMUM_TEMPERATURE_NO, new UnsignedWordElement(3018)), //
						m(EssChannelId.BECU1_MAXIMUM_TEMPERATURE, new UnsignedWordElement(3019),
								new ElementToChannelOffsetConverter(-40))),

				new FC3ReadRegistersTask(3200, Priority.LOW, //
						m(EssChannelId.BECU2_CHARGE_CURRENT, new UnsignedWordElement(3200)), //
						m(EssChannelId.BECU2_DISCHARGE_CURRENT, new UnsignedWordElement(3201)), //
						m(EssChannelId.BECU2_VOLT, new UnsignedWordElement(3202)), //
						m(EssChannelId.BECU2_CURRENT, new UnsignedWordElement(3203)), //
						m(EssChannelId.BECU2_SOC, new UnsignedWordElement(3204))), //
				new FC3ReadRegistersTask(3205, Priority.LOW, //
						m(new BitsWordElement(3205, this) //
								.bit(0, EssChannelId.STATE_53) //
								.bit(1, EssChannelId.STATE_54) //
								.bit(2, EssChannelId.STATE_55) //
								.bit(3, EssChannelId.STATE_56) //
								.bit(4, EssChannelId.STATE_57) //
								.bit(5, EssChannelId.STATE_58) //
								.bit(6, EssChannelId.STATE_59) //
								.bit(7, EssChannelId.STATE_60) //
								.bit(8, EssChannelId.STATE_61) //
								.bit(9, EssChannelId.STATE_62) //
								.bit(10, EssChannelId.STATE_63) //
								.bit(11, EssChannelId.STATE_64) //
								.bit(12, EssChannelId.STATE_65) //
								.bit(13, EssChannelId.STATE_66) //
								.bit(14, EssChannelId.STATE_67) //
								.bit(15, EssChannelId.STATE_68)), //
						m(new BitsWordElement(3206, this) //
								.bit(0, EssChannelId.STATE_69) //
								.bit(1, EssChannelId.STATE_70) //
								.bit(2, EssChannelId.STATE_71) //
								.bit(4, EssChannelId.STATE_72) //
								.bit(5, EssChannelId.STATE_73) //
								.bit(6, EssChannelId.STATE_74) //
								.bit(7, EssChannelId.STATE_75) //
								.bit(8, EssChannelId.STATE_76) //
								.bit(9, EssChannelId.STATE_77) //
								.bit(10, EssChannelId.STATE_78) //
								.bit(11, EssChannelId.STATE_79) //
								.bit(12, EssChannelId.STATE_80) //
								.bit(13, EssChannelId.STATE_81) //
								.bit(14, EssChannelId.STATE_82) //
								.bit(15, EssChannelId.STATE_83)),
						m(new BitsWordElement(3207, this) //
								.bit(0, EssChannelId.STATE_84) //
								.bit(1, EssChannelId.STATE_85) //
								.bit(2, EssChannelId.STATE_86) //
								.bit(3, EssChannelId.STATE_87) //
								.bit(4, EssChannelId.STATE_88) //
								.bit(5, EssChannelId.STATE_89) //
								.bit(6, EssChannelId.STATE_90) //
								.bit(7, EssChannelId.STATE_91) //
								.bit(8, EssChannelId.STATE_92) //
								.bit(9, EssChannelId.STATE_93) //
								.bit(10, EssChannelId.STATE_94) //
								.bit(13, EssChannelId.STATE_95) //
								.bit(14, EssChannelId.STATE_96) //
								.bit(15, EssChannelId.STATE_97)), //
						m(new BitsWordElement(3208, this) //
								.bit(0, EssChannelId.STATE_98) //
								.bit(1, EssChannelId.STATE_99) //
								.bit(2, EssChannelId.STATE_100) //
								.bit(9, EssChannelId.STATE_101) //
								.bit(10, EssChannelId.STATE_102) //
								.bit(12, EssChannelId.STATE_103) //
								.bit(13, EssChannelId.STATE_104)),
						m(EssChannelId.BECU2_VERSION, new UnsignedWordElement(3209)), //
						new DummyRegisterElement(3210, 3211), //
						m(EssChannelId.BECU2_MIN_VOLT_NO, new UnsignedWordElement(3212)), //
						m(EssChannelId.BECU2_MIN_VOLT, new UnsignedWordElement(3213)), //
						m(EssChannelId.BECU2_MAX_VOLT_NO, new UnsignedWordElement(3214)), //
						m(EssChannelId.BECU2_MAX_VOLT, new UnsignedWordElement(3215)), // ^
						m(EssChannelId.BECU2_MIN_TEMP_NO, new UnsignedWordElement(3216)), //
						m(EssChannelId.BECU2_MIN_TEMP, new UnsignedWordElement(3217)), //
						m(EssChannelId.BECU2_MAX_TEMP_NO, new UnsignedWordElement(3218)), //
						m(EssChannelId.BECU2_MAX_TEMP, new UnsignedWordElement(3219))), //
				new FC3ReadRegistersTask(4000, Priority.LOW, //
						m(EssChannelId.SYSTEM_WORK_STATE, new UnsignedDoublewordElement(4000)), //
						m(EssChannelId.SYSTEM_WORK_MODE_STATE, new UnsignedDoublewordElement(4002))), //
				new FC3ReadRegistersTask(4800, Priority.LOW, //
						m(EssChannelId.BECU_NUM, new UnsignedWordElement(4800)), //
						// TODO BECU_WORK_STATE has been implemented with both registers(4801 and 4807)
						m(EssChannelId.BECU_WORK_STATE, new UnsignedWordElement(4801)), //
						new DummyRegisterElement(4802), //
						m(EssChannelId.BECU_CHARGE_CURRENT, new UnsignedWordElement(4803)), //
						m(EssChannelId.BECU_DISCHARGE_CURRENT, new UnsignedWordElement(4804)), //
						m(EssChannelId.BECU_VOLT, new UnsignedWordElement(4805)), //
						m(EssChannelId.BECU_CURRENT, new UnsignedWordElement(4806))), //
				new FC16WriteRegistersTask(4809, //
						m(new BitsWordElement(4809, this) //
								.bit(0, EssChannelId.STATE_111) //
								.bit(1, EssChannelId.STATE_112))), //
				new FC3ReadRegistersTask(4808, Priority.LOW, //
						m(new BitsWordElement(4808, this) //
								.bit(0, EssChannelId.STATE_105) //
								.bit(1, EssChannelId.STATE_106) //
								.bit(2, EssChannelId.STATE_107) //
								.bit(3, EssChannelId.STATE_108) //
								.bit(4, EssChannelId.STATE_109) //
								.bit(9, EssChannelId.STATE_110)), //
						m(new BitsWordElement(4809, this) //
								.bit(0, EssChannelId.STATE_111) //
								.bit(1, EssChannelId.STATE_112)), //
						m(new BitsWordElement(4810, this) //
								.bit(0, EssChannelId.STATE_113) //
								.bit(1, EssChannelId.STATE_114) //
								.bit(2, EssChannelId.STATE_115) //
								.bit(3, EssChannelId.STATE_116) //
								.bit(4, EssChannelId.STATE_117) //
								.bit(5, EssChannelId.STATE_118) //
								.bit(6, EssChannelId.STATE_119) //
								.bit(7, EssChannelId.STATE_120) //
								.bit(8, EssChannelId.STATE_121) //
								.bit(9, EssChannelId.STATE_122) //
								.bit(10, EssChannelId.STATE_123) //
								.bit(11, EssChannelId.STATE_124) //
								.bit(12, EssChannelId.STATE_125) //
								.bit(13, EssChannelId.STATE_126) //
								.bit(14, EssChannelId.STATE_127) //
								.bit(15, EssChannelId.STATE_128)), //
						m(new BitsWordElement(4811, this) //
								.bit(0, EssChannelId.STATE_129) //
								.bit(1, EssChannelId.STATE_130) //
								.bit(2, EssChannelId.STATE_131) //
								.bit(3, EssChannelId.STATE_132) //
								.bit(4, EssChannelId.STATE_133) //
								.bit(5, EssChannelId.STATE_134) //
								.bit(6, EssChannelId.STATE_135) //
								.bit(7, EssChannelId.STATE_136) //
								.bit(8, EssChannelId.STATE_137) //
								.bit(9, EssChannelId.STATE_138) //
								.bit(10, EssChannelId.STATE_139) //
								.bit(11, EssChannelId.STATE_140) //
								.bit(12, EssChannelId.STATE_141) //
								.bit(13, EssChannelId.STATE_142) //
								.bit(14, EssChannelId.STATE_143)),
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(4812), new ElementToChannelConverter(
								// element -> channel
								value -> {
									// Set SoC to 100 % if battery is full and AllowedCharge is zero
									if (value == null) {
										return null;
									}
									int soc = (Integer) value;
									IntegerReadChannel allowedCharge = this
											.channel(EssChannelId.BECU1_CHARGE_CURRENT_LIMIT);
									IntegerReadChannel allowedDischarge = this
											.channel(EssChannelId.BECU1_DISCHARGE_CURRENT_LIMIT);
									if (soc > 95 && allowedCharge.value().orElse(-1) == 0
											&& allowedDischarge.value().orElse(0) != 0) {
										return 100;
									} else {
										return value;
									}
								}, //
									// channel -> element
								value -> value)) //
				), //

				new FC3ReadRegistersTask(30166, Priority.LOW, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(30166))), //
				new FC16WriteRegistersTask(9014, //
						m(EssChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(EssChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(EssChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(EssChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(EssChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(EssChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(EssChannelId.SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(EssChannelId.PCS_MODE, new UnsignedWordElement(30559))), //
				new FC16WriteRegistersTask(30157, //
						m(EssChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(EssChannelId.PCS_MODE, new UnsignedWordElement(30158))));//
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString(); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(FeneconMiniEss.class, accessMode, 300) //
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