package io.openems.edge.goodwe.common;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.goodwe.charger.AbstractGoodWeEtCharger;
import io.openems.edge.goodwe.common.enums.BatteryMode;
import io.openems.edge.goodwe.common.enums.GoodweType;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public abstract class AbstractGoodWe extends AbstractOpenemsModbusComponent
		implements GoodWe, OpenemsComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(AbstractGoodWe.class);

	private final io.openems.edge.common.channel.ChannelId activePowerChannelId;
	private final io.openems.edge.common.channel.ChannelId dcDischargePowerChannelId;
	private final CalculateEnergyFromPower calculateAcChargeEnergy;
	private final CalculateEnergyFromPower calculateAcDischargeEnergy;
	private final CalculateEnergyFromPower calculateDcChargeEnergy;
	private final CalculateEnergyFromPower calculateDcDischargeEnergy;

	protected final Set<AbstractGoodWeEtCharger> chargers = new HashSet<>();

	protected AbstractGoodWe(//
			io.openems.edge.common.channel.ChannelId activePowerChannelId, //
			io.openems.edge.common.channel.ChannelId dcDischargePowerChannelId, //
			io.openems.edge.common.channel.ChannelId activeChargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId activeDischargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId dcChargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId dcDischargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsNamedException {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.activePowerChannelId = activePowerChannelId;
		this.dcDischargePowerChannelId = dcDischargePowerChannelId;
		this.calculateAcChargeEnergy = new CalculateEnergyFromPower(this, activeChargeEnergyChannelId);
		this.calculateAcDischargeEnergy = new CalculateEnergyFromPower(this, activeDischargeEnergyChannelId);
		this.calculateDcChargeEnergy = new CalculateEnergyFromPower(this, dcChargeEnergyChannelId);
		this.calculateDcDischargeEnergy = new CalculateEnergyFromPower(this, dcDischargeEnergyChannelId);
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(35001, Priority.LOW, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(35001)), //
						new DummyRegisterElement(35002), //
						m(GoodWe.ChannelId.SERIAL_NUMBER, new StringWordElement(35003, 8)), //
						m(GoodWe.ChannelId.GOODWE_TYPE, new StringWordElement(35011, 5), new ElementToChannelConverter(
								// element -> channel
								value -> {
									// Evaluate GoodweType
									final GoodweType result;
									if (value == null) {
										result = GoodweType.UNDEFINED;
									} else {
										String stringValue = TypeUtils.<String>getAsType(OpenemsType.STRING, value);
										switch (stringValue) {
										case "GW10K-BT":
											result = GoodweType.GOODWE_10K_BT;
											break;
										case "GW8K-BT":
											result = GoodweType.GOODWE_8K_BT;
											break;
										case "GW5K-BT":
											result = GoodweType.GOODWE_5K_BT;
											break;
										case "GW10K-ET":
											result = GoodweType.GOODWE_10K_ET;
											break;
										case "GW8K-ET":
											result = GoodweType.GOODWE_8K_ET;
											break;
										case "GW5K-ET":
											result = GoodweType.GOODWE_5K_ET;
											break;
										default:
											this.logInfo(this.log, "Unable to identify GoodWe by name [" + value + "]");
											result = GoodweType.UNDEFINED;
											break;
										}
									}
									// Log on first occurrence
									if (result != this.getGoodweType()) {
										switch (result) {
										case GOODWE_10K_BT:
										case GOODWE_8K_BT:
										case GOODWE_5K_BT:
										case GOODWE_10K_ET:
										case GOODWE_8K_ET:
										case GOODWE_5K_ET:
											this.logInfo(this.log, "Identified " + result.getName());
											break;
										case UNDEFINED:
											break;
										}
									}
									return result;
								}, //

								// channel -> element
								value -> value)),

						m(GoodWe.ChannelId.DSP1_SOFTWARE_VERSION, new UnsignedWordElement(35016)), //
						m(GoodWe.ChannelId.DSP2_SOFTWARE_VERSION, new UnsignedWordElement(35017)), //
						m(GoodWe.ChannelId.DSP_SPN_VERSION, new UnsignedWordElement(35018)), //
						m(GoodWe.ChannelId.ARM_SOFTWARE_VERSION, new UnsignedWordElement(35019)), //
						m(GoodWe.ChannelId.ARM_SVN_VERSION, new UnsignedWordElement(35020)), //
						m(GoodWe.ChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(35021)), //
						new DummyRegisterElement(35022, 35026), //
						m(GoodWe.ChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(35027)), //
						new DummyRegisterElement(35028, 35049), //
						m(GoodWe.ChannelId.SIMCCID, new UnsignedWordElement(35050))), //
				new FC3ReadRegistersTask(35103, Priority.LOW, //
						m(GoodWe.ChannelId.V_PV1, new UnsignedWordElement(35103),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV1, new UnsignedWordElement(35104),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.P_PV1, new UnsignedDoublewordElement(35105),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V_PV2, new UnsignedWordElement(35107),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV2, new UnsignedWordElement(35108),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.P_PV2, new UnsignedDoublewordElement(35109),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V_PV3, new UnsignedWordElement(35111),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV3, new UnsignedWordElement(35112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.P_PV3, new UnsignedDoublewordElement(35113),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V_PV4, new UnsignedWordElement(35115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV4, new UnsignedWordElement(35116),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.P_PV4, new UnsignedDoublewordElement(35117),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.PV_MODE, new UnsignedDoublewordElement(35119)), //
						m(GoodWe.ChannelId.VGRID_R, new UnsignedWordElement(35121),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.IGRID_R, new UnsignedWordElement(35122),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FGRID_R, new UnsignedWordElement(35123),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35124), //
						m(GoodWe.ChannelId.PGRID_R, new SignedWordElement(35125)), //
						m(GoodWe.ChannelId.VGRID_S, new UnsignedWordElement(35126),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.IGRID_S, new UnsignedWordElement(35127),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FGRID_S, new UnsignedWordElement(35128),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35129), //
						m(GoodWe.ChannelId.PGRID_S, new SignedWordElement(35130)), //
						m(GoodWe.ChannelId.VGRID_T, new UnsignedWordElement(35131),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.IGRID_T, new UnsignedWordElement(35132),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FGRID_T, new UnsignedWordElement(35133),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35134), //
						m(GoodWe.ChannelId.PGRID_T, new SignedWordElement(35135)), //
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
						m(GoodWe.ChannelId.TOTAL_INV_POWER, new SignedWordElement(35138)), //
						new DummyRegisterElement(35139), //
						m(GoodWe.ChannelId.AC_ACTIVE_POWER, new SignedWordElement(35140), //
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35141), //
						m(GoodWe.ChannelId.AC_REACTIVE_POWER, new SignedWordElement(35142), //
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35143), //
						m(GoodWe.ChannelId.AC_APPARENT_POWER, new SignedWordElement(35144), //
								ElementToChannelConverter.INVERT), //
						m(GoodWe.ChannelId.BACK_UP_V_LOAD_R, new UnsignedWordElement(35145), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BACK_UP_I_LOAD_R, new UnsignedWordElement(35146),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BACK_UP_F_LOAD_R, new UnsignedWordElement(35147),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LOAD_MODE_R, new UnsignedWordElement(35148)), //
						new DummyRegisterElement(35149), //
						m(GoodWe.ChannelId.BACK_UP_P_LOAD_R, new SignedWordElement(35150)), //
						m(GoodWe.ChannelId.BACK_UP_V_LOAD_S, new UnsignedWordElement(35151),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BACK_UP_I_LOAD_S, new UnsignedWordElement(35152),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BACK_UP_F_LOAD_S, new UnsignedWordElement(35153),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LOAD_MODE_S, new UnsignedWordElement(35154)), //
						new DummyRegisterElement(35155), //
						m(GoodWe.ChannelId.BACK_UP_P_LOAD_S, new SignedWordElement(35156)), //
						m(GoodWe.ChannelId.BACK_UP_V_LOAD_T, new UnsignedWordElement(35157),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BACK_UP_I_LOAD_T, new UnsignedWordElement(35158),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BACK_UP_F_LOAD_T, new UnsignedWordElement(35159),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LOAD_MODE_T, new UnsignedWordElement(35160)), //
						new DummyRegisterElement(35161), //
						m(GoodWe.ChannelId.BACK_UP_P_LOAD_T, new SignedWordElement(35162)), //
						new DummyRegisterElement(35163), //
						m(GoodWe.ChannelId.P_LOAD_R, new SignedWordElement(35164)), //
						new DummyRegisterElement(35165), //
						m(GoodWe.ChannelId.P_LOAD_S, new SignedWordElement(35166)), //
						new DummyRegisterElement(35167), //
						m(GoodWe.ChannelId.P_LOAD_T, new SignedWordElement(35168)), //
						new DummyRegisterElement(35169), //
						m(GoodWe.ChannelId.TOTAL_BACK_UP_LOAD, new SignedWordElement(35170)), //
						new DummyRegisterElement(35171), //
						m(GoodWe.ChannelId.TOTAL_LOAD_POWER, new SignedWordElement(35172)), //
						m(GoodWe.ChannelId.UPS_LOAD_PERCENT, new UnsignedWordElement(35173),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.AIR_TEMPERATURE, new SignedWordElement(35174),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MODULE_TEMPERATURE, new SignedWordElement(35175),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.RADIATOR_TEMPERATURE, new SignedWordElement(35176),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FUNCTION_BIT_VALUE, new UnsignedWordElement(35177)), //
						m(GoodWe.ChannelId.BUS_VOLTAGE, new UnsignedWordElement(35178),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.NBUS_VOLTAGE, new UnsignedWordElement(35179),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(35180, Priority.HIGH, //
						m(GoodWe.ChannelId.V_BATTERY1, new UnsignedWordElement(35180),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_BATTERY1, new SignedWordElement(35181),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35182), //
						// required for calculation of ActivePower
						m(GoodWe.ChannelId.P_BATTERY1, new SignedWordElement(35183)),
						m(GoodWe.ChannelId.BATTERY_MODE, new UnsignedWordElement(35184))), //

				new FC3ReadRegistersTask(35185, Priority.LOW, //
						m(GoodWe.ChannelId.WARNING_CODE, new UnsignedWordElement(35185)), //
						m(GoodWe.ChannelId.SAFETY_COUNTRY, new UnsignedWordElement(35186)), //
						m(GoodWe.ChannelId.WORK_MODE_2, new UnsignedWordElement(35187)), //
						m(GoodWe.ChannelId.OPERATION_MODE, new UnsignedWordElement(35188)), //
						m(new BitsWordElement(35189, this) //
								.bit(0, GoodWe.ChannelId.STATE_0) //
								.bit(1, GoodWe.ChannelId.STATE_1) //
								.bit(2, GoodWe.ChannelId.STATE_2) //
								.bit(3, GoodWe.ChannelId.STATE_3) //
								.bit(4, GoodWe.ChannelId.STATE_4) //
								.bit(5, GoodWe.ChannelId.STATE_5) //
								.bit(6, GoodWe.ChannelId.STATE_6) //
								.bit(7, GoodWe.ChannelId.STATE_7) //
								.bit(8, GoodWe.ChannelId.STATE_8) //
								.bit(9, GoodWe.ChannelId.STATE_9) //
								.bit(10, GoodWe.ChannelId.STATE_10) //
								.bit(11, GoodWe.ChannelId.STATE_11) //
								.bit(12, GoodWe.ChannelId.STATE_12) //
								.bit(13, GoodWe.ChannelId.STATE_13)//
								.bit(14, GoodWe.ChannelId.STATE_14)//
								.bit(15, GoodWe.ChannelId.STATE_15)//
						), //
						m(new BitsWordElement(35190, this) //
								.bit(0, GoodWe.ChannelId.STATE_16) //
								.bit(1, GoodWe.ChannelId.STATE_17) //
								.bit(2, GoodWe.ChannelId.STATE_18) //
								.bit(3, GoodWe.ChannelId.STATE_19) //
								.bit(4, GoodWe.ChannelId.STATE_20) //
								.bit(5, GoodWe.ChannelId.STATE_21) //
								.bit(6, GoodWe.ChannelId.STATE_22) //
								.bit(7, GoodWe.ChannelId.STATE_23) //
								.bit(8, GoodWe.ChannelId.STATE_24) //
								.bit(9, GoodWe.ChannelId.STATE_25) //
								.bit(10, GoodWe.ChannelId.STATE_26) //
								.bit(11, GoodWe.ChannelId.STATE_27) //
								.bit(12, GoodWe.ChannelId.STATE_28) //
								.bit(13, GoodWe.ChannelId.STATE_29) //
								.bit(14, GoodWe.ChannelId.STATE_30) //
								.bit(15, GoodWe.ChannelId.STATE_31) //
						), //
						m(GoodWe.ChannelId.PV_E_TOTAL, new UnsignedDoublewordElement(35191),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.PV_E_DAY, new UnsignedDoublewordElement(35193),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL, new UnsignedDoublewordElement(35195),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.H_TOTAL, new UnsignedDoublewordElement(35197)), //
						m(GoodWe.ChannelId.E_DAY_SELL, new UnsignedWordElement(35199),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY, new UnsignedDoublewordElement(35200),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DAY_BUY, new UnsignedWordElement(35202),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_LOAD, new UnsignedDoublewordElement(35203),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_LOAD_DAY, new UnsignedWordElement(35205),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(this.getDcChargeEnergyChannel(), new UnsignedDoublewordElement(35206), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(GoodWe.ChannelId.E_CHARGE_DAY, new UnsignedWordElement(35208),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(this.getDcDischargeEnergyChannel(), new UnsignedDoublewordElement(35209),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(GoodWe.ChannelId.E_DISCHARGE_DAY, new UnsignedWordElement(35211),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BATTERY_STRINGS, new UnsignedWordElement(35212)), //
						m(GoodWe.ChannelId.CPLD_WARNING_CODE, new UnsignedWordElement(35213)), //
						m(GoodWe.ChannelId.W_CHARGER_CTRL_FLAG, new UnsignedWordElement(35214)), //
						m(GoodWe.ChannelId.DERATE_FLAG, new UnsignedWordElement(35215)), //
						m(GoodWe.ChannelId.DERATE_FROZEN_POWER, new UnsignedDoublewordElement(35216)), //
						m(GoodWe.ChannelId.DIAG_STATUS_H, new UnsignedDoublewordElement(35218)), //
						m(GoodWe.ChannelId.DIAG_STATUS_L, new UnsignedDoublewordElement(35220))), //

				// External Communication Data(ARM)
				new FC3ReadRegistersTask(36000, Priority.LOW, //
						m(GoodWe.ChannelId.COM_MODE, new UnsignedWordElement(36000)), //
						m(GoodWe.ChannelId.RSSI, new UnsignedWordElement(36001)), //
						m(GoodWe.ChannelId.MANUFACTURE_CODE, new UnsignedWordElement(36002)), //
						m(GoodWe.ChannelId.B_METER_COMMUNICATE_STATUS, new UnsignedWordElement(36003)), //
						m(GoodWe.ChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004)), //
						m(GoodWe.ChannelId.MT_ACTIVE_POWER_R, new SignedWordElement(36005)), //
						m(GoodWe.ChannelId.MT_ACTIVE_POWER_S, new SignedWordElement(36006)), //
						m(GoodWe.ChannelId.MT_ACTIVE_POWER_T, new SignedWordElement(36007)), //
						m(GoodWe.ChannelId.MT_TOTAL_ACTIVE_POWER, new SignedWordElement(36008)), //
						m(GoodWe.ChannelId.MT_TOTAL_REACTIVE_POWER, new UnsignedWordElement(36009)), //
						m(GoodWe.ChannelId.METER_PF_R, new UnsignedWordElement(36010),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.METER_PF_S, new UnsignedWordElement(36011),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.METER_PF_T, new UnsignedWordElement(36012),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.METER_POWER_FACTOR, new UnsignedWordElement(36013),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.METER_FREQUENCE, new UnsignedWordElement(36014),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.E_TOTAL_SELL, new FloatDoublewordElement(36015),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY_F, new FloatDoublewordElement(36017),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MT_ACTIVE_POWER_R_2, new SignedDoublewordElement(36019)), //
						m(GoodWe.ChannelId.MT_ACTIVE_POWER_S_2, new SignedDoublewordElement(36021)), //
						m(GoodWe.ChannelId.MT_ACTIVE_POWER_T_2, new SignedDoublewordElement(36023)), //
						m(GoodWe.ChannelId.MT_TOTAL_ACTIVE_POWER_2, new SignedDoublewordElement(36025)), //
						m(GoodWe.ChannelId.MT_REACTIVE_POWER_R, new SignedDoublewordElement(36027)), //
						m(GoodWe.ChannelId.MT_REACTIVE_POWER_S, new SignedDoublewordElement(36029)), //
						m(GoodWe.ChannelId.MT_REACTIVE_POWER_T, new SignedDoublewordElement(36031)), //
						m(GoodWe.ChannelId.MT_TOTAL_REACTIVE_POWER_2, new SignedDoublewordElement(36033)), //
						m(GoodWe.ChannelId.MT_APPARENT_POWER_R, new SignedDoublewordElement(36035)), //
						m(GoodWe.ChannelId.MT_APPARENT_POWER_S, new SignedDoublewordElement(36037)), //
						m(GoodWe.ChannelId.MT_APPARENT_POWER_T, new SignedDoublewordElement(36039)), //
						m(GoodWe.ChannelId.MT_TOTAL_APPARENT_POWER, new SignedDoublewordElement(36041)), //
						m(GoodWe.ChannelId.METER_TYPE, new UnsignedWordElement(36043)), //
						m(GoodWe.ChannelId.METER_SOFTWARE_VERSION, new UnsignedWordElement(36044))), //

				// TODO Prioority.ONCE ? !!!
				// Flash Information
				new FC3ReadRegistersTask(36900, Priority.ONCE,
						m(GoodWe.ChannelId.FLASH_PGM_PARA_VER, new UnsignedWordElement(36900)), //
						m(GoodWe.ChannelId.FLASH_PGM_WRITE_COUNT, new UnsignedDoublewordElement(36901)), //
						m(GoodWe.ChannelId.FLASH_SYS_PARA_VER, new UnsignedWordElement(36903)), //
						m(GoodWe.ChannelId.FLASH_SYS_WRITE_COUNT, new UnsignedDoublewordElement(36904)), //
						m(GoodWe.ChannelId.FLASH_BAT_PARA_VER, new UnsignedWordElement(36906)), //
						m(GoodWe.ChannelId.FLASH_BAT_WRITE_COUNT, new UnsignedDoublewordElement(36907)), //
						m(GoodWe.ChannelId.FLASH_EEPROM_PARA_VER, new UnsignedWordElement(36909)), //
						m(GoodWe.ChannelId.FLASH_EEPROM_WRITE_COUNT, new UnsignedDoublewordElement(36910)), //
						m(GoodWe.ChannelId.WIFI_DATA_SEND_COUNT, new UnsignedWordElement(36912)), //
						m(GoodWe.ChannelId.WIFI_UP_DATA_DEBUG, new UnsignedWordElement(36913))), //

				new FC3ReadRegistersTask(37001, Priority.HIGH,
						m(GoodWe.ChannelId.BATTERY_TYPE_INDEX, new UnsignedWordElement(37001)), //
						m(GoodWe.ChannelId.BMS_STATUS, new UnsignedWordElement(37002)), //
						m(GoodWe.ChannelId.BMS_PACK_TEMPERATURE, new UnsignedWordElement(37003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_CHARGE_IMAX, new UnsignedWordElement(37004)), //
						m(GoodWe.ChannelId.BMS_DISCHARGE_IMAX, new UnsignedWordElement(37005)), //
						new DummyRegisterElement(37006), //
						this.getSocModbusElement(37007), //
						m(GoodWe.ChannelId.BMS_SOH, new UnsignedWordElement(37008)), //
						m(GoodWe.ChannelId.BMS_BATTERY_STRINGS, new UnsignedWordElement(37009))), //
				new FC3ReadRegistersTask(37000, Priority.LOW, //
						m(new BitsWordElement(37000, this) //
								.bit(0, GoodWe.ChannelId.DRM0)//
								.bit(1, GoodWe.ChannelId.DRM1)//
								.bit(2, GoodWe.ChannelId.DRM2)//
								.bit(3, GoodWe.ChannelId.DRM3)//
								.bit(4, GoodWe.ChannelId.DRM4)//
								.bit(5, GoodWe.ChannelId.DRM5)//
								.bit(6, GoodWe.ChannelId.DRM6)//
								.bit(7, GoodWe.ChannelId.DRM7)//
								.bit(8, GoodWe.ChannelId.DRM8)//
								.bit(15, GoodWe.ChannelId.DRED_CONNECT)//
						), //
						m(GoodWe.ChannelId.BATTERY_TYPE_INDEX, new UnsignedWordElement(37001)), //
						m(GoodWe.ChannelId.BMS_STATUS, new UnsignedWordElement(37002)), //
						m(GoodWe.ChannelId.BMS_PACK_TEMPERATURE, new UnsignedWordElement(37003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_CHARGE_IMAX, new UnsignedWordElement(37004)), //
						m(GoodWe.ChannelId.BMS_DISCHARGE_IMAX, new UnsignedWordElement(37005)), //
						m(new BitsWordElement(37006, this) //
								.bit(0, GoodWe.ChannelId.STATE_42)//
								.bit(1, GoodWe.ChannelId.STATE_43)//
								.bit(2, GoodWe.ChannelId.STATE_44)//
								.bit(3, GoodWe.ChannelId.STATE_45)//
								.bit(4, GoodWe.ChannelId.STATE_46)//
								.bit(5, GoodWe.ChannelId.STATE_47)//
								.bit(6, GoodWe.ChannelId.STATE_48)//
								.bit(7, GoodWe.ChannelId.STATE_49)//
								.bit(8, GoodWe.ChannelId.STATE_50)//
								.bit(9, GoodWe.ChannelId.STATE_51)//
								.bit(10, GoodWe.ChannelId.STATE_52)//
								.bit(11, GoodWe.ChannelId.STATE_53)//
								.bit(12, GoodWe.ChannelId.STATE_54)//
								.bit(13, GoodWe.ChannelId.STATE_55)//
								.bit(14, GoodWe.ChannelId.STATE_56)//
								.bit(15, GoodWe.ChannelId.STATE_57)//
						), //
						m(GoodWe.ChannelId.BMS_SOC, new UnsignedWordElement(37007)), //
						m(GoodWe.ChannelId.BMS_SOH, new UnsignedWordElement(37008)), //
						m(GoodWe.ChannelId.BMS_BATTERY_STRINGS, new UnsignedWordElement(37009)), //
						m(new BitsWordElement(37010, this) //
								.bit(0, GoodWe.ChannelId.STATE_58)//
								.bit(1, GoodWe.ChannelId.STATE_59)//
								.bit(2, GoodWe.ChannelId.STATE_60)//
								.bit(3, GoodWe.ChannelId.STATE_61)//
								.bit(4, GoodWe.ChannelId.STATE_62)//
								.bit(5, GoodWe.ChannelId.STATE_63)//
								.bit(6, GoodWe.ChannelId.STATE_64)//
								.bit(7, GoodWe.ChannelId.STATE_65)//
								.bit(8, GoodWe.ChannelId.STATE_66)//
								.bit(9, GoodWe.ChannelId.STATE_67)//
								.bit(10, GoodWe.ChannelId.STATE_68)//
								.bit(11, GoodWe.ChannelId.STATE_69)//
						), //
						m(GoodWe.ChannelId.BATTERY_PROTOCOL, new UnsignedWordElement(37011)), //
						// TODO BMS_ERROR_CODE_H register 37012 Table 8-7 BMS Alarm Code bits Bit16-31
						// are reserved
						// TODO Same for BMS_WARNING_CODE_H Table 8-8
						new DummyRegisterElement(37012, 37013), //
						m(GoodWe.ChannelId.BMS_SOFTWARE_VERSION, new UnsignedWordElement(37014)), //
						m(GoodWe.ChannelId.BATTERY_HARDWARE_VERSION, new UnsignedWordElement(37015)), //
						m(GoodWe.ChannelId.MAXIMUM_CELL_TEMPERATURE_ID, new UnsignedWordElement(37016)), //
						m(GoodWe.ChannelId.MINIMUM_CELL_TEMPERATURE_ID, new UnsignedWordElement(37017)), //
						m(GoodWe.ChannelId.MAXIMUM_CELL_VOLTAGE_ID, new UnsignedWordElement(37018)), //
						m(GoodWe.ChannelId.MINIMUM_CELL_VOLTAGE_ID, new UnsignedWordElement(37019)), //
						m(GoodWe.ChannelId.MAXIMUM_CELL_TEMPERATURE, new UnsignedWordElement(37020),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MINIMUM_CELL_TEMPERATURE, new UnsignedWordElement(37021),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MAXIMUM_CELL_VOLTAGE, new UnsignedWordElement(37022)), //
						m(GoodWe.ChannelId.MINIMUM_CELL_VOLTAGE, new UnsignedWordElement(37023)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_1, new UnsignedWordElement(37024)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_2, new UnsignedWordElement(37025)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_3, new UnsignedWordElement(37026)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_4, new UnsignedWordElement(37027)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_5, new UnsignedWordElement(37028)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_6, new UnsignedWordElement(37029)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_7, new UnsignedWordElement(37030)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_8, new UnsignedWordElement(37031)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_9, new UnsignedWordElement(37032)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_10, new UnsignedWordElement(37033)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_11, new UnsignedWordElement(37034)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_12, new UnsignedWordElement(37035)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_13, new UnsignedWordElement(37036)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_14, new UnsignedWordElement(37037)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_15, new UnsignedWordElement(37038)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_16, new UnsignedWordElement(37039)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_17, new UnsignedWordElement(37040)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_18, new UnsignedWordElement(37041)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_19, new UnsignedWordElement(37042)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_20, new UnsignedWordElement(37043)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_21, new UnsignedWordElement(37044)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_22, new UnsignedWordElement(37045)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_23, new UnsignedWordElement(37046)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_24, new UnsignedWordElement(37047)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_25, new UnsignedWordElement(37048)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_26, new UnsignedWordElement(37049)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_27, new UnsignedWordElement(37050)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_28, new UnsignedWordElement(37051)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_29, new UnsignedWordElement(37052)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_30, new UnsignedWordElement(37053)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_31, new UnsignedWordElement(37054)), //
						m(GoodWe.ChannelId.PASS_INFORMATION_32, new UnsignedWordElement(37055))), //

				// Read Error for all registers in range 37100-37149
				// m(GoodWe.ChannelId.BMS_FLAG, new UnsignedWordElement(37100)), //
				// m(GoodWe.ChannelId.BMS_WORK_MODE, new UnsignedWordElement(37101)), //
				// m(GoodWe.ChannelId.BMS_ALLOW_CHARGE_POWER, new
				// UnsignedDoublewordElement(37102)), //
				// m(GoodWe.ChannelId.BMS_ALLOW_DISCHARGE_POWER, new
				// UnsignedDoublewordElement(37104)), //
				// m(GoodWe.ChannelId.BMS_RELAY_STATUS, new UnsignedWordElement(37106)), //
				// m(GoodWe.ChannelId.BATTERY_MODULE_NUMBER, new UnsignedWordElement(37107)), //
				// m(GoodWe.ChannelId.BMS_SHUTDOWN_FAULT_CODE, new UnsignedWordElement(37108)),
				// //
				// m(GoodWe.ChannelId.BATTERY_READY_ENABLE, new UnsignedWordElement(37109)), //
				// m(GoodWe.ChannelId.ALARM_UNDER_TEMPERATURE_ID, new
				// UnsignedWordElement(37110)), //
				// m(GoodWe.ChannelId.ALARM_OVER_TEMPERATURE_ID, new
				// UnsignedWordElement(37111)), //
				// m(GoodWe.ChannelId.ALARM_DIFFER_TEMPERATURE_ID, new
				// UnsignedWordElement(37112)), //
				// m(GoodWe.ChannelId.ALARM_CHARGE_CURRENT_ID, new UnsignedWordElement(37113)),
				// //
				// m(GoodWe.ChannelId.ALARM_DISCHARGE_CURRENT_ID, new
				// UnsignedWordElement(37114)), //
				// m(GoodWe.ChannelId.ALARM_CELL_OVER_VOLTAGE_ID, new
				// UnsignedWordElement(37115)), //
				// m(GoodWe.ChannelId.ALARM_CELL_UNDER_VOLTAGE_ID, new
				// UnsignedWordElement(37116)), //
				// m(GoodWe.ChannelId.ALARM_SOC_LOWER_ID, new UnsignedWordElement(37117)), //
				// m(GoodWe.ChannelId.ALARM_CELL_VOLTAGE_DIFFER_ID, new
				// UnsignedWordElement(37118)), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_1, new SignedWordElement(37119),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_2, new SignedWordElement(37120),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_3, new SignedWordElement(37121),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_4, new SignedWordElement(37122),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_5, new SignedWordElement(37123),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_6, new SignedWordElement(37124),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_7, new SignedWordElement(37125),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_CURRENT_8, new SignedWordElement(37126),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.BATTERY_1_SOC, new UnsignedWordElement(37127)), //
				// m(GoodWe.ChannelId.BATTERY_2_SOC, new UnsignedWordElement(37128)), //
				// m(GoodWe.ChannelId.BATTERY_3_SOC, new UnsignedWordElement(37129)), //
				// m(GoodWe.ChannelId.BATTERY_4_SOC, new UnsignedWordElement(37130)), //
				// m(GoodWe.ChannelId.BATTERY_5_SOC, new UnsignedWordElement(37131)), //
				// m(GoodWe.ChannelId.BATTERY_6_SOC, new UnsignedWordElement(37132)), //
				// m(GoodWe.ChannelId.BATTERY_7_SOC, new UnsignedWordElement(37133)), //
				// m(GoodWe.ChannelId.BATTERY_8_SOC, new UnsignedWordElement(37134)), //
				// m(GoodWe.ChannelId.BATTERY_1_SN, new UnsignedDoublewordElement(37135)), //
				// m(GoodWe.ChannelId.BATTERY_2_SN, new UnsignedDoublewordElement(37137)), //
				// m(GoodWe.ChannelId.BATTERY_3_SN, new UnsignedDoublewordElement(37139)), //
				// m(GoodWe.ChannelId.BATTERY_4_SN, new UnsignedDoublewordElement(37141)), //
				// m(GoodWe.ChannelId.BATTERY_5_SN, new UnsignedDoublewordElement(37143)), //
				// m(GoodWe.ChannelId.BATTERY_6_SN, new UnsignedDoublewordElement(37145)), //
				// m(GoodWe.ChannelId.BATTERY_7_SN, new UnsignedDoublewordElement(37147)), //
				// m(GoodWe.ChannelId.BATTERY_8_SN, new UnsignedDoublewordElement(37149))), //

				new FC3ReadRegistersTask(38000, Priority.LOW, //
						m(GoodWe.ChannelId.WORK_MODE, new UnsignedWordElement(38000)), //
						m(GoodWe.ChannelId.ERROR_MESSAGE_H, new UnsignedWordElement(38001)), //
						m(GoodWe.ChannelId.ERROR_MESSAGE_L, new UnsignedWordElement(38002)), //
						m(GoodWe.ChannelId.SIM_VOLTAGE, new UnsignedWordElement(38003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.SIM_FREQUENCY, new UnsignedWordElement(38004),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.TEST_RESULT, new UnsignedWordElement(38005)), //
						new DummyRegisterElement(38006, 38007), //
						m(GoodWe.ChannelId.VAC_1, new UnsignedWordElement(38008),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FAC_1, new UnsignedWordElement(38009),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.PAC_1, new UnsignedWordElement(38010)), //
						new DummyRegisterElement(38011), //
						m(GoodWe.ChannelId.LINE_1_AVG_FAULT_VALUE, new UnsignedWordElement(38012),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_1_AVG_FAULT_TIME, new UnsignedWordElement(38013)), //
						m(GoodWe.ChannelId.LINE_1_V_HIGH_FAULT_VALUE, new UnsignedWordElement(38014),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_1_V_HIGH_FAULT_TIME, new UnsignedWordElement(38015)), //
						m(GoodWe.ChannelId.LINE_1_V_LOW_FAULT_VALUE_S1, new UnsignedWordElement(38016),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_1_V_LOW_FAULT_TIME_S1, new UnsignedWordElement(38017)), //
						m(GoodWe.ChannelId.LINE_1_V_LOW_FAULT_VALUE_S2, new UnsignedWordElement(38018),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_1_V_LOW_FAULT_TIME_S2, new UnsignedWordElement(38019)), //
						m(GoodWe.ChannelId.LINE_1_F_HIGH_FAULT_VALUE_COM, new UnsignedWordElement(38020),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_1_F_HIGH_FAULT_TIME_COM, new UnsignedWordElement(38021)), //
						m(GoodWe.ChannelId.LINE_1_FLOW_FAULT_VALUE_COM, new UnsignedWordElement(38022),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_1_FLOW_FAULT_TIME_COM, new UnsignedWordElement(38023)), //
						m(GoodWe.ChannelId.LINE_1_F_HIGH_FAULT_VALUE, new UnsignedWordElement(38024),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_1_F_HIGH_FAULT_TIME, new UnsignedWordElement(38025)), //
						m(GoodWe.ChannelId.LINE_1_F_LOW_FAULT_VALUE, new UnsignedWordElement(38026),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_1_F_LOW_FAULT_TIME, new UnsignedWordElement(38027)), //
						m(GoodWe.ChannelId.VAC_2, new UnsignedWordElement(38028),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FAC_2, new UnsignedWordElement(38029),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.PAC_2, new UnsignedWordElement(38030)), //
						new DummyRegisterElement(38031), //
						m(GoodWe.ChannelId.LINE_2_AVG_FAULT_VALUE, new UnsignedWordElement(38032),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_2_AVG_FAULT_TIME, new UnsignedWordElement(38033)), //
						m(GoodWe.ChannelId.LINE_2_V_HIGH_FAULT_VALUE, new UnsignedWordElement(38034),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_2_V_HIGH_FAULT_TIME, new UnsignedWordElement(38035)), //
						m(GoodWe.ChannelId.LINE_2_V_LOW_FAULT_VALUE_S1, new UnsignedWordElement(38036),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_2_V_LOW_FAULT_TIME_S1, new UnsignedWordElement(38037)), //
						m(GoodWe.ChannelId.LINE_2_V_LOW_FAULT_VALUE_S2, new UnsignedWordElement(38038),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_2_V_LOW_FAULT_TIME_S2, new UnsignedWordElement(38039)), //
						m(GoodWe.ChannelId.LINE_2_F_HIGH_FAULT_VALUE_COM, new UnsignedWordElement(38040),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_2_F_HIGH_FAULT_TIME_COM, new UnsignedWordElement(38041)), //
						m(GoodWe.ChannelId.LINE_2_FLOW_FAULT_VALUE_COM, new UnsignedWordElement(38042),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_2_FLOW_FAULT_TIME_COM, new UnsignedWordElement(38043)), //
						m(GoodWe.ChannelId.LINE_2_F_HIGH_FAULT_VALUE, new UnsignedWordElement(38044),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_2_F_HIGH_FAULT_TIME, new UnsignedWordElement(38045)), //
						m(GoodWe.ChannelId.LINE_2_F_LOW_FAULT_VALUE, new UnsignedWordElement(38046),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_2_F_LOW_FAULT_TIME, new UnsignedWordElement(38047)), //
						m(GoodWe.ChannelId.VAC_3, new UnsignedWordElement(38048),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FAC_3, new UnsignedWordElement(38049),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.PAC_3, new UnsignedWordElement(38050)), //
						new DummyRegisterElement(38051), //
						m(GoodWe.ChannelId.LINE_3_AVG_FAULT_VALUE, new UnsignedWordElement(38052),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_3_AVG_FAULT_TIME, new UnsignedWordElement(38053)), //
						m(GoodWe.ChannelId.LINE_3_V_HIGH_FAULT_VALUE, new UnsignedWordElement(38054),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_3_V_HIGH_FAULT_TIME, new UnsignedWordElement(38055)), //
						m(GoodWe.ChannelId.LINE_3_V_LOW_FAULT_VALUE_S1, new UnsignedWordElement(38056),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_3_V_LOW_FAULT_TIME_S1, new UnsignedWordElement(38057)), //
						m(GoodWe.ChannelId.LINE_3_V_LOW_FAULT_VALUE_S2, new UnsignedWordElement(38058),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LINE_3_V_LOW_FAULT_TIME_S2, new UnsignedWordElement(38059)), //
						m(GoodWe.ChannelId.LINE_3_F_HIGH_FAULT_VALUE_COM, new UnsignedWordElement(38060),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_3_F_HIGH_FAULT_TIME_COM, new UnsignedWordElement(38061)), //
						m(GoodWe.ChannelId.LINE_3_FLOW_FAULT_VALUE_COM, new UnsignedWordElement(38062),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_3_FLOW_FAULT_TIME_COM, new UnsignedWordElement(38063)), //
						m(GoodWe.ChannelId.LINE_3_F_HIGH_FAULT_VALUE, new UnsignedWordElement(38064),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_3_F_HIGH_FAULT_TIME, new UnsignedWordElement(38065)), //
						m(GoodWe.ChannelId.LINE_3_F_LOW_FAULT_VALUE, new UnsignedWordElement(38066),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.LINE_3_F_LOW_FAULT_TIME, new UnsignedWordElement(38067))), //

				//// Power Limit
				// new FC3ReadRegistersTask(38450, Priority.LOW, //
				//// Read Error for registerin range 48450-38463
				// m(GoodWe.ChannelId.FEED_POWER_LIMIT_COEFFICIENT, new
				//// UnsignedWordElement(38450)), //
				// m(GoodWe.ChannelId.L1_POWER_LIMIT, new UnsignedWordElement(38451)), //
				// m(GoodWe.ChannelId.L2_POWER_LIMIT, new UnsignedWordElement(38452)), //
				// m(GoodWe.ChannelId.L3_POWER_LIMIT, new UnsignedWordElement(38453)), //
				// m(GoodWe.ChannelId.INVERTER_POWER_FACTOR, new SignedWordElement(38454),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
				// m(GoodWe.ChannelId.PV_METER_DC_POWER, new SignedDoublewordElement(38455)), //
				// m(GoodWe.ChannelId.E_TOTAL_GRID_CHARGE, new UnsignedDoublewordElement(38457),
				// ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
				// m(GoodWe.ChannelId.DISPATCH_SWITCH, new UnsignedWordElement(38459)), //
				// m(GoodWe.ChannelId.DISPATCH_POWER, new SignedDoublewordElement(38460)), //
				// m(GoodWe.ChannelId.DISPATCH_SOC, new UnsignedWordElement(38462)), //
				// m(GoodWe.ChannelId.DISPATCH_MODE, new UnsignedWordElement(38463))),//

				// Setting and Controlling Data Registers
				new FC3ReadRegistersTask(45127, Priority.ONCE, //
						// Read Error For "FEED_POWER_LIMIT_COEFFICIENT", "ROUTER_PASSWORD",
						// "ROUTER_ENCRYPTION_METHOD"
						// m(GoodWe.ChannelId.ROUTER_SSID, new StringWordElement(45024, 30)), //
						// m(GoodWe.ChannelId.ROUTER_PASSWORD, new StringWordElement(45054, 20)), //
						// m(GoodWe.ChannelId.ROUTER_ENCRYPTION_METHOD, new StringWordElement(45074,
						// 1)), //
						// m(GoodWe.ChannelId.DOMAIN1, new StringWordElement(45075, 25)), //
						// m(GoodWe.ChannelId.PORT_NUMBER1, new UnsignedWordElement(45100)), //
						// m(GoodWe.ChannelId.DOMAIN2, new StringWordElement(45101, 25)), //
						// m(GoodWe.ChannelId.PORT_NUMBER2, new UnsignedWordElement(45126)), //
						m(GoodWe.ChannelId.MODBUS_ADDRESS, new UnsignedWordElement(45127)), //
						m(GoodWe.ChannelId.MODBUS_MANUFACTURER, new StringWordElement(45128, 4)), //
						m(GoodWe.ChannelId.MODBUS_BAUDRATE, new UnsignedDoublewordElement(45132))), //

				// TODO RTC reg num 45200-45201-45202

				new FC3ReadRegistersTask(45203, Priority.ONCE, //
						m(GoodWe.ChannelId.SERIAL_NUMBER, new StringWordElement(45203, 8)), //
						m(GoodWe.ChannelId.DEVICE_TYPE, new StringWordElement(45211, 5)), //
						m(GoodWe.ChannelId.RESUME_FACTORY_SETTING, new UnsignedWordElement(45216)), //
						m(GoodWe.ChannelId.CLEAR_DATA, new UnsignedWordElement(45217))), //

				new FC3ReadRegistersTask(45222, Priority.LOW, //
						// TODO Read Error, but its possible via qmodmaster
						m(GoodWe.ChannelId.PV_E_TOTAL_2, new UnsignedDoublewordElement(45222),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.PV_E_DAY_2, new UnsignedDoublewordElement(45224),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_SELL_2, new UnsignedDoublewordElement(45226),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.H_TOTAL_2, new UnsignedDoublewordElement(45228)), //
						m(GoodWe.ChannelId.E_DAY_SELL_2, new UnsignedWordElement(45230),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY_2, new UnsignedDoublewordElement(45231),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DAY_BUY_2, new UnsignedWordElement(45233),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_LOAD_2, new UnsignedDoublewordElement(45234),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_LOAD_DAY_2, new UnsignedWordElement(45236),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_CHARGE_2, new UnsignedDoublewordElement(45237),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_CHARGE_DAY_2, new UnsignedWordElement(45239),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_DISCHARGE_2, new UnsignedDoublewordElement(45240),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DISCHARGE_DAY_2, new UnsignedWordElement(45242),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LANGUAGE, new UnsignedWordElement(45243)), //
						m(GoodWe.ChannelId.SAFETY_COUNTRY_CODE, new UnsignedWordElement(45244)), //
						m(GoodWe.ChannelId.ISO, new UnsignedWordElement(45245)), //
						m(GoodWe.ChannelId.LVRT, new UnsignedWordElement(45246)), //
						m(GoodWe.ChannelId.ISLANDING, new UnsignedWordElement(45247))), //

				new FC3ReadRegistersTask(45249, Priority.LOW, //
						m(GoodWe.ChannelId.BURN_IN_RESET_TIME, new UnsignedWordElement(45249)), //
						m(GoodWe.ChannelId.PV_START_VOLTAGE, new UnsignedWordElement(45250),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.ENABLE_MPPT4_SHADOW, new UnsignedWordElement(45251)), //
						m(GoodWe.ChannelId.BACK_UP_ENABLE, new UnsignedWordElement(45252)), //
						m(GoodWe.ChannelId.AUTO_START_BACKUP, new UnsignedWordElement(45253)), //
						m(GoodWe.ChannelId.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(45254)), //
						m(GoodWe.ChannelId.REPAID_CUT_OFF, new UnsignedWordElement(45255)), //
						m(GoodWe.ChannelId.BACKUP_START_DLY, new UnsignedWordElement(45256)), //
						m(GoodWe.ChannelId.UPS_STD_VOLT_TYPE, new UnsignedWordElement(45257)), //
						new DummyRegisterElement(45258), //
						m(GoodWe.ChannelId.BURN_IN_MODE, new UnsignedWordElement(45259)), //
						m(GoodWe.ChannelId.BACKUP_OVERLOAD_DELAY, new UnsignedWordElement(45260)), //
						m(GoodWe.ChannelId.UPSPHASE_TYPE, new UnsignedWordElement(45261)), //
						new DummyRegisterElement(45262), //
						m(GoodWe.ChannelId.DERATE_RATE_VDE, new UnsignedWordElement(45263)), //
						m(GoodWe.ChannelId.THREE_PHASE_UNBALANCED_OUTPUT, new UnsignedWordElement(45264)), //
						m(GoodWe.ChannelId.PRE_RELAY_CHECK_ENABLE, new UnsignedWordElement(45265)), //
						m(GoodWe.ChannelId.HIGH_IMP_MODE, new UnsignedWordElement(45266)), //
						m(GoodWe.ChannelId.BAT_SP_FUNC, new UnsignedWordElement(45267)), //
						m(GoodWe.ChannelId.AFCI_SHUT_OFF_PWM, new UnsignedWordElement(45268))), //
				// Illegal data adress
				// new FC3ReadRegistersTask(45333, Priority.LOW, //
				// m(GoodWe.ChannelId.USER_LICENCE, new StringWordElement(45333, 3)), //
				// m(GoodWe.ChannelId.REMOTE_USER_LICENCE, new StringWordElement(45336, 3)), //
				// m(GoodWe.ChannelId.REMOTE_LOCK_CODE, new StringWordElement(45339, 3))), //

				new FC3ReadRegistersTask(45350, Priority.LOW, //
						m(GoodWe.ChannelId.BMS_LEAD_CAPACITY, new UnsignedWordElement(45350)), // [25,2000]
						m(GoodWe.ChannelId.BMS_STRINGS, new UnsignedWordElement(45351)), // [4~12] N
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(45352),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [500*N,600*N]
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(45353),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45354),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [400*N,480*N]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(45355),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_SOC_UNDER_MIN, new UnsignedWordElement(45356)), // [0,100]
						m(GoodWe.ChannelId.BMS_OFFLINE_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45357),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(45358))), //

				// Safety
				new FC3ReadRegistersTask(45400, Priority.LOW, //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GoodWe.ChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_START_TIME, new UnsignedWordElement(45421)), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45426))), //

				new FC3ReadRegistersTask(45428, Priority.LOW, //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //
						m(GoodWe.ChannelId.POWER_SLOPE_ENABLE, new UnsignedWordElement(45432))), //

				// Cos Phi Curve
				new FC3ReadRegistersTask(45433, Priority.LOW, //
						m(GoodWe.ChannelId.ENABLE_CURVE_PU, new UnsignedWordElement(45433)), //
						m(GoodWe.ChannelId.POINT_A_VALUE, new UnsignedWordElement(45434)), //
						m(GoodWe.ChannelId.POINT_A_PF, new UnsignedWordElement(45435),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POINT_B_VALUE, new UnsignedWordElement(45436)), //
						m(GoodWe.ChannelId.POINT_B_PF, new UnsignedWordElement(45437),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POINT_C_VALUE, new UnsignedWordElement(45438)), //
						m(GoodWe.ChannelId.POINT_C_PF, new UnsignedWordElement(45439),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // ), //
						m(GoodWe.ChannelId.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER, new SignedWordElement(45442)), //

						// Power and frequency curve
						m(new BitsWordElement(45443, this)//
								.bit(0, GoodWe.ChannelId.POWER_FREQUENCY_ENABLED)//
								.bit(1, GoodWe.ChannelId.POWER_FREQUENCY_RESPONSE_MODE)//
						), //
						m(GoodWe.ChannelId.FFROZEN_DCH, new UnsignedWordElement(45444),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH, new UnsignedWordElement(45445),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_DCH, new UnsignedWordElement(45446),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_CH, new UnsignedWordElement(45447),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_WAITING_TIME, new UnsignedWordElement(45448)), //
						m(GoodWe.ChannelId.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_SLOPE, new UnsignedWordElement(45451)), //
						m(GoodWe.ChannelId.FFROZEN_DCH_SLOPE, new UnsignedWordElement(45452),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH_SLOPE, new UnsignedWordElement(45453),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.DOWN_SLOPE_POWER_REFERENCE, new UnsignedWordElement(45454)), //
						m(GoodWe.ChannelId.DOWN_SLOP, new UnsignedWordElement(45455))), //

				// QU Curve
				new FC3ReadRegistersTask(45456, Priority.LOW, //
						m(GoodWe.ChannelId.ENABLE_CURVE_QU, new UnsignedWordElement(45456)), //
						m(GoodWe.ChannelId.LOCK_IN_POWER_QU, new UnsignedWordElement(45457)), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER_QU, new UnsignedWordElement(45458)), //
						m(GoodWe.ChannelId.V1_VOLTAGE, new UnsignedWordElement(45459),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodWe.ChannelId.V2_VOLTAGE, new UnsignedWordElement(45461),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodWe.ChannelId.V3_VOLTAGE, new UnsignedWordElement(45463),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodWe.ChannelId.V4_VOLTAGE, new UnsignedWordElement(45465),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE, new UnsignedWordElement(45466)), //
						m(GoodWe.ChannelId.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodWe.ChannelId.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodWe.ChannelId.MISCELLANEA, new UnsignedWordElement(45469)), //
						m(GoodWe.ChannelId.RATED_VOLTAGE, new UnsignedWordElement(45470)), //
						m(GoodWe.ChannelId.RESPONSE_TIME, new UnsignedWordElement(45471))), //

				// PU Curve
				new FC3ReadRegistersTask(45472, Priority.LOW, //
						m(GoodWe.ChannelId.PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodWe.ChannelId.POWER_CHANGE_RATE, new UnsignedWordElement(45473)), //
						m(GoodWe.ChannelId.V1_VOLTAGE_PU, new UnsignedWordElement(45474),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE_PU, new SignedWordElement(45475),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VOLTAGE_PU, new UnsignedWordElement(45476),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE_PU, new SignedWordElement(45477),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VOLTAGE_PU, new UnsignedWordElement(45478),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE_PU, new SignedWordElement(45479),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VOLTAGE_PU, new UnsignedWordElement(45480),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE_PU, new SignedWordElement(45481),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FIXED_POWER_FACTOR, new UnsignedWordElement(45482),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FIXED_REACTIVE_POWER, new UnsignedWordElement(45483),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_LIMIT_BY_VOLT_START_VOL, new UnsignedWordElement(45485),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_LIMIT_BY_VOLT_START_PER, new UnsignedWordElement(45486)), //
						m(GoodWe.ChannelId.GRID_LIMIT_BY_VOLT_SLOPE, new UnsignedWordElement(45487)), //
						m(GoodWe.ChannelId.AUTO_TEST_ENABLE, new UnsignedWordElement(45488)), //
						m(GoodWe.ChannelId.AUTO_TEST_STEP, new UnsignedWordElement(45489)), //
						m(GoodWe.ChannelId.UW_ITALY_FREQ_MODE, new UnsignedWordElement(45490)), //
						m(GoodWe.ChannelId.ALL_POWER_CURVE_DISABLE, new UnsignedWordElement(45491)), //
						m(GoodWe.ChannelId.R_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45492)), //
						m(GoodWe.ChannelId.S_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45493)), //
						m(GoodWe.ChannelId.T_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45494)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3, new UnsignedWordElement(45495),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3_TIME, new UnsignedWordElement(45496)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3, new UnsignedWordElement(45497),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3_TIME, new UnsignedWordElement(45498)), //
						m(GoodWe.ChannelId.ZVRT_CONFIG, new UnsignedWordElement(45499)), //
						m(GoodWe.ChannelId.LVRT_START_VOLT, new UnsignedWordElement(45500),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_END_VOLT, new UnsignedWordElement(45501),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_START_TRIP_TIME, new UnsignedWordElement(45502)), //
						m(GoodWe.ChannelId.LVRT_END_TRIP_TIME, new UnsignedWordElement(45503)), //
						m(GoodWe.ChannelId.LVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45504),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_VOLT, new UnsignedWordElement(45505),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_END_VOLT, new UnsignedWordElement(45506),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_TRIP_TIME, new UnsignedWordElement(45507)), //
						m(GoodWe.ChannelId.HVRT_END_TRIP_TIME, new UnsignedWordElement(45508)), //
						m(GoodWe.ChannelId.HVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45509),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)//
				), //

				new FC3ReadRegistersTask(47000, Priority.LOW, //
						m(GoodWe.ChannelId.SELECT_WORK_MODE, new UnsignedWordElement(47000)), //
						m(GoodWe.ChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodWe.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002)), //
						m(GoodWe.ChannelId.SIMULATE_METER_POWER, new UnsignedWordElement(47003)), //
						m(GoodWe.ChannelId.BREEZE_ON_OFF, new UnsignedWordElement(47004)), //
						m(GoodWe.ChannelId.LOG_DATA_ENABLE, new UnsignedWordElement(47005)), //
						m(GoodWe.ChannelId.DATA_SEND_INTERVAL, new UnsignedWordElement(47006)), //
						m(GoodWe.ChannelId.DRED_CMD, new UnsignedWordElement(47007)), //
						m(GoodWe.ChannelId.LED_TEST_FLAG, new UnsignedWordElement(47008)), //
						m(GoodWe.ChannelId.WIFI_OR_LAN_SWITCH, new UnsignedWordElement(47009)), //
						m(GoodWe.ChannelId.DRED_OFFGRID_CHECK, new UnsignedWordElement(47010)), //
						m(GoodWe.ChannelId.EXTERNAL_EMS_FLAG, new UnsignedWordElement(47011)), //
						m(GoodWe.ChannelId.LED_BLINK_TIME, new UnsignedWordElement(47012)), //
						m(GoodWe.ChannelId.WIFI_LED_STATE, new UnsignedWordElement(47013)), //
						m(GoodWe.ChannelId.COM_LED_STATE, new UnsignedWordElement(47014))), //

				new FC3ReadRegistersTask(47500, Priority.LOW, //
						m(GoodWe.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						m(GoodWe.ChannelId.BMS_FLOAT_VOLT, new UnsignedWordElement(47501),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_FLOAT_CURRENT, new UnsignedWordElement(47502)), //
						m(GoodWe.ChannelId.BMS_FLOAT_TIME, new UnsignedWordElement(47503)), //
						m(GoodWe.ChannelId.BMS_TYPE_INDEX_ARM, new UnsignedWordElement(47504)), //
						m(GoodWe.ChannelId.MANUFACTURE_CODE, new UnsignedWordElement(47505)), //
						m(GoodWe.ChannelId.DC_VOLT_OUTPUT, new UnsignedWordElement(47506)), //
						m(GoodWe.ChannelId.BMS_AVG_CHG_VOLT, new UnsignedWordElement(47507),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_AVG_CHG_HOURS, new UnsignedWordElement(47508)), //
						m(GoodWe.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWe.ChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(GoodWe.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWe.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512)), //
						m(GoodWe.ChannelId.BMS_CURR_LMT_COFF, new UnsignedWordElement(47513)), //
						m(GoodWe.ChannelId.BATTERY_PROTOCOL_ARM, new UnsignedWordElement(47514)), //

						m(GoodWe.ChannelId.WORK_WEEK_1_START_TIME, new UnsignedWordElement(47515)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_END_TIME, new UnsignedWordElement(47516)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_BAT_POWER_PERCENT, new UnsignedWordElement(47517)), //
						m(new BitsWordElement(47518, this)//
								.bit(0, GoodWe.ChannelId.WORK_WEEK_1_SUNDAY)//
								.bit(1, GoodWe.ChannelId.WORK_WEEK_1_MONDAY)//
								.bit(2, GoodWe.ChannelId.WORK_WEEK_1_TUESDAY)//
								.bit(3, GoodWe.ChannelId.WORK_WEEK_1_WEDNESDAY)//
								.bit(4, GoodWe.ChannelId.WORK_WEEK_1_THURSDAY)//
								.bit(5, GoodWe.ChannelId.WORK_WEEK_1_FRIDAY)//
								.bit(6, GoodWe.ChannelId.WORK_WEEK_1_SATURDAY)//
								.bit(7, GoodWe.ChannelId.WORK_WEEK_1_NA)//
								.bit(8, GoodWe.ChannelId.WORK_WEEK_1_ENABLED)//
						)), //

				new FC3ReadRegistersTask(47519, Priority.LOW, //
						m(GoodWe.ChannelId.WORK_WEEK_2_START_TIME, new UnsignedWordElement(47519)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_END_TIME, new UnsignedWordElement(47520)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_BAT_POWER_PERCENT, new UnsignedWordElement(47521)), //
						m(new BitsWordElement(47522, this)//
								.bit(0, GoodWe.ChannelId.WORK_WEEK_2_SUNDAY)//
								.bit(1, GoodWe.ChannelId.WORK_WEEK_2_MONDAY)//
								.bit(2, GoodWe.ChannelId.WORK_WEEK_2_TUESDAY)//
								.bit(3, GoodWe.ChannelId.WORK_WEEK_2_WEDNESDAY)//
								.bit(4, GoodWe.ChannelId.WORK_WEEK_2_THURSDAY)//
								.bit(5, GoodWe.ChannelId.WORK_WEEK_2_FRIDAY)//
								.bit(6, GoodWe.ChannelId.WORK_WEEK_2_SATURDAY)//
								.bit(7, GoodWe.ChannelId.WORK_WEEK_2_NA)//
								.bit(8, GoodWe.ChannelId.WORK_WEEK_2_ENABLED)//
						), //

						m(GoodWe.ChannelId.WORK_WEEK_3_START_TIME, new UnsignedWordElement(47523)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_END_TIME, new UnsignedWordElement(47524)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_BAT_POWER_PERCENT, new UnsignedWordElement(47525)), //
						m(new BitsWordElement(47526, this)//
								.bit(0, GoodWe.ChannelId.WORK_WEEK_3_SUNDAY)//
								.bit(1, GoodWe.ChannelId.WORK_WEEK_3_MONDAY)//
								.bit(2, GoodWe.ChannelId.WORK_WEEK_3_TUESDAY)//
								.bit(3, GoodWe.ChannelId.WORK_WEEK_3_WEDNESDAY)//
								.bit(4, GoodWe.ChannelId.WORK_WEEK_3_THURSDAY)//
								.bit(5, GoodWe.ChannelId.WORK_WEEK_3_FRIDAY)//
								.bit(6, GoodWe.ChannelId.WORK_WEEK_3_SATURDAY)//
								.bit(7, GoodWe.ChannelId.WORK_WEEK_3_NA)//
								.bit(8, GoodWe.ChannelId.WORK_WEEK_3_ENABLED)//
						), //

						m(GoodWe.ChannelId.WORK_WEEK_4_START_TIME, new UnsignedWordElement(47527)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_END_TIME, new UnsignedWordElement(47528)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_BMS_POWER_PERCENT, new UnsignedWordElement(47529)), //
						m(new BitsWordElement(47530, this)//
								.bit(0, GoodWe.ChannelId.WORK_WEEK_4_SUNDAY)//
								.bit(1, GoodWe.ChannelId.WORK_WEEK_4_MONDAY)//
								.bit(2, GoodWe.ChannelId.WORK_WEEK_4_TUESDAY)//
								.bit(3, GoodWe.ChannelId.WORK_WEEK_4_WEDNESDAY)//
								.bit(4, GoodWe.ChannelId.WORK_WEEK_4_THURSDAY)//
								.bit(5, GoodWe.ChannelId.WORK_WEEK_4_FRIDAY)//
								.bit(6, GoodWe.ChannelId.WORK_WEEK_4_SATURDAY)//
								.bit(7, GoodWe.ChannelId.WORK_WEEK_4_NA)//
								.bit(8, GoodWe.ChannelId.WORK_WEEK_4_ENABLED)//
						), //
						m(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						new DummyRegisterElement(47533), //
						m(GoodWe.ChannelId.THREE_PHASE_FEED_POWER_ENABLE, new UnsignedWordElement(47534)), //
						m(GoodWe.ChannelId.R_PHASE_FEED_POWER_PARA, new UnsignedWordElement(47535)), //
						m(GoodWe.ChannelId.S_PHASE_FEED_POWER_PARA, new UnsignedWordElement(47536)), //
						m(GoodWe.ChannelId.T_PHASE_FEED_POWER_PARA, new UnsignedWordElement(47537)), //
						m(GoodWe.ChannelId.STOP_SOC_ADJUST, new UnsignedWordElement(47538))//
				), //

				// Setting and Controlling Data Registers
				new FC16WriteRegistersTask(45075, //
						// Read Error For "FEED_POWER_LIMIT_COEFFICIENT", "ROUTER_PASSWORD",
						// "ROUTER_ENCRYPTION_METHOD"
						// m(GoodWe.ChannelId.ROUTER_SSID, new StringWordElement(45024, 30)), //
						// m(GoodWe.ChannelId.ROUTER_PASSWORD, new StringWordElement(45054, 20)), //
						// m(GoodWe.ChannelId.ROUTER_ENCRYPTION_METHOD, new StringWordElement(45074,
						// 1)), //
						m(GoodWe.ChannelId.DOMAIN1, new StringWordElement(45075, 25)), //
						m(GoodWe.ChannelId.PORT_NUMBER1, new UnsignedWordElement(45100)), //
						m(GoodWe.ChannelId.DOMAIN2, new StringWordElement(45101, 25)), //
						m(GoodWe.ChannelId.PORT_NUMBER2, new UnsignedWordElement(45126)), //
						m(GoodWe.ChannelId.MODBUS_ADDRESS, new UnsignedWordElement(45127)), //
						m(GoodWe.ChannelId.MODBUS_MANUFACTURER, new StringWordElement(45128, 4)), //
						m(GoodWe.ChannelId.MODBUS_BAUDRATE, new UnsignedDoublewordElement(45132))), //

				new FC16WriteRegistersTask(45203, //
						m(GoodWe.ChannelId.SERIAL_NUMBER, new StringWordElement(45203, 8)), //
						m(GoodWe.ChannelId.GOODWE_TYPE, new StringWordElement(45211, 5)), //
						m(GoodWe.ChannelId.RESUME_FACTORY_SETTING, new UnsignedWordElement(45216)), //
						m(GoodWe.ChannelId.CLEAR_DATA, new UnsignedWordElement(45217)), //
						m(GoodWe.ChannelId.ALLOW_CONNECT_TO_GRID, new UnsignedWordElement(45218)), //
						m(GoodWe.ChannelId.FORBID_CONNECT_TO_GRID, new UnsignedWordElement(45219)), //
						m(GoodWe.ChannelId.RESET, new UnsignedWordElement(45220)), //
						m(GoodWe.ChannelId.RESET_SPS, new UnsignedWordElement(45221)), //
						m(GoodWe.ChannelId.PV_E_TOTAL_2, new UnsignedDoublewordElement(45222),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.PV_E_DAY_2, new UnsignedDoublewordElement(45224),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_SELL_2, new UnsignedDoublewordElement(45226),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.H_TOTAL_2, new UnsignedDoublewordElement(45228)), //
						m(GoodWe.ChannelId.E_DAY_SELL_2, new UnsignedWordElement(45230),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY_2, new UnsignedDoublewordElement(45231),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DAY_BUY_2, new UnsignedWordElement(45233),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_LOAD_2, new UnsignedDoublewordElement(45234),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_LOAD_DAY_2, new UnsignedWordElement(45236),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_CHARGE_2, new UnsignedDoublewordElement(45237),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_CHARGE_DAY_2, new UnsignedWordElement(45239),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_DISCHARGE_2, new UnsignedDoublewordElement(45240),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DISCHARGE_DAY_2, new UnsignedWordElement(45242),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LANGUAGE, new UnsignedWordElement(45243)), //
						m(GoodWe.ChannelId.SAFETY_COUNTRY_CODE, new UnsignedWordElement(45244)), //
						m(GoodWe.ChannelId.ISO, new UnsignedWordElement(45245)), //
						m(GoodWe.ChannelId.LVRT, new UnsignedWordElement(45246)), //
						m(GoodWe.ChannelId.ISLANDING, new UnsignedWordElement(45247)), //
						new DummyRegisterElement(45248), //
						m(GoodWe.ChannelId.BURN_IN_RESET_TIME, new UnsignedWordElement(45249)), //
						m(GoodWe.ChannelId.PV_START_VOLTAGE, new UnsignedWordElement(45250),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.ENABLE_MPPT4_SHADOW, new UnsignedWordElement(45251)), //
						m(GoodWe.ChannelId.BACK_UP_ENABLE, new UnsignedWordElement(45252)), //
						m(GoodWe.ChannelId.AUTO_START_BACKUP, new UnsignedWordElement(45253)), //
						m(GoodWe.ChannelId.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(45254)), //
						m(GoodWe.ChannelId.REPAID_CUT_OFF, new UnsignedWordElement(45255)), //
						m(GoodWe.ChannelId.BACKUP_START_DLY, new UnsignedWordElement(45256)), //
						m(GoodWe.ChannelId.UPS_STD_VOLT_TYPE, new UnsignedWordElement(45257)), //
						new DummyRegisterElement(45258), //
						m(GoodWe.ChannelId.BURN_IN_MODE, new UnsignedWordElement(45259)), //
						m(GoodWe.ChannelId.BACKUP_OVERLOAD_DELAY, new UnsignedWordElement(45260)), //
						m(GoodWe.ChannelId.UPSPHASE_TYPE, new UnsignedWordElement(45261)), //
						new DummyRegisterElement(45262), //
						m(GoodWe.ChannelId.DERATE_RATE_VDE, new UnsignedWordElement(45263)), //
						m(GoodWe.ChannelId.THREE_PHASE_UNBALANCED_OUTPUT, new UnsignedWordElement(45264)), //
						m(GoodWe.ChannelId.PRE_RELAY_CHECK_ENABLE, new UnsignedWordElement(45265)), //
						m(GoodWe.ChannelId.HIGH_IMP_MODE, new UnsignedWordElement(45266)), //
						m(GoodWe.ChannelId.BAT_SP_FUNC, new UnsignedWordElement(45267)), //
						m(GoodWe.ChannelId.AFCI_SHUT_OFF_PWM, new UnsignedWordElement(45268)), //
						new DummyRegisterElement(45269, 45329), //
						m(GoodWe.ChannelId.DEVICE_LICENCE, new StringWordElement(45330, 3)), //
						m(GoodWe.ChannelId.USER_LICENCE, new StringWordElement(45333, 3)), //
						m(GoodWe.ChannelId.REMOTE_USER_LICENCE, new StringWordElement(45336, 3)), //
						m(GoodWe.ChannelId.REMOTE_LOCK_CODE, new StringWordElement(45339, 3))), //

				new FC16WriteRegistersTask(45350, //
						m(GoodWe.ChannelId.BMS_LEAD_CAPACITY, new UnsignedWordElement(45350)), // [25,2000]
						m(GoodWe.ChannelId.BMS_STRINGS, new UnsignedWordElement(45351)), // [4~12] N
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(45352),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [500*N,600*N]
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(45353),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45354),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [400*N,480*N]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(45355),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_SOC_UNDER_MIN, new UnsignedWordElement(45356))), // [0,100]

				new FC16WriteRegistersTask(45360, //
						m(GoodWe.ChannelId.CLEAR_BATTERY_SETTING, new UnsignedWordElement(45360)), //
						new DummyRegisterElement(45361, 45399), //
						// Safety
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GoodWe.ChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_START_TIME, new UnsignedWordElement(45421)), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45426)), //
						new DummyRegisterElement(45427), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //
						m(GoodWe.ChannelId.POWER_SLOPE_ENABLE, new UnsignedWordElement(45432))), //

				// Cos Phi Curve
				new FC16WriteRegistersTask(45433, //
						m(GoodWe.ChannelId.ENABLE_CURVE_PU, new UnsignedWordElement(45433)), //
						m(GoodWe.ChannelId.POINT_A_VALUE, new UnsignedWordElement(45434)), //
						m(GoodWe.ChannelId.POINT_A_PF, new UnsignedWordElement(45435),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POINT_B_VALUE, new UnsignedWordElement(45436)), //
						m(GoodWe.ChannelId.POINT_B_PF, new UnsignedWordElement(45437),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POINT_C_VALUE, new UnsignedWordElement(45438)), //
						m(GoodWe.ChannelId.POINT_C_PF, new UnsignedWordElement(45439),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // ), //
						m(GoodWe.ChannelId.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER, new SignedWordElement(45442))), //

				// Power and frequency curve
				// m(new BitsWordElement(45443, this)//
				// .bit(0, GoodWe.ChannelId.STATE_70)//
				// .bit(1, GoodWe.ChannelId.STATE_71)), //

				new FC16WriteRegistersTask(45444, //
						m(GoodWe.ChannelId.FFROZEN_DCH, new UnsignedWordElement(45444),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH, new UnsignedWordElement(45445),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_DCH, new UnsignedWordElement(45446),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_CH, new UnsignedWordElement(45447),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_WAITING_TIME, new UnsignedWordElement(45448)), //
						m(GoodWe.ChannelId.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_SLOPE, new UnsignedWordElement(45451)), //
						m(GoodWe.ChannelId.FFROZEN_DCH_SLOPE, new UnsignedWordElement(45452),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH_SLOPE, new UnsignedWordElement(45453),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.DOWN_SLOPE_POWER_REFERENCE, new UnsignedWordElement(45454)), //
						m(GoodWe.ChannelId.DOWN_SLOP, new UnsignedWordElement(45455)), //

						// QU Curve
						m(GoodWe.ChannelId.ENABLE_CURVE_QU, new UnsignedWordElement(45456)), //
						m(GoodWe.ChannelId.LOCK_IN_POWER_QU, new UnsignedWordElement(45457)), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER_QU, new UnsignedWordElement(45458)), //
						m(GoodWe.ChannelId.V1_VOLTAGE, new UnsignedWordElement(45459),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodWe.ChannelId.V2_VOLTAGE, new UnsignedWordElement(45461),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodWe.ChannelId.V3_VOLTAGE, new UnsignedWordElement(45463),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodWe.ChannelId.V4_VOLTAGE, new UnsignedWordElement(45465),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE, new UnsignedWordElement(45466)), //
						m(GoodWe.ChannelId.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodWe.ChannelId.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodWe.ChannelId.MISCELLANEA, new UnsignedWordElement(45469)), //
						m(GoodWe.ChannelId.RATED_VOLTAGE, new UnsignedWordElement(45470)), //
						m(GoodWe.ChannelId.RESPONSE_TIME, new UnsignedWordElement(45471))), //

				// PU Curve
				new FC16WriteRegistersTask(45472, //
						m(GoodWe.ChannelId.PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodWe.ChannelId.POWER_CHANGE_RATE, new UnsignedWordElement(45473)), //
						m(GoodWe.ChannelId.V1_VOLTAGE_PU, new UnsignedWordElement(45474),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE_PU, new SignedWordElement(45475),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VOLTAGE_PU, new UnsignedWordElement(45476),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE_PU, new SignedWordElement(45477),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VOLTAGE_PU, new UnsignedWordElement(45478),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE_PU, new SignedWordElement(45479),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VOLTAGE_PU, new UnsignedWordElement(45480),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE_PU, new SignedWordElement(45481),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FIXED_POWER_FACTOR, new UnsignedWordElement(45482),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FIXED_REACTIVE_POWER, new UnsignedWordElement(45483),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_LIMIT_BY_VOLT_START_VOL, new UnsignedWordElement(45485),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_LIMIT_BY_VOLT_START_PER, new UnsignedWordElement(45486)), //
						m(GoodWe.ChannelId.GRID_LIMIT_BY_VOLT_SLOPE, new UnsignedWordElement(45487)), //
						m(GoodWe.ChannelId.AUTO_TEST_ENABLE, new UnsignedWordElement(45488)), //
						m(GoodWe.ChannelId.AUTO_TEST_STEP, new UnsignedWordElement(45489)), //
						m(GoodWe.ChannelId.UW_ITALY_FREQ_MODE, new UnsignedWordElement(45490)), //
						m(GoodWe.ChannelId.ALL_POWER_CURVE_DISABLE, new UnsignedWordElement(45491)), //
						m(GoodWe.ChannelId.R_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45492)), //
						m(GoodWe.ChannelId.S_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45493)), //
						m(GoodWe.ChannelId.T_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45494)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3, new UnsignedWordElement(45495),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3_TIME, new UnsignedWordElement(45496)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3, new UnsignedWordElement(45497),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3_TIME, new UnsignedWordElement(45498)), //
						m(GoodWe.ChannelId.ZVRT_CONFIG, new UnsignedWordElement(45499)), //
						m(GoodWe.ChannelId.LVRT_START_VOLT, new UnsignedWordElement(45500),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_END_VOLT, new UnsignedWordElement(45501),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_START_TRIP_TIME, new UnsignedWordElement(45502)), //
						m(GoodWe.ChannelId.LVRT_END_TRIP_TIME, new UnsignedWordElement(45503)), //
						m(GoodWe.ChannelId.LVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45504),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_VOLT, new UnsignedWordElement(45505),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_END_VOLT, new UnsignedWordElement(45506),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_TRIP_TIME, new UnsignedWordElement(45507)), //
						m(GoodWe.ChannelId.HVRT_END_TRIP_TIME, new UnsignedWordElement(45508)), //
						m(GoodWe.ChannelId.HVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45509),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)//
				), //
				new FC16WriteRegistersTask(47000, //
						m(GoodWe.ChannelId.SELECT_WORK_MODE, new UnsignedWordElement(47000)), //
						m(GoodWe.ChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodWe.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC16WriteRegistersTask(47003, //
						m(GoodWe.ChannelId.SIMULATE_METER_POWER, new UnsignedWordElement(47003)), //
						m(GoodWe.ChannelId.BREEZE_ON_OFF, new UnsignedWordElement(47004)), //
						m(GoodWe.ChannelId.LOG_DATA_ENABLE, new UnsignedWordElement(47005)), //
						m(GoodWe.ChannelId.DATA_SEND_INTERVAL, new UnsignedWordElement(47006)), //
						m(GoodWe.ChannelId.DRED_CMD, new UnsignedWordElement(47007)), //
						m(GoodWe.ChannelId.LED_TEST_FLAG, new UnsignedWordElement(47008)), //
						m(GoodWe.ChannelId.WIFI_OR_LAN_SWITCH, new UnsignedWordElement(47009)), //
						m(GoodWe.ChannelId.DRED_OFFGRID_CHECK, new UnsignedWordElement(47010)), //
						m(GoodWe.ChannelId.EXTERNAL_EMS_FLAG, new UnsignedWordElement(47011)), //
						m(GoodWe.ChannelId.LED_BLINK_TIME, new UnsignedWordElement(47012)), //
						m(GoodWe.ChannelId.WIFI_LED_STATE, new UnsignedWordElement(47013)), //
						m(GoodWe.ChannelId.COM_LED_STATE, new UnsignedWordElement(47014))), //

				new FC16WriteRegistersTask(47500, //
						m(GoodWe.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						m(GoodWe.ChannelId.BMS_FLOAT_VOLT, new UnsignedWordElement(47501),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_FLOAT_CURRENT, new UnsignedWordElement(47502),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_FLOAT_TIME, new UnsignedWordElement(47503)), //
						m(GoodWe.ChannelId.BMS_TYPE_INDEX_ARM, new UnsignedWordElement(47504)), //
						m(GoodWe.ChannelId.MANUFACTURE_CODE, new UnsignedWordElement(47505)), //
						m(GoodWe.ChannelId.DC_VOLT_OUTPUT, new UnsignedWordElement(47506)), //
						m(GoodWe.ChannelId.BMS_AVG_CHG_VOLT, new UnsignedWordElement(47507),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_AVG_CHG_HOURS, new UnsignedWordElement(47508)), //
						m(GoodWe.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWe.ChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(GoodWe.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWe.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512)), //

						m(GoodWe.ChannelId.BMS_CURR_LMT_COFF, new UnsignedWordElement(47513),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.BATTERY_PROTOCOL_ARM, new UnsignedWordElement(47514)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_START_TIME, new UnsignedWordElement(47515)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_END_TIME, new UnsignedWordElement(47516)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_BAT_POWER_PERCENT, new UnsignedWordElement(47517)), //
						new DummyRegisterElement(47518), //

						m(GoodWe.ChannelId.WORK_WEEK_2_START_TIME, new UnsignedWordElement(47519)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_END_TIME, new UnsignedWordElement(47520)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_BAT_POWER_PERCENT, new UnsignedWordElement(47521)), //
						new DummyRegisterElement(47522), //

						m(GoodWe.ChannelId.WORK_WEEK_3_START_TIME, new UnsignedWordElement(47523)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_END_TIME, new UnsignedWordElement(47524)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_BAT_POWER_PERCENT, new UnsignedWordElement(47525)), //
						new DummyRegisterElement(47526), //

						m(GoodWe.ChannelId.WORK_WEEK_4_START_TIME, new UnsignedWordElement(47527)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_END_TIME, new UnsignedWordElement(47528)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_BMS_POWER_PERCENT, new UnsignedWordElement(47529)), //
						new DummyRegisterElement(47530), //

						m(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						m(GoodWe.ChannelId.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533)), //
						m(GoodWe.ChannelId.THREE_PHASE_FEED_POWER_ENABLE, new UnsignedWordElement(47534)), //
						m(GoodWe.ChannelId.R_PHASE_FEED_POWER_PARA, new UnsignedWordElement(47535)), //
						m(GoodWe.ChannelId.S_PHASE_FEED_POWER_PARA, new UnsignedWordElement(47536)), //
						m(GoodWe.ChannelId.T_PHASE_FEED_POWER_PARA, new UnsignedWordElement(47537)), //
						m(GoodWe.ChannelId.STOP_SOC_ADJUST, new UnsignedWordElement(47538)), //
						m(GoodWe.ChannelId.WIFI_RESET, new UnsignedWordElement(47539)), //
						m(GoodWe.ChannelId.ARM_SOFT_RESET, new UnsignedWordElement(47540))).debug(), //

				new FC16WriteRegistersTask(47900, //
						m(GoodWe.ChannelId.WBMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodWe.ChannelId.WBMS_STRINGS, new UnsignedWordElement(47901)), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(47902),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(47903),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(47904),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(47905),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_VOLTAGE, new UnsignedWordElement(47906),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CURRENT, new UnsignedWordElement(47907),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_SOC, new UnsignedWordElement(47908)), //
						m(GoodWe.ChannelId.WBMS_SOH, new UnsignedWordElement(47909)), //
						m(GoodWe.ChannelId.WBMS_TEMPERATURE, new SignedWordElement(47910),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						/**
						 * Warning Codes (table 8-8).
						 * 
						 * <ul>
						 * <li>Bit 12-31 Reserved
						 * <li>Bit 11: System High Temperature
						 * <li>Bit 10: System Low Temperature 2
						 * <li>Bit 09: System Low Temperature 1
						 * <li>Bit 08: Cell Imbalance
						 * <li>Bit 07: System Reboot
						 * <li>Bit 06: Communication Failure
						 * <li>Bit 05: Discharge Over-Current
						 * <li>Bit 04: Charge Over-Current
						 * <li>Bit 03: Cell Low Temperature
						 * <li>Bit 02: Cell High Temperature
						 * <li>Bit 01: Discharge Under-Voltage
						 * <li>Bit 00: Charge Over-Voltage
						 * </ul>
						 */
						m(GoodWe.ChannelId.WBMS_WARNING_CODE, new UnsignedDoublewordElement(47911)), //
						/**
						 * Alarm Codes (table 8-7).
						 * 
						 * <ul>
						 * <li>Bit 16-31 Reserved
						 * <li>Bit 15: Charge Over-Voltage Fault
						 * <li>Bit 14: Discharge Under-Voltage Fault
						 * <li>Bit 13: Cell High Temperature
						 * <li>Bit 12: Communication Fault
						 * <li>Bit 11: Charge Circuit Fault
						 * <li>Bit 10: Discharge Circuit Fault
						 * <li>Bit 09: Battery Lock
						 * <li>Bit 08: Battery Break
						 * <li>Bit 07: DC Bus Fault
						 * <li>Bit 06: Precharge Fault
						 * <li>Bit 05: Discharge Over-Current
						 * <li>Bit 04: Charge Over-Current
						 * <li>Bit 03: Cell Low Temperature
						 * <li>Bit 02: Cell High Temperature
						 * <li>Bit 01: Discharge Under-Voltage
						 * <li>Bit 00: Charge Over-Voltage
						 * </ul>
						 */
						m(GoodWe.ChannelId.WBMS_ALARM_CODE, new UnsignedDoublewordElement(47913)), //
						/**
						 * BMS Status
						 * 
						 * <ul>
						 * <li>Bit 2: Stop Discharge
						 * <li>Bit 1: Stop Charge
						 * <li>Bit 0: Force Charge
						 * </ul>
						 */
						m(GoodWe.ChannelId.WBMS_STATUS, new UnsignedWordElement(47915)), //
						m(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, new UnsignedWordElement(47916))).debug(), //

				new FC3ReadRegistersTask(47900, Priority.LOW, //
						m(GoodWe.ChannelId.WBMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodWe.ChannelId.WBMS_STRINGS, new UnsignedWordElement(47901)), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(47902),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(47903),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(47904),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(47905),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_VOLTAGE, new UnsignedWordElement(47906),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CURRENT, new UnsignedWordElement(47907)), //
						m(GoodWe.ChannelId.WBMS_SOC, new UnsignedWordElement(47908)), //
						m(GoodWe.ChannelId.WBMS_SOH, new UnsignedWordElement(47909)), //
						m(GoodWe.ChannelId.WBMS_TEMPERATURE, new SignedWordElement(47910),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_WARNING_CODE, new UnsignedDoublewordElement(47911)), //
						m(GoodWe.ChannelId.WBMS_ALARM_CODE, new UnsignedDoublewordElement(47913)), //
						// TODO reset to individual states

						m(GoodWe.ChannelId.WBMS_STATUS, new UnsignedWordElement(47915)), //
						m(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, new UnsignedWordElement(47916)))); //
	}

	protected AbstractModbusElement<?> getSocModbusElement(int address) throws NotImplementedException {
		if (this instanceof HybridEss) {
			return m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(address), new ElementToChannelConverter(
					// element -> channel
					value -> {
						// Set SoC to undefined if there is No Battery
						EnumReadChannel batteryModeChannel = this.channel(GoodWe.ChannelId.BATTERY_MODE);
						BatteryMode batteryMode = batteryModeChannel.value().asEnum();
						if (batteryMode == BatteryMode.NO_BATTERY || batteryMode == BatteryMode.UNDEFINED) {
							return null;
						} else {
							return value;
						}
					},
					// channel -> element
					value -> value));
		} else if (this instanceof HybridManagedSymmetricBatteryInverter) {
			return new DummyRegisterElement(address);
		} else {
			throw new NotImplementedException("Wrong implementation of AbstractGoodWe");
		}
	}

	private io.openems.edge.common.channel.ChannelId getDcDischargeEnergyChannel() throws NotImplementedException {
		if (this instanceof HybridEss) {
			return HybridEss.ChannelId.DC_DISCHARGE_ENERGY;
		} else if (this instanceof HybridManagedSymmetricBatteryInverter) {
			return HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY;
		} else {
			throw new NotImplementedException("Wrong implementation of AbstractGoodWe");
		}
	}

	private io.openems.edge.common.channel.ChannelId getDcChargeEnergyChannel() throws OpenemsException {
		if (this instanceof HybridEss) {
			return HybridEss.ChannelId.DC_CHARGE_ENERGY;
		} else if (this instanceof HybridManagedSymmetricBatteryInverter) {
			return HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY;
		} else {
			throw new NotImplementedException("Wrong implementation of AbstractGoodWe");
		}
	}

	@Override
	public final void addCharger(AbstractGoodWeEtCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public final void removeCharger(AbstractGoodWeEtCharger charger) {
		this.chargers.remove(charger);
	}

	/**
	 * Gets the PV production from chargers ACTUAL_POWER. Returns null if the PV
	 * production is not available.
	 * 
	 * @return production power
	 */
	protected final Integer calculatePvProduction() {
		Integer productionPower = null;
		for (AbstractGoodWeEtCharger charger : this.chargers) {
			productionPower = TypeUtils.sum(productionPower, charger.getActualPower().get());
		}
		return productionPower;
	}

	protected void updatePowerAndEnergyChannels() {
		Integer productionPower = this.calculatePvProduction();
		final Channel<Integer> pBattery1Channel = this.channel(GoodWe.ChannelId.P_BATTERY1);
		Integer dcDischargePower = pBattery1Channel.value().get();
		Integer acActivePower = TypeUtils.sum(productionPower, dcDischargePower);

		/*
		 * Update AC Active Power
		 */
		IntegerReadChannel activePowerChannel = this.channel(this.activePowerChannelId);
		activePowerChannel.setNextValue(acActivePower);

		/*
		 * Calculate AC Energy
		 */
		if (acActivePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (acActivePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(acActivePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(acActivePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}

		/*
		 * Update DC Discharge Power
		 */
		IntegerReadChannel dcDischargePowerChannel = this.channel(this.dcDischargePowerChannelId);
		dcDischargePowerChannel.setNextValue(dcDischargePower);

		/*
		 * Calculate DC Energy
		 */
		if (dcDischargePower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else {
			if (dcDischargePower > 0) {
				// Discharge
				this.calculateDcChargeEnergy.update(0);
				this.calculateDcDischargeEnergy.update(dcDischargePower);
			} else {
				// Charge
				this.calculateDcChargeEnergy.update(dcDischargePower * -1);
				this.calculateDcDischargeEnergy.update(0);
			}
		}
	}

	/**
	 * TODO Gets Surplus Power.
	 * 
	 * @return {@link Integer}
	 */
	public abstract Integer getSurplusPower();
}
