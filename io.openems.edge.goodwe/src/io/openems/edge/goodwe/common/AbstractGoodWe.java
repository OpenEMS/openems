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
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
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
								value -> value))),

				new FC3ReadRegistersTask(35111, Priority.LOW, //
						m(GoodWe.ChannelId.V_PV3, new UnsignedWordElement(35111),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV3, new UnsignedWordElement(35112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35113, 35114), //
						m(GoodWe.ChannelId.V_PV4, new UnsignedWordElement(35115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV4, new UnsignedWordElement(35116),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35117, 35118), //
						m(GoodWe.ChannelId.PV_MODE, new UnsignedDoublewordElement(35119))), //

				new FC3ReadRegistersTask(35136, Priority.LOW, //
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
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

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
						m(GoodWe.ChannelId.WORK_MODE, new UnsignedWordElement(35187)), //
						m(GoodWe.ChannelId.OPERATION_MODE, new UnsignedDoublewordElement(35188))), //

				new FC3ReadRegistersTask(35206, Priority.LOW, //
						m(this.getDcChargeEnergyChannel(), new UnsignedDoublewordElement(35206), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(35208), //
						m(this.getDcDischargeEnergyChannel(), new UnsignedDoublewordElement(35209),
								ElementToChannelConverter.SCALE_FACTOR_2)), //

				new FC3ReadRegistersTask(36003, Priority.LOW, //
						m(GoodWe.ChannelId.B_METER_COMMUNICATE_STATUS, new UnsignedWordElement(36003)), //
						m(GoodWe.ChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004))), //

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

				new FC16WriteRegistersTask(45350, //
						m(GoodWe.ChannelId.LEAD_BAT_CAPACITY, new UnsignedWordElement(45350),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BATT_STRINGS, new UnsignedWordElement(45351)), //
						m(GoodWe.ChannelId.BATT_CHARGE_VOLT_MAX, new UnsignedWordElement(45352),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodWe.ChannelId.BATT_CHARGE_CURR_MAX, new UnsignedWordElement(45353),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(45354, //
						m(GoodWe.ChannelId.BATT_VOLT_UNDER_MIN, new UnsignedWordElement(45354), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC16WriteRegistersTask(45355, //
						m(GoodWe.ChannelId.BATT_DISCHARGE_CURR_MAX, new UnsignedWordElement(45355),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(45350, Priority.LOW, //
						m(GoodWe.ChannelId.LEAD_BAT_CAPACITY, new UnsignedWordElement(45350), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BATT_STRINGS, new UnsignedWordElement(45351)), //
						m(GoodWe.ChannelId.BATT_CHARGE_VOLT_MAX, new UnsignedWordElement(45352),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodWe.ChannelId.BATT_CHARGE_CURR_MAX, new UnsignedWordElement(45353),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(45354, Priority.HIGH, //
						m(GoodWe.ChannelId.BATT_VOLT_UNDER_MIN, new UnsignedWordElement(45354),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(45355, Priority.HIGH, //
						m(GoodWe.ChannelId.BATT_DISCHARGE_CURR_MAX, new UnsignedWordElement(45355),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC16WriteRegistersTask(47000, //
						m(GoodWe.ChannelId.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(GoodWe.ChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodWe.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC3ReadRegistersTask(47000, Priority.LOW, //
						m(GoodWe.ChannelId.APP_MODE_INDEX, new UnsignedWordElement(47000)), //
						m(GoodWe.ChannelId.METER_CHECK_VALUE, new UnsignedWordElement(47001)), //
						m(GoodWe.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002))), //

				new FC16WriteRegistersTask(47500, //
						m(GoodWe.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						new DummyRegisterElement(47501, 47508), //
						m(GoodWe.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWe.ChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510)), //
						m(GoodWe.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWe.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512))), //

				new FC16WriteRegistersTask(47531, //
						m(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						m(GoodWe.ChannelId.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533))), //

				new FC3ReadRegistersTask(47500, Priority.LOW,
						m(GoodWe.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						new DummyRegisterElement(47501, 47508), //
						m(GoodWe.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWe.ChannelId.FEED_POWER_PARA, new UnsignedWordElement(47510))), //

				new FC3ReadRegistersTask(47511, Priority.LOW,
						m(GoodWe.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWe.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512))), //

				new FC3ReadRegistersTask(47531, Priority.LOW,
						m(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532))), //

				new FC6WriteRegisterTask(47906, //
						m(GoodWe.ChannelId.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(47907, //
						m(GoodWe.ChannelId.WBMS_BAT_CURRENT, new UnsignedWordElement(47907))), //
				new FC6WriteRegisterTask(47908, //
						m(GoodWe.ChannelId.WBMS_BAT_SOC, new UnsignedWordElement(47908))), //
				new FC6WriteRegisterTask(47909, //
						m(GoodWe.ChannelId.WBMS_BAT_SOH, new UnsignedWordElement(47909))), //
				new FC6WriteRegisterTask(47910, //
						m(GoodWe.ChannelId.WBMS_BAT_TEMPERATURE, new SignedWordElement(47910),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(47900, Priority.LOW, //
						m(GoodWe.ChannelId.BMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodWe.ChannelId.BATT_STRINGS_RS485, new UnsignedWordElement(47901)), //
						m(GoodWe.ChannelId.WBMS_BAT_CHARGE_VMAX, new UnsignedWordElement(47902),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_BAT_CHARGE_IMAX, new UnsignedWordElement(47903),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_BAT_DISCHARGE_VMIN, new UnsignedWordElement(47904),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_BAT_DISCHARGE_IMAX, new UnsignedWordElement(47905),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_BAT_VOLTAGE, new UnsignedWordElement(47906),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_BAT_CURRENT, new UnsignedWordElement(47907)), //
						m(GoodWe.ChannelId.WBMS_BAT_SOC, new UnsignedWordElement(47908)), //
						m(GoodWe.ChannelId.WBMS_BAT_SOH, new UnsignedWordElement(47909)), //
						m(GoodWe.ChannelId.WBMS_BAT_TEMPERATURE, new SignedWordElement(47910),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(new BitsWordElement(47911, this) //
								.bit(0, GoodWe.ChannelId.STATE_58) //
								.bit(1, GoodWe.ChannelId.STATE_59) //
								.bit(2, GoodWe.ChannelId.STATE_60) //
								.bit(3, GoodWe.ChannelId.STATE_61) //
								.bit(4, GoodWe.ChannelId.STATE_62) //
								.bit(5, GoodWe.ChannelId.STATE_63) //
								.bit(6, GoodWe.ChannelId.STATE_64) //
								.bit(7, GoodWe.ChannelId.STATE_65) //
								.bit(8, GoodWe.ChannelId.STATE_66) //
								.bit(9, GoodWe.ChannelId.STATE_67) //
								.bit(10, GoodWe.ChannelId.STATE_68) //
								.bit(11, GoodWe.ChannelId.STATE_69)), //
						new DummyRegisterElement(47912), //
						m(new BitsWordElement(47913, this) //
								.bit(0, GoodWe.ChannelId.STATE_42) //
								.bit(1, GoodWe.ChannelId.STATE_43) //
								.bit(2, GoodWe.ChannelId.STATE_44) //
								.bit(3, GoodWe.ChannelId.STATE_45) //
								.bit(4, GoodWe.ChannelId.STATE_46) //
								.bit(5, GoodWe.ChannelId.STATE_47) //
								.bit(6, GoodWe.ChannelId.STATE_48) //
								.bit(7, GoodWe.ChannelId.STATE_49) //
								.bit(8, GoodWe.ChannelId.STATE_50) //
								.bit(9, GoodWe.ChannelId.STATE_51) //
								.bit(10, GoodWe.ChannelId.STATE_52) //
								.bit(11, GoodWe.ChannelId.STATE_53) //
								.bit(12, GoodWe.ChannelId.STATE_54) //
								.bit(13, GoodWe.ChannelId.STATE_55) //
								.bit(14, GoodWe.ChannelId.STATE_56) //
								.bit(15, GoodWe.ChannelId.STATE_57)), //
						new DummyRegisterElement(47914), //
						m(new BitsWordElement(47915, this) //
								.bit(0, GoodWe.ChannelId.STATE_79) //
								.bit(1, GoodWe.ChannelId.STATE_80) //
								.bit(2, GoodWe.ChannelId.STATE_81))));
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
}
