package io.openems.edge.goodwe.et;

import java.util.function.Consumer;

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

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.goodwe.et.charger.GoodweETChargerPV1;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodweET.Battery-Inverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

public class GoodweETImpl extends AbstractOpenemsModbusComponent
		implements GoodweET, AsymmetricEss, SymmetricEss, OpenemsComponent {

	protected GoodweETChargerPV1 charger = null;

	protected final static int MAX_APPARENT_POWER = 5_000;

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;


	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus); // Bridge Modbus
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GoodweETImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				GoodweChannelIdET.values() // 
		);
		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).setNextValue(GoodweETImpl.MAX_APPARENT_POWER);
		
		final Channel<Integer> essPower = this.channel(GoodweChannelIdET.P_BATTERY1);
		final Channel<Integer> pvPower1 = this.channel(GoodweChannelIdET.P_PV1);
		final Channel<Integer> pvPower2 = this.channel(GoodweChannelIdET.P_PV2);
		final Consumer<Value<Integer>> actualPowerSum = ignore -> {
			this.getActivePower().setNextValue(TypeUtils.sum(pvPower1.value().get(), pvPower2.value().get(), essPower.value().get()));
		};
		pvPower1.onSetNextValue(actualPowerSum);
		pvPower2.onSetNextValue(actualPowerSum);
		essPower.onSetNextValue(actualPowerSum);
	}

	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(35000, Priority.LOW, //
						m(GoodweChannelIdET.MODBUS_PROTOCOL_VERSION, new UnsignedWordElement(35000)), //
						m(GoodweChannelIdET.RATED_POWER, new UnsignedWordElement(35001)), //
						m(GoodweChannelIdET.AC_OUTPUT_TYPE, new UnsignedWordElement(35002)), //
						m(GoodweChannelIdET.SERIAL_NUMBER, new StringWordElement(35003, 8)), //
						m(GoodweChannelIdET.DEVICE_TYPE, new StringWordElement(35011, 5))), //
				new FC3ReadRegistersTask(35016, Priority.LOW, //
						m(GoodweChannelIdET.DSP1_SOFTWARE_VERSION, new UnsignedWordElement(35016)), //
						m(GoodweChannelIdET.DSP2_SOFTWARE_VERSION, new UnsignedWordElement(35017)), //
						m(GoodweChannelIdET.DSP_SPN_VERSION, new UnsignedWordElement(35018)), //
						m(GoodweChannelIdET.ARM_SOFTWARE_VERSION, new UnsignedWordElement(35019)), //
						m(GoodweChannelIdET.ARM_SVN_VERSION, new UnsignedWordElement(35020)), //
						m(GoodweChannelIdET.DSP_INTERNAL_FIRMWARE_VERSION, new StringWordElement(35021, 6)), //
						m(GoodweChannelIdET.ARM_INTERNAL_FIRMWARE_VERSION, new StringWordElement(35027, 6))), //
				new FC3ReadRegistersTask(35050, Priority.LOW, //
						m(GoodweChannelIdET.SIMCCID, new StringWordElement(35050, 10))), //
				new FC3ReadRegistersTask(35100, Priority.LOW, //
						m(GoodweChannelIdET.RTC_YEAR_MONTH, new UnsignedWordElement(35100)), //
						m(GoodweChannelIdET.RTC_DATE_HOUR, new UnsignedWordElement(35101)), //
						m(GoodweChannelIdET.RTC_MINUTE_SECOND, new UnsignedWordElement(35102))), //
				
				new FC3ReadRegistersTask(35111, Priority.LOW, //
						m(GoodweChannelIdET.V_PV3, new UnsignedWordElement(35111),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.I_PV3, new UnsignedWordElement(35112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35113, 35114), //
						m(GoodweChannelIdET.V_PV4, new UnsignedWordElement(35115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.I_PV4, new UnsignedWordElement(35116),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35117, 35118), //
						m(GoodweChannelIdET.PV_MODE, new UnsignedDoublewordElement(35119))), //
//						
				new FC3ReadRegistersTask(35136, Priority.HIGH, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(35136), //
								new ElementToChannelConverter((value) -> {
									Integer intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
									if (intValue != null) {
										switch (intValue) {
										case 0:
											return GridMode.OFF_GRID;
										case 1:
											return GridMode.ON_GRID;
										case 2:
											return GridMode.UNDEFINED;
										}
									}
									return GridMode.UNDEFINED;
								}))), //
				new FC3ReadRegistersTask(35138, Priority.LOW, //
						m(GoodweChannelIdET.TOTAL_INV_POWER, new SignedWordElement(35138)), //
						new DummyRegisterElement(35139, 35143), //
						m(GoodweChannelIdET.AC_APPARENT_POWER, new SignedWordElement(35144)), //
						m(GoodweChannelIdET.BACK_UP_V_LOAD_R, new UnsignedWordElement(35145), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BACK_UP_I_LOAD_R, new UnsignedWordElement(35146),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BACK_UP_F_LOAD_R, new UnsignedWordElement(35147),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.LOAD_MODE_R, new UnsignedWordElement(35148)), //
						new DummyRegisterElement(35149), //
						m(GoodweChannelIdET.BACK_UP_P_LOAD_R, new SignedWordElement(35150)), //
						m(GoodweChannelIdET.BACK_UP_V_LOAD_S, new UnsignedWordElement(35151),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BACK_UP_I_LOAD_S, new UnsignedWordElement(35152),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BACK_UP_F_LOAD_S, new UnsignedWordElement(35153),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.LOAD_MODE_S, new UnsignedWordElement(35154)), //
						new DummyRegisterElement(35155), //
						m(GoodweChannelIdET.BACK_UP_P_LOAD_S, new SignedWordElement(35156)), //
						m(GoodweChannelIdET.BACK_UP_V_LOAD_T, new UnsignedWordElement(35157),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BACK_UP_I_LOAD_T, new UnsignedWordElement(35158),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BACK_UP_F_LOAD_T, new UnsignedWordElement(35159),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.LOAD_MODE_T, new UnsignedWordElement(35160)), //
						new DummyRegisterElement(35161), //
						m(GoodweChannelIdET.BACK_UP_P_LOAD_T, new SignedWordElement(35162)), //
						new DummyRegisterElement(35163), //
						m(GoodweChannelIdET.P_LOAD_R, new SignedWordElement(35164)), //
						new DummyRegisterElement(35165), //
						m(GoodweChannelIdET.P_LOAD_S, new SignedWordElement(35166)), //
						new DummyRegisterElement(35167), //
						m(GoodweChannelIdET.P_LOAD_T, new SignedWordElement(35168)), //
						new DummyRegisterElement(35169), //
						m(GoodweChannelIdET.TOTAL_BACK_UP_LOAD, new SignedWordElement(35170)), //
						new DummyRegisterElement(35171), //
						m(GoodweChannelIdET.TOTAL_LOAD_POWER, new SignedWordElement(35172)), //
						m(GoodweChannelIdET.UPS_LOAD_PERCENT, new UnsignedWordElement(35173),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

				new FC3ReadRegistersTask(35174, Priority.LOW, //
						m(GoodweChannelIdET.AIR_TEMPERATURE, new SignedWordElement(35174),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.MODULE_TEMPERATURE, new SignedWordElement(35175),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.RADIATOR_TEMPERATURE, new SignedWordElement(35176),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.FUNCTION_BIT_VALUE, new UnsignedWordElement(35177)), //
						m(GoodweChannelIdET.BUS_VOLTAGE, new UnsignedWordElement(35178),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.NBUS_VOLTAGE, new UnsignedWordElement(35179),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V_BATTERY1, new UnsignedWordElement(35180),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.I_BATTERY1, new SignedWordElement(35181),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35182), //
						m(GoodweChannelIdET.P_BATTERY1, new SignedWordElement(35183)), //
						m(GoodweChannelIdET.BATTERY_MODE, new UnsignedWordElement(35184)), //
						m(GoodweChannelIdET.WARNING_CODE, new UnsignedWordElement(35185)), //
						m(GoodweChannelIdET.SAFETY_COUNTRY, new UnsignedWordElement(35186)), //
						m(GoodweChannelIdET.WORK_MODE, new UnsignedWordElement(35187)), //
						m(GoodweChannelIdET.OPERATION_MODE, new UnsignedDoublewordElement(35188))), //

				new FC3ReadRegistersTask(35105, Priority.LOW, //
						m(GoodweChannelIdET.P_PV1, new UnsignedDoublewordElement(35105)), //
						new DummyRegisterElement(35107, 35108), //
						m(GoodweChannelIdET.P_PV2, new UnsignedDoublewordElement(35109)), //
						new DummyRegisterElement(35111, 35112), //
						m(GoodweChannelIdET.P_PV3, new UnsignedDoublewordElement(35113)), //
						new DummyRegisterElement(35115, 35116), //
						m(GoodweChannelIdET.P_PV4, new UnsignedDoublewordElement(35117))), //

				new FC3ReadRegistersTask(35189, Priority.LOW, //
						m(new BitsWordElement(35189, this) //
								.bit(0, GoodweChannelIdET.STATE_0) //
								.bit(1, GoodweChannelIdET.STATE_1) //
								.bit(2, GoodweChannelIdET.STATE_2) //
								.bit(3, GoodweChannelIdET.STATE_3) //
								.bit(4, GoodweChannelIdET.STATE_4) //
								.bit(5, GoodweChannelIdET.STATE_5) //
								.bit(6, GoodweChannelIdET.STATE_6) //
								.bit(7, GoodweChannelIdET.STATE_7) //
								.bit(8, GoodweChannelIdET.STATE_8) //
								.bit(9, GoodweChannelIdET.STATE_9) //
								.bit(10, GoodweChannelIdET.STATE_10) //
								.bit(11, GoodweChannelIdET.STATE_11) //
								.bit(12, GoodweChannelIdET.STATE_12) //
								.bit(13, GoodweChannelIdET.STATE_13) //
								.bit(14, GoodweChannelIdET.STATE_14) //
								.bit(15, GoodweChannelIdET.STATE_15) //
						), //
						m(new BitsWordElement(35190, this) //
								.bit(0, GoodweChannelIdET.STATE_16) //
								.bit(1, GoodweChannelIdET.STATE_17) //
								.bit(2, GoodweChannelIdET.STATE_18) //
								.bit(3, GoodweChannelIdET.STATE_19) //
								.bit(4, GoodweChannelIdET.STATE_20) //
								.bit(5, GoodweChannelIdET.STATE_21) //
								.bit(6, GoodweChannelIdET.STATE_22) //
								.bit(7, GoodweChannelIdET.STATE_23) //
								.bit(8, GoodweChannelIdET.STATE_24) //
								.bit(9, GoodweChannelIdET.STATE_25) //
								.bit(10, GoodweChannelIdET.STATE_26) //
								.bit(11, GoodweChannelIdET.STATE_27) //
								.bit(12, GoodweChannelIdET.STATE_28) //
								.bit(13, GoodweChannelIdET.STATE_29) //
								.bit(14, GoodweChannelIdET.STATE_30) //
								.bit(15, GoodweChannelIdET.STATE_31))),

				new FC3ReadRegistersTask(35191, Priority.LOW, //
						m(GoodweChannelIdET.PV_E_TOTAL, new UnsignedDoublewordElement(35191),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.PV_E_DAY, new UnsignedDoublewordElement(35193),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL, new UnsignedDoublewordElement(35195),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.H_TOTAL, new UnsignedDoublewordElement(35197)), //
						m(GoodweChannelIdET.E_DAY_SELL, new UnsignedWordElement(35199),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_BUY, new UnsignedDoublewordElement(35200),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_DAY_BUY, new UnsignedWordElement(35202),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_LOAD, new UnsignedDoublewordElement(35203),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_LOAD_DAY, new UnsignedWordElement(35205),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_BATTERY_CHARGE, new UnsignedDoublewordElement(35206),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_CHARGE_DAY, new UnsignedWordElement(35208),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_BATTERY_DISCHARGE, new UnsignedDoublewordElement(35209),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_DISCHARGE_DAY, new UnsignedWordElement(35211),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_STRINGS, new UnsignedWordElement(35212)), //
						m(GoodweChannelIdET.CPLD_WARNING_CODE, new UnsignedWordElement(35213)), //
						m(GoodweChannelIdET.W_CHARGER_CTRL_FLAG, new UnsignedWordElement(35214)), //
						m(GoodweChannelIdET.DERATE_FLAG, new UnsignedWordElement(35215)), //
						new DummyRegisterElement(35216), //
						m(GoodweChannelIdET.DERATE_FROZEN_POWER, new SignedWordElement(35217)), //
						m(GoodweChannelIdET.DIAG_STATUS_H, new UnsignedDoublewordElement(35218)), //
						m(GoodweChannelIdET.DIAG_STATUS_L, new UnsignedDoublewordElement(35220))), //

				new FC3ReadRegistersTask(36000, Priority.LOW, //
						m(GoodweChannelIdET.COM_MODE, new UnsignedWordElement(36000)), //
						m(GoodweChannelIdET.RSSI, new UnsignedWordElement(36001)), //
						m(GoodweChannelIdET.MANIFACTURE_CODE, new UnsignedWordElement(36002)), //
						m(GoodweChannelIdET.B_METER_COMMUNICATE_STATUS, new UnsignedWordElement(36003)), //
						m(GoodweChannelIdET.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004)), //
						new DummyRegisterElement(36005, 36009), //
						m(GoodweChannelIdET.METER_PF_R, new UnsignedWordElement(36010),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.METER_PF_S, new UnsignedWordElement(36011),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.METER_PF_T, new UnsignedWordElement(36012),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.METER_POWER_FACTOR, new UnsignedWordElement(36013),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.METER_FREQUENCE, new UnsignedWordElement(36014),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.E_TOTAL_SELL, new FloatDoublewordElement(36015),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_BUY2, new UnsignedWordElement(36017),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(37000, Priority.LOW, //
						m(new BitsWordElement(37000, this) //
								.bit(0, GoodweChannelIdET.STATE_32) //
								.bit(1, GoodweChannelIdET.STATE_33) //
								.bit(2, GoodweChannelIdET.STATE_34) //
								.bit(3, GoodweChannelIdET.STATE_35) //
								.bit(4, GoodweChannelIdET.STATE_36) //
								.bit(5, GoodweChannelIdET.STATE_37) //
								.bit(6, GoodweChannelIdET.STATE_38) //
								.bit(7, GoodweChannelIdET.STATE_39) //
								.bit(8, GoodweChannelIdET.STATE_40) //
								.bit(15, GoodweChannelIdET.STATE_41))),

				new FC3ReadRegistersTask(37001, Priority.LOW,
						m(GoodweChannelIdET.BATTERY_TYPE_INDEX, new UnsignedWordElement(37001)), //
						m(GoodweChannelIdET.BMS_STATUS, new UnsignedWordElement(37002)), //
						m(GoodweChannelIdET.BMS_PACK_TEMPERATURE, new UnsignedWordElement(37003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BMS_CHARGE_IMAX, new UnsignedWordElement(37004)), //
						m(GoodweChannelIdET.BMS_DISCHARGE_IMAX, new UnsignedWordElement(37005))), //

				new FC3ReadRegistersTask(37006, Priority.LOW, //
						m(new BitsWordElement(37006, this) //
								.bit(0, GoodweChannelIdET.STATE_42) //
								.bit(1, GoodweChannelIdET.STATE_43) //
								.bit(2, GoodweChannelIdET.STATE_44) //
								.bit(3, GoodweChannelIdET.STATE_45) //
								.bit(4, GoodweChannelIdET.STATE_46) //
								.bit(5, GoodweChannelIdET.STATE_47) //
								.bit(6, GoodweChannelIdET.STATE_48) //
								.bit(7, GoodweChannelIdET.STATE_49) //
								.bit(8, GoodweChannelIdET.STATE_50) //
								.bit(9, GoodweChannelIdET.STATE_51) //
								.bit(10, GoodweChannelIdET.STATE_52) //
								.bit(11, GoodweChannelIdET.STATE_53) //
								.bit(12, GoodweChannelIdET.STATE_54) //
								.bit(13, GoodweChannelIdET.STATE_55) //
								.bit(14, GoodweChannelIdET.STATE_56) //
								.bit(15, GoodweChannelIdET.STATE_57))),

				new FC3ReadRegistersTask(37007, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(37007), new ElementToChannelConverter(
								// element -> channel
								value -> {
									// Set SoC to undefined if there is No Battery
									EnumReadChannel batteryModeChannel = this.channel(GoodweChannelIdET.BATTERY_MODE);
									BatteryMode batteryMode = batteryModeChannel.value().asEnum();
									if (batteryMode == BatteryMode.NO_BATTERY || batteryMode == BatteryMode.UNDEFINED) {
										return null;
									} else {
										return value;
									}
								},
								// channel -> element
								value -> value)), //
						m(GoodweChannelIdET.BMS_SOH, new UnsignedWordElement(37008)), //
						m(GoodweChannelIdET.BMS_BATTERY_STRINGS, new UnsignedWordElement(37009))), //

				new FC3ReadRegistersTask(37010, Priority.LOW, //
						m(new BitsWordElement(37010, this) //
								.bit(0, GoodweChannelIdET.STATE_58) //
								.bit(1, GoodweChannelIdET.STATE_59) //
								.bit(2, GoodweChannelIdET.STATE_60) //
								.bit(3, GoodweChannelIdET.STATE_61) //
								.bit(4, GoodweChannelIdET.STATE_62) //
								.bit(5, GoodweChannelIdET.STATE_63) //
								.bit(6, GoodweChannelIdET.STATE_64) //
								.bit(7, GoodweChannelIdET.STATE_65) //
								.bit(8, GoodweChannelIdET.STATE_66) //
								.bit(9, GoodweChannelIdET.STATE_67) //
								.bit(10, GoodweChannelIdET.STATE_68) //
								.bit(11, GoodweChannelIdET.STATE_69))),

				new FC3ReadRegistersTask(37011, Priority.LOW,
						m(GoodweChannelIdET.BATTERY_PROTOCOL, new UnsignedWordElement(37011))), //
				new FC3ReadRegistersTask(45000, Priority.LOW,
						m(GoodweChannelIdET.USER_PASSWORD1, new StringWordElement(45000, 8)), //
						m(GoodweChannelIdET.USER_PASSWORD2, new StringWordElement(45008, 8)),
						m(GoodweChannelIdET.USER_PASSWORD3, new StringWordElement(45016, 8)),
						m(GoodweChannelIdET.ROUTER_SSID, new StringWordElement(45024, 30)),
						m(GoodweChannelIdET.ROUTER_PASSWORD, new StringWordElement(45054, 20)),
						m(GoodweChannelIdET.ROUTER_ENCRYPTION_METHOD, new StringWordElement(45074, 1)),
						m(GoodweChannelIdET.DOMAIN1, new StringWordElement(45075, 25))),
				new FC3ReadRegistersTask(45100, Priority.LOW,
						m(GoodweChannelIdET.PORT_NUMBER1, new UnsignedWordElement(45100)),
						m(GoodweChannelIdET.DOMAIN2, new StringWordElement(45101, 25)),
						m(GoodweChannelIdET.PORT_NUMBER2, new UnsignedWordElement(45126)),
						m(GoodweChannelIdET.MODBUS_ADDRESS, new UnsignedWordElement(45127)),
						m(GoodweChannelIdET.MODBUS_MANUFACTURER, new StringWordElement(45128, 4)),
						m(GoodweChannelIdET.MODBUS_BADRATE_485, new UnsignedDoublewordElement(45132))),
				new FC3ReadRegistersTask(45200, Priority.LOW,
						m(GoodweChannelIdET.RTC_YEAR_MONTH_2, new UnsignedWordElement(45200)),
						m(GoodweChannelIdET.RTC_DAY_HOUR_2, new UnsignedWordElement(45201)),
						m(GoodweChannelIdET.RTC_MINUTE_SECOND_2, new UnsignedWordElement(45202)),
						m(GoodweChannelIdET.SERIAL_NUMBER_2, new StringWordElement(45203, 8)),
						m(GoodweChannelIdET.DEVICE_TYPE_2, new StringWordElement(45211, 5)),
						m(GoodweChannelIdET.RESUME_FACTORY_SETTING, new UnsignedWordElement(45216)),
						m(GoodweChannelIdET.CLEAR_DATA, new UnsignedWordElement(45217))),
				new FC16WriteRegistersTask(45000, //
						m(GoodweChannelIdET.USER_PASSWORD1, new StringWordElement(45000, 8)), //
						m(GoodweChannelIdET.USER_PASSWORD2, new StringWordElement(45008, 8)),
						m(GoodweChannelIdET.USER_PASSWORD3, new StringWordElement(45016, 8)),
						m(GoodweChannelIdET.ROUTER_SSID, new StringWordElement(45024, 30)),
						m(GoodweChannelIdET.ROUTER_PASSWORD, new StringWordElement(45054, 20)),
						m(GoodweChannelIdET.ROUTER_ENCRYPTION_METHOD, new StringWordElement(45074, 1)),
						m(GoodweChannelIdET.DOMAIN1, new StringWordElement(45075, 25))),
				new FC16WriteRegistersTask(45100, //
						m(GoodweChannelIdET.PORT_NUMBER1, new UnsignedWordElement(45100)),
						m(GoodweChannelIdET.DOMAIN2, new StringWordElement(45101, 25)),
						m(GoodweChannelIdET.PORT_NUMBER2, new UnsignedWordElement(45126)),
						m(GoodweChannelIdET.MODBUS_ADDRESS, new UnsignedWordElement(45127)),
						m(GoodweChannelIdET.MODBUS_MANUFACTURER, new StringWordElement(45128, 4)),
						m(GoodweChannelIdET.MODBUS_BADRATE_485, new UnsignedDoublewordElement(45132))),
				new FC16WriteRegistersTask(45200, //
						m(GoodweChannelIdET.RTC_YEAR_MONTH_2, new UnsignedWordElement(45200)),
						m(GoodweChannelIdET.RTC_DAY_HOUR_2, new UnsignedWordElement(45201)),
						m(GoodweChannelIdET.RTC_MINUTE_SECOND_2, new UnsignedWordElement(45202)),
						m(GoodweChannelIdET.SERIAL_NUMBER_2, new StringWordElement(45203, 8)),
						m(GoodweChannelIdET.DEVICE_TYPE_2, new StringWordElement(45211, 5)),
						m(GoodweChannelIdET.RESUME_FACTORY_SETTING, new UnsignedWordElement(45216)),
						m(GoodweChannelIdET.CLEAR_DATA, new UnsignedWordElement(45217)),
						m(GoodweChannelIdET.START, new UnsignedWordElement(45218)),
						m(GoodweChannelIdET.STOP, new UnsignedWordElement(45219)),
						m(GoodweChannelIdET.RESET, new UnsignedWordElement(45220)),
						m(GoodweChannelIdET.RESET_SPS, new UnsignedWordElement(45221))),
				new FC16WriteRegistersTask(45222,
						m(GoodweChannelIdET.PV_E_TOTAL_3, new UnsignedDoublewordElement(45222),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.PV_E_DAY_3, new UnsignedDoublewordElement(45224),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_SELL_3, new UnsignedDoublewordElement(45226),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.H_TOTAL_3, new UnsignedDoublewordElement(45228)), //
						m(GoodweChannelIdET.E_DAY_SELL_3, new UnsignedWordElement(45230),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_BUY_3, new UnsignedDoublewordElement(45231),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_DAY_BUY_3, new UnsignedWordElement(45233),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_LOAD_3, new UnsignedDoublewordElement(45234),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_LOAD_DAY_3, new UnsignedWordElement(45236),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_BATTERY_CHARGE_3, new UnsignedDoublewordElement(45237),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_CHARGE_DAY_3, new UnsignedWordElement(45239),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_BATTERY_DISCHARGE_3, new UnsignedDoublewordElement(45240),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_DISCHARGE_DAY_3, new UnsignedWordElement(45242),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.LANGUAGE, new UnsignedWordElement(45243)), //
						m(GoodweChannelIdET.SAFETY_COUNTRY_CODE, new UnsignedWordElement(45244)), //
						m(GoodweChannelIdET.ISO, new UnsignedWordElement(45245)), //
						m(GoodweChannelIdET.LVRT, new UnsignedWordElement(45246)), //
						m(GoodweChannelIdET.ISLANDING, new UnsignedWordElement(45247)), //
						new DummyRegisterElement(45248), //
						m(GoodweChannelIdET.BURN_IN_RESET_TIME, new UnsignedWordElement(45249)), //
						m(GoodweChannelIdET.PV_START_VOLTAGE, new UnsignedWordElement(45250),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.ENABLE_MPPT_4SHADOW, new UnsignedWordElement(45251)), //
						m(GoodweChannelIdET.BACK_UP_ENABLE, new UnsignedWordElement(45252)), //
						m(GoodweChannelIdET.AUTO_START_BACKUP, new UnsignedWordElement(45253)), //
						m(GoodweChannelIdET.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(45254)), //
						m(GoodweChannelIdET.REPAID_CUT_OFF, new UnsignedWordElement(45255)), //
						m(GoodweChannelIdET.BACKUP_START_DLY, new UnsignedWordElement(45256)), //
						m(GoodweChannelIdET.UPS_STD_VOLT_TYPE, new UnsignedWordElement(45257)), //
						m(GoodweChannelIdET.UNDER_ATS, new UnsignedWordElement(45258)), //
						m(GoodweChannelIdET.BURN_IN_MODE, new UnsignedWordElement(45259)), //
						m(GoodweChannelIdET.BACKUP_OVERLOAD_DELAY, new UnsignedWordElement(45260)), //
						m(GoodweChannelIdET.UPSPHASE_TYPE, new UnsignedWordElement(45261)), // .
						new DummyRegisterElement(45262), //
						m(GoodweChannelIdET.DERATE_RATE_VDE, new UnsignedWordElement(45263))), //

				new FC16WriteRegistersTask(45350, //
						m(GoodweChannelIdET.LEAD_BAT_CAPACITY, new UnsignedWordElement(45350)), //
						m(GoodweChannelIdET.BATTERY_STRINGS, new UnsignedWordElement(45351)), //
						m(GoodweChannelIdET.BATT_CHARGE_VOLT_MAX, new UnsignedWordElement(45352),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_CHARGE_CURR_MAX, new UnsignedWordElement(45353),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_VOLT_UNDER_MIN, new UnsignedWordElement(45354),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_DISCHARGE_CURR_MAX, new UnsignedWordElement(45355),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_SOC_UNDER_MIN, new UnsignedWordElement(45356)), //
						m(GoodweChannelIdET.BATT_OFF_LINE_VOLT_UNDER_MIN, new UnsignedWordElement(45357),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(45358)), //
						new DummyRegisterElement(45359), //
						m(GoodweChannelIdET.CLEAR_BATTERY_SETTING, new UnsignedWordElement(45360))), //

				new FC3ReadRegistersTask(45222, Priority.LOW, //
						m(GoodweChannelIdET.PV_E_TOTAL_3, new UnsignedDoublewordElement(45222),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.PV_E_DAY_3, new UnsignedDoublewordElement(45224),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_SELL_3, new UnsignedDoublewordElement(45226),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.H_TOTAL_3, new UnsignedDoublewordElement(45228)), //
						m(GoodweChannelIdET.E_DAY_SELL_3, new UnsignedWordElement(45230),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_BUY_3, new UnsignedDoublewordElement(45231),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_DAY_BUY_3, new UnsignedWordElement(45233),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_TOTAL_LOAD_3, new UnsignedDoublewordElement(45234),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_LOAD_DAY_3, new UnsignedWordElement(45236),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_BATTERY_CHARGE_3, new UnsignedDoublewordElement(45237),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_CHARGE_DAY_3, new UnsignedWordElement(45239),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_BATTERY_DISCHARGE_3, new UnsignedDoublewordElement(45240),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.E_DISCHARGE_DAY_3, new UnsignedWordElement(45242),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.LANGUAGE, new UnsignedWordElement(45243)), //
						m(GoodweChannelIdET.SAFETY_COUNTRY_CODE, new UnsignedWordElement(45244)), //
						m(GoodweChannelIdET.ISO, new UnsignedWordElement(45245)), //
						m(GoodweChannelIdET.LVRT, new UnsignedWordElement(45246)), //
						m(GoodweChannelIdET.ISLANDING, new UnsignedWordElement(45247)), //
						new DummyRegisterElement(45248), //
						m(GoodweChannelIdET.BURN_IN_RESET_TIME, new UnsignedWordElement(45249)), //
						m(GoodweChannelIdET.PV_START_VOLTAGE, new UnsignedWordElement(45250),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.ENABLE_MPPT_4SHADOW, new UnsignedWordElement(45251)), //
						m(GoodweChannelIdET.BACK_UP_ENABLE, new UnsignedWordElement(45252)), //
						m(GoodweChannelIdET.AUTO_START_BACKUP, new UnsignedWordElement(45253)), //
						m(GoodweChannelIdET.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(45254)), //
						m(GoodweChannelIdET.REPAID_CUT_OFF, new UnsignedWordElement(45255)), //
						m(GoodweChannelIdET.BACKUP_START_DLY, new UnsignedWordElement(45256)), //
						m(GoodweChannelIdET.UPS_STD_VOLT_TYPE, new UnsignedWordElement(45257)), //
						m(GoodweChannelIdET.UNDER_ATS, new UnsignedWordElement(45258)), //
						m(GoodweChannelIdET.BURN_IN_MODE, new UnsignedWordElement(45259)), //
						m(GoodweChannelIdET.BACKUP_OVERLOAD_DELAY, new UnsignedWordElement(45260)), //
						m(GoodweChannelIdET.UPSPHASE_TYPE, new UnsignedWordElement(45261)), // .
						new DummyRegisterElement(45262), //
						m(GoodweChannelIdET.DERATE_RATE_VDE, new UnsignedWordElement(45263))), //

				new FC3ReadRegistersTask(45350, Priority.LOW, //
						m(GoodweChannelIdET.LEAD_BAT_CAPACITY, new UnsignedWordElement(45350)), //
						m(GoodweChannelIdET.BATTERY_STRINGS, new UnsignedWordElement(45351)), //
						m(GoodweChannelIdET.BATT_CHARGE_VOLT_MAX, new UnsignedWordElement(45352),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_CHARGE_CURR_MAX, new UnsignedWordElement(45353),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_VOLT_UNDER_MIN, new UnsignedWordElement(45354),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_DISCHARGE_CURR_MAX, new UnsignedWordElement(45355),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_SOC_UNDER_MIN, new UnsignedWordElement(45356)), //
						m(GoodweChannelIdET.BATT_OFF_LINE_VOLT_UNDER_MIN, new UnsignedWordElement(45357),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATT_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(45358))), //

				// Cos-Phi Curve
				new FC16WriteRegistersTask(45433, //
						m(GoodweChannelIdET.ENABLE_CURVE, new UnsignedWordElement(45433)), //
						m(GoodweChannelIdET.POINT_A_VALUE, new UnsignedWordElement(45434)), //
						m(GoodweChannelIdET.POINT_A_PF, new UnsignedWordElement(45435),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.POINT_B_VALUE, new UnsignedWordElement(45436)), //
						m(GoodweChannelIdET.POINT_B_PF, new UnsignedWordElement(45437),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.POINT_C_VALUE, new UnsignedWordElement(45438)), //
						m(GoodweChannelIdET.POINT_C_PF, new UnsignedWordElement(45439),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.LOCK_OUT_POWER, new UnsignedWordElement(45442))), //

				new FC3ReadRegistersTask(45433, Priority.LOW, //
						m(GoodweChannelIdET.ENABLE_CURVE, new UnsignedWordElement(45433)), //
						m(GoodweChannelIdET.POINT_A_VALUE, new UnsignedWordElement(45434)), //
						m(GoodweChannelIdET.POINT_A_PF, new UnsignedWordElement(45435),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.POINT_B_VALUE, new UnsignedWordElement(45436)), //
						m(GoodweChannelIdET.POINT_B_PF, new UnsignedWordElement(45437),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.POINT_C_VALUE, new UnsignedWordElement(45438)), //
						m(GoodweChannelIdET.POINT_C_PF, new UnsignedWordElement(45439),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.LOCK_OUT_POWER, new UnsignedWordElement(45442))), //

				// Power and Frequency curve
				new FC16WriteRegistersTask(45443, //
						m(new BitsWordElement(45443, this) //
								.bit(0, GoodweChannelIdET.STATE_70) //
								.bit(1, GoodweChannelIdET.STATE_71)), //
						m(GoodweChannelIdET.FFROZEN_DCH, new UnsignedWordElement(45444),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FFROZEN_CH, new UnsignedWordElement(45445),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FSTOP_DCH, new UnsignedWordElement(45446),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FSTOP_CH, new UnsignedWordElement(45447),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.RECOVERY_WAITING_TIME, new UnsignedWordElement(45448)), //
						m(GoodweChannelIdET.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.RECOVERY_SLOPE, new UnsignedWordElement(45451)), //
						m(GoodweChannelIdET.FFROZEN_DCH_SLOPE, new UnsignedWordElement(45452),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FFROZEN_CH_SLOPE, new UnsignedWordElement(45453),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.DOWN_SLOPE_POWER_REFERENCE, new UnsignedWordElement(45454)), //
						m(GoodweChannelIdET.DOWN_SLOP, new UnsignedWordElement(45455))), //

				new FC3ReadRegistersTask(45443, Priority.LOW, //
						m(new BitsWordElement(45443, this) //
								.bit(0, GoodweChannelIdET.STATE_70) //
								.bit(1, GoodweChannelIdET.STATE_71)), //
						m(GoodweChannelIdET.FFROZEN_DCH, new UnsignedWordElement(45444),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FFROZEN_CH, new UnsignedWordElement(45445),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FSTOP_DCH, new UnsignedWordElement(45446),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FSTOP_CH, new UnsignedWordElement(45447),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.RECOVERY_WAITING_TIME, new UnsignedWordElement(45448)), //
						m(GoodweChannelIdET.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.RECOVERY_SLOPE, new UnsignedWordElement(45451)), //
						m(GoodweChannelIdET.FFROZEN_DCH_SLOPE, new UnsignedWordElement(45452),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FFROZEN_CH_SLOPE, new UnsignedWordElement(45453),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.DOWN_SLOPE_POWER_REFERENCE, new UnsignedWordElement(45454)), //
						m(GoodweChannelIdET.DOWN_SLOP, new UnsignedWordElement(45455))), //

				// QU Curve
				new FC16WriteRegistersTask(45456, //
						m(GoodweChannelIdET.ENABLE_CURVE_QU, new UnsignedWordElement(45456)), //
						m(GoodweChannelIdET.LOCK_IN_POWER_QU, new UnsignedWordElement(45457)), //
						m(GoodweChannelIdET.LOCK_OUT_POWER_QU, new UnsignedWordElement(45458)), //
						m(GoodweChannelIdET.V1_VOLTAGE, new UnsignedWordElement(45459),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodweChannelIdET.V2_VOLTAGE, new UnsignedWordElement(45461),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodweChannelIdET.V3_VOLTAGE, new UnsignedWordElement(45463),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodweChannelIdET.V4_VOLTAGE, new UnsignedWordElement(45465),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V4_VALUE, new UnsignedWordElement(45466)), //
						m(GoodweChannelIdET.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodweChannelIdET.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodweChannelIdET.MISCELLANEA, new UnsignedWordElement(45469)), //
						m(GoodweChannelIdET.RATED_VOLTAGE, new UnsignedWordElement(45470)), //
						m(GoodweChannelIdET.RESPONSE_TIME, new UnsignedWordElement(45471))), //

				new FC3ReadRegistersTask(45456, Priority.LOW,
						m(GoodweChannelIdET.ENABLE_CURVE_QU, new UnsignedWordElement(45456)), //
						m(GoodweChannelIdET.LOCK_IN_POWER_QU, new UnsignedWordElement(45457)), //
						m(GoodweChannelIdET.LOCK_OUT_POWER_QU, new UnsignedWordElement(45458)), //
						m(GoodweChannelIdET.V1_VOLTAGE, new UnsignedWordElement(45459),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodweChannelIdET.V2_VOLTAGE, new UnsignedWordElement(45461),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodweChannelIdET.V3_VOLTAGE, new UnsignedWordElement(45463),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodweChannelIdET.V4_VOLTAGE, new UnsignedWordElement(45465),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V4_VALUE, new UnsignedWordElement(45466)), //
						m(GoodweChannelIdET.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodweChannelIdET.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodweChannelIdET.MISCELLANEA, new UnsignedWordElement(45469)), //
						m(GoodweChannelIdET.RATED_VOLTAGE, new UnsignedWordElement(45470)), //
						m(GoodweChannelIdET.RESPONSE_TIME, new UnsignedWordElement(45471))), //

				// PU Curve
				new FC16WriteRegistersTask(45472, //
						m(GoodweChannelIdET.PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodweChannelIdET.POWER_CHANGE_RATE, new UnsignedWordElement(45473)), //
						m(GoodweChannelIdET.V1_VOLTAGE_PU, new UnsignedWordElement(45474),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V1_VALUE_PU, new SignedWordElement(45475),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V2_VOLTAGE_PU, new UnsignedWordElement(45476),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V2_VALUE_PU, new SignedWordElement(45477),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V3_VOLTAGE_PU, new UnsignedWordElement(45478),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V3_VALUE_PU, new SignedWordElement(45479),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V4_VOLTAGE_PU, new UnsignedWordElement(45480),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V4_VALUE_PU, new SignedWordElement(45481),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.FIXED_POWER_FACTOR, new UnsignedWordElement(45482),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FIXED_REACTIVE_POWER, new UnsignedWordElement(45483),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.GRID_LIMIT_BY_VOLT_START_VOL, new UnsignedWordElement(45485),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.GRID_LIMIT_BY_VOLT_START_PER, new UnsignedWordElement(45486),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.GRID_LIMIT_BY_VOLT_SLOPE, new UnsignedWordElement(45487),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.AUTO_TEST_ENABLE, new UnsignedWordElement(45488),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.AUTO_TEST_STEP, new UnsignedWordElement(45489),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.UW_ITALY_FREQ_MODE, new UnsignedWordElement(45490))), //

				new FC3ReadRegistersTask(45472, Priority.LOW,
						m(GoodweChannelIdET.PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodweChannelIdET.POWER_CHANGE_RATE, new UnsignedWordElement(45473)), //
						m(GoodweChannelIdET.V1_VOLTAGE_PU, new UnsignedWordElement(45474),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V1_VALUE_PU, new SignedWordElement(45475),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V2_VOLTAGE_PU, new UnsignedWordElement(45476),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V2_VALUE_PU, new SignedWordElement(45477),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V3_VOLTAGE_PU, new UnsignedWordElement(45478),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V3_VALUE_PU, new SignedWordElement(45479),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V4_VOLTAGE_PU, new UnsignedWordElement(45480),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.V4_VALUE_PU, new SignedWordElement(45481),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.FIXED_POWER_FACTOR, new UnsignedWordElement(45482),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.FIXED_REACTIVE_POWER, new UnsignedWordElement(45483),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.GRID_LIMIT_BY_VOLT_START_VOL, new UnsignedWordElement(45485),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.GRID_LIMIT_BY_VOLT_START_PER, new UnsignedWordElement(45486),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(45487, 45489), //
						m(GoodweChannelIdET.UW_ITALY_FREQ_MODE, new UnsignedWordElement(45490))), //

				new FC16WriteRegistersTask(47000, //
						m(GoodweChannelIdET.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(GoodweChannelIdET.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodweChannelIdET.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002)), //
						m(GoodweChannelIdET.SIMULATE_METER_POWER, new UnsignedWordElement(47003)), //
						m(GoodweChannelIdET.BREEZE_ON_OFF, new UnsignedWordElement(47004)), //
						m(GoodweChannelIdET.LOG_DATA_ENABLE, new UnsignedWordElement(47005)), //
						m(GoodweChannelIdET.DATA_SEND_INTERVAL, new UnsignedWordElement(47006))), //

				new FC3ReadRegistersTask(47000, Priority.LOW, //
						m(GoodweChannelIdET.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(GoodweChannelIdET.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodweChannelIdET.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002)), //
						m(GoodweChannelIdET.SIMULATE_METER_POWER, new UnsignedWordElement(47003)), //
						m(GoodweChannelIdET.BREEZE_ON_OFF, new UnsignedWordElement(47004)), //
						m(GoodweChannelIdET.LOG_DATA_ENABLE, new UnsignedWordElement(47005)), //
						m(GoodweChannelIdET.DATA_SEND_INTERVAL, new UnsignedWordElement(47006))), //

				new FC16WriteRegistersTask(47500, //
						m(GoodweChannelIdET.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						m(GoodweChannelIdET.BATTERY_FLOAT_VOLT, new UnsignedWordElement(47501), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATTERY_FLOAT_CURRENT, new UnsignedWordElement(47502),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATTERY_FLOAT_TIME, new UnsignedWordElement(47503)), //
						m(GoodweChannelIdET.BATTERY_TYPE_INDEX_ARM, new UnsignedWordElement(47504)), //
						m(GoodweChannelIdET.MANUFACTURE_CODE, new UnsignedWordElement(47505)),
						m(GoodweChannelIdET.DC_VOLT_OUTPUT, new UnsignedWordElement(47506)), //
						m(GoodweChannelIdET.BAT_AVG_CHG_VOLT, new UnsignedWordElement(47507),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BAT_AVG_CHG_HOURS, new UnsignedWordElement(47508)), //
						m(GoodweChannelIdET.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodweChannelIdET.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(GoodweChannelIdET.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodweChannelIdET.EMS_POWER_SET, new UnsignedWordElement(47512)), //
						m(GoodweChannelIdET.BAT_BMS_CURR_LMT_COFF, new UnsignedWordElement(47513),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.BATTERY_PROTOCOL_ARM, new UnsignedWordElement(47514)), //
						m(GoodweChannelIdET.START_TIME_1, new UnsignedWordElement(47515)), //
						m(GoodweChannelIdET.END_TIME_1, new UnsignedWordElement(47516)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_1, new UnsignedWordElement(47517)), //
						m(new BitsWordElement(47518, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.START_TIME_2, new UnsignedWordElement(47519)), //
						m(GoodweChannelIdET.END_TIME_2, new UnsignedWordElement(47520)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_2, new UnsignedWordElement(47521)), //
						m(new BitsWordElement(47522, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.START_TIME_3, new UnsignedWordElement(47523)), //
						m(GoodweChannelIdET.END_TIME_3, new UnsignedWordElement(47524)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_3, new UnsignedWordElement(47525)), //
						m(new BitsWordElement(47526, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.START_TIME_4, new UnsignedWordElement(47527)), //
						m(GoodweChannelIdET.END_TIME_4, new UnsignedWordElement(47528)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_4, new UnsignedWordElement(47529)), //
						m(new BitsWordElement(47530, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodweChannelIdET.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						m(GoodweChannelIdET.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533))), //

				new FC3ReadRegistersTask(47500, Priority.LOW,
						m(GoodweChannelIdET.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						m(GoodweChannelIdET.BATTERY_FLOAT_VOLT, new UnsignedWordElement(47501),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATTERY_FLOAT_CURRENT, new UnsignedWordElement(47502),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BATTERY_FLOAT_TIME, new UnsignedWordElement(47503)), //
						m(GoodweChannelIdET.BATTERY_TYPE_INDEX_ARM, new UnsignedWordElement(47504)), //
						m(GoodweChannelIdET.MANUFACTURE_CODE, new UnsignedWordElement(47505)),
						m(GoodweChannelIdET.DC_VOLT_OUTPUT, new UnsignedWordElement(47506)), //
						m(GoodweChannelIdET.BAT_AVG_CHG_VOLT, new UnsignedWordElement(47507),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelIdET.BAT_AVG_CHG_HOURS, new UnsignedWordElement(47508)), //
						m(GoodweChannelIdET.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodweChannelIdET.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(GoodweChannelIdET.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodweChannelIdET.EMS_POWER_SET, new UnsignedWordElement(47512)), //
						m(GoodweChannelIdET.BAT_BMS_CURR_LMT_COFF, new UnsignedWordElement(47513),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodweChannelIdET.BATTERY_PROTOCOL_ARM, new UnsignedWordElement(47514)), //
						m(GoodweChannelIdET.START_TIME_1, new UnsignedWordElement(47515)), //
						m(GoodweChannelIdET.END_TIME_1, new UnsignedWordElement(47516)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_1, new UnsignedWordElement(47517)), //
						m(new BitsWordElement(47518, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.START_TIME_2, new UnsignedWordElement(47519)), //
						m(GoodweChannelIdET.END_TIME_2, new UnsignedWordElement(47520)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_2, new UnsignedWordElement(47521)), //
						m(new BitsWordElement(47522, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.START_TIME_3, new UnsignedWordElement(47523)), //
						m(GoodweChannelIdET.END_TIME_3, new UnsignedWordElement(47524)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_3, new UnsignedWordElement(47525)), //
						m(new BitsWordElement(47526, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.START_TIME_4, new UnsignedWordElement(47527)), //
						m(GoodweChannelIdET.END_TIME_4, new UnsignedWordElement(47528)), //
						m(GoodweChannelIdET.BAT_POWER_PERCENT_4, new UnsignedWordElement(47529)), //
						m(new BitsWordElement(47530, this) //
								.bit(0, GoodweChannelIdET.STATE_72) //
								.bit(1, GoodweChannelIdET.STATE_73) //
								.bit(2, GoodweChannelIdET.STATE_74) //
								.bit(3, GoodweChannelIdET.STATE_75) //
								.bit(4, GoodweChannelIdET.STATE_76) //
								.bit(5, GoodweChannelIdET.STATE_77) //
								.bit(6, GoodweChannelIdET.STATE_78)), //
						m(GoodweChannelIdET.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodweChannelIdET.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						m(GoodweChannelIdET.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533))), //

				new FC16WriteRegistersTask(47900, //
						m(GoodweChannelIdET.BMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodweChannelIdET.BATT_STRINGS_RS485, new UnsignedWordElement(47901)), //
						m(GoodweChannelIdET.WBMS_BAT_CHARGE_VMAX, new UnsignedWordElement(47902)), //
						m(GoodweChannelIdET.WBMS_BAT_CHARGE_IMAX, new UnsignedWordElement(47903)), //
						m(GoodweChannelIdET.WBMS_BAT_DISCHARGE_VMIN, new UnsignedWordElement(47904)), //
						m(GoodweChannelIdET.WBMS_BAT_DISCHARGE_IMAX, new UnsignedWordElement(47905)), //
						m(GoodweChannelIdET.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906)), //
						m(GoodweChannelIdET.WBMS_BAT_CURRENT, new UnsignedWordElement(47907)), //
						m(GoodweChannelIdET.WBMS_BAT_SOC, new UnsignedWordElement(47908)), //
						m(GoodweChannelIdET.WBMS_BAT_SOH, new UnsignedWordElement(47909)), //
						m(GoodweChannelIdET.WBMS_BAT_TEMPERATURE, new UnsignedWordElement(47910)), //
						m(new BitsWordElement(47911, this) //
								.bit(0, GoodweChannelIdET.STATE_42) //
								.bit(1, GoodweChannelIdET.STATE_43) //
								.bit(2, GoodweChannelIdET.STATE_44) //
								.bit(3, GoodweChannelIdET.STATE_45) //
								.bit(4, GoodweChannelIdET.STATE_46) //
								.bit(5, GoodweChannelIdET.STATE_47) //
								.bit(6, GoodweChannelIdET.STATE_48) //
								.bit(7, GoodweChannelIdET.STATE_49) //
								.bit(8, GoodweChannelIdET.STATE_50) //
								.bit(9, GoodweChannelIdET.STATE_51) //
								.bit(10, GoodweChannelIdET.STATE_52) //
								.bit(11, GoodweChannelIdET.STATE_53) //
								.bit(12, GoodweChannelIdET.STATE_54) //
								.bit(13, GoodweChannelIdET.STATE_55) //
								.bit(14, GoodweChannelIdET.STATE_56) //
								.bit(15, GoodweChannelIdET.STATE_57)), //
						m(new BitsWordElement(47912, this) //
								.bit(0, GoodweChannelIdET.STATE_58) //
								.bit(1, GoodweChannelIdET.STATE_59) //
								.bit(2, GoodweChannelIdET.STATE_60) //
								.bit(3, GoodweChannelIdET.STATE_61) //
								.bit(4, GoodweChannelIdET.STATE_62) //
								.bit(5, GoodweChannelIdET.STATE_63) //
								.bit(6, GoodweChannelIdET.STATE_64) //
								.bit(7, GoodweChannelIdET.STATE_65) //
								.bit(8, GoodweChannelIdET.STATE_66) //
								.bit(9, GoodweChannelIdET.STATE_67) //
								.bit(10, GoodweChannelIdET.STATE_68) //
								.bit(11, GoodweChannelIdET.STATE_69)), //
						m(new BitsWordElement(47913, this) //
								.bit(0, GoodweChannelIdET.STATE_79) //
								.bit(1, GoodweChannelIdET.STATE_80) //
								.bit(2, GoodweChannelIdET.STATE_81)))); //
	}

	@Override
	public void setCharger(GoodweETChargerPV1 charger) {
		this.charger = charger;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

}
