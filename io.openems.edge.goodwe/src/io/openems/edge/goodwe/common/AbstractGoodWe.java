package io.openems.edge.goodwe.common;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

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
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
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
	private final io.openems.edge.common.channel.ChannelId reactivePowerChannelId;
	private final io.openems.edge.common.channel.ChannelId dcDischargePowerChannelId;
	private final CalculateEnergyFromPower calculateAcChargeEnergy;
	private final CalculateEnergyFromPower calculateAcDischargeEnergy;
	private final CalculateEnergyFromPower calculateDcChargeEnergy;
	private final CalculateEnergyFromPower calculateDcDischargeEnergy;

	protected final Set<AbstractGoodWeEtCharger> chargers = new HashSet<>();

	protected AbstractGoodWe(//
			io.openems.edge.common.channel.ChannelId activePowerChannelId, //
			io.openems.edge.common.channel.ChannelId reactivePowerChannelId, //
			io.openems.edge.common.channel.ChannelId dcDischargePowerChannelId, //
			io.openems.edge.common.channel.ChannelId activeChargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId activeDischargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId dcChargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId dcDischargeEnergyChannelId, //
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsNamedException {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.activePowerChannelId = activePowerChannelId;
		this.reactivePowerChannelId = reactivePowerChannelId;
		this.dcDischargePowerChannelId = dcDischargePowerChannelId;
		this.calculateAcChargeEnergy = new CalculateEnergyFromPower(this, activeChargeEnergyChannelId);
		this.calculateAcDischargeEnergy = new CalculateEnergyFromPower(this, activeDischargeEnergyChannelId);
		this.calculateDcChargeEnergy = new CalculateEnergyFromPower(this, dcChargeEnergyChannelId);
		this.calculateDcDischargeEnergy = new CalculateEnergyFromPower(this, dcDischargeEnergyChannelId);
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var protocol = new ModbusProtocol(this, //

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
										// TODO add identification for FENECON branded inverter
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
										case "FHI-10-DAH":
											result = GoodweType.FENECON_FHI_10_DAH;
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
										case FENECON_FHI_10_DAH:
											this.logInfo(this.log, "Identified " + result.getName());
											break;
										case UNDEFINED:
											break;
										}
									}
									return result;
								}, //

								// channel -> element
								value -> value))

				), //

				new FC3ReadRegistersTask(35016, Priority.LOW, //
						m(GoodWe.ChannelId.DSP_FM_VERSION_MASTER, new UnsignedWordElement(35016)), //
						m(GoodWe.ChannelId.DSP_FM_VERSION_SLAVE, new UnsignedWordElement(35017)), //
						m(GoodWe.ChannelId.DSP_BETA_VERSION, new UnsignedWordElement(35018)), //
						m(GoodWe.ChannelId.ARM_FM_VERSION, new UnsignedWordElement(35019)), //
						m(GoodWe.ChannelId.ARM_BETA_VERSION, new UnsignedWordElement(35020)) //
				), //

				new FC3ReadRegistersTask(35111, Priority.LOW, //
						// Registers for PV1 and PV2 (35103 to 35110) are read via DC-Charger
						// implementation
						m(GoodWe.ChannelId.V_PV3, new UnsignedWordElement(35111), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV3, new UnsignedWordElement(35112), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.P_PV3, new UnsignedDoublewordElement(35113)), //
						m(GoodWe.ChannelId.V_PV4, new UnsignedWordElement(35115), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_PV4, new UnsignedWordElement(35116), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.P_PV4, new UnsignedDoublewordElement(35117)), //
						m(GoodWe.ChannelId.PV_MODE, new UnsignedDoublewordElement(35119)), //
						// Registers for Grid Smart-Meter (35121 to 35135) are read via GridMeter
						// implementation
						new DummyRegisterElement(35121, 35135),
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(35136), //
								new ElementToChannelConverter(value -> {
									Integer intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
									if (intValue != null) {
										switch (intValue) {
										case 0:
											return GridMode.UNDEFINED;
										case 1:
											return GridMode.ON_GRID;
										case 2:
											return GridMode.OFF_GRID;
										}
									}
									return GridMode.UNDEFINED;
								}))), //

				new FC3ReadRegistersTask(35137, Priority.LOW, //
						m(GoodWe.ChannelId.TOTAL_INV_POWER, new SignedDoublewordElement(35137)), //
						m(GoodWe.ChannelId.AC_ACTIVE_POWER, new SignedDoublewordElement(35139), //
								INVERT), //
						m(this.reactivePowerChannelId, new SignedDoublewordElement(35141), //
								INVERT), //
						m(GoodWe.ChannelId.AC_APPARENT_POWER, new SignedDoublewordElement(35143), //
								INVERT), //
						new DummyRegisterElement(35145, 35147), //
						m(GoodWe.ChannelId.LOAD_MODE_R, new UnsignedWordElement(35148)), //
						new DummyRegisterElement(35149, 35153), //
						m(GoodWe.ChannelId.LOAD_MODE_S, new UnsignedWordElement(35154)), //
						new DummyRegisterElement(35155, 35159), //
						m(GoodWe.ChannelId.LOAD_MODE_T, new UnsignedWordElement(35160)), //
						new DummyRegisterElement(35161, 35162), //
						m(GoodWe.ChannelId.P_LOAD_R, new SignedDoublewordElement(35163)), //
						m(GoodWe.ChannelId.P_LOAD_S, new SignedDoublewordElement(35165)), //
						m(GoodWe.ChannelId.P_LOAD_T, new SignedDoublewordElement(35167)), //
						m(GoodWe.ChannelId.TOTAL_BACK_UP_LOAD_POWER, new SignedDoublewordElement(35169)), //
						m(GoodWe.ChannelId.TOTAL_LOAD_POWER, new SignedDoublewordElement(35171)), //
						m(GoodWe.ChannelId.UPS_LOAD_PERCENT, new UnsignedWordElement(35173), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.AIR_TEMPERATURE, new SignedWordElement(35174), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MODULE_TEMPERATURE, new SignedWordElement(35175), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.RADIATOR_TEMPERATURE, new SignedWordElement(35176), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FUNCTION_BIT_VALUE, new UnsignedWordElement(35177)), //
						m(GoodWe.ChannelId.BUS_VOLTAGE, new UnsignedWordElement(35178), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.NBUS_VOLTAGE, new UnsignedWordElement(35179), SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(35180, Priority.HIGH, //
						m(GoodWe.ChannelId.V_BATTERY1, new UnsignedWordElement(35180), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.I_BATTERY1, new SignedWordElement(35181), SCALE_FACTOR_MINUS_1), //
						// Required for calculation of ActivePower; wrongly documented in official
						// Modbus protocol v1.9 as being Unsigned.
						m(GoodWe.ChannelId.P_BATTERY1, new SignedDoublewordElement(35182)),
						m(GoodWe.ChannelId.BATTERY_MODE, new UnsignedWordElement(35184))), //

				new FC3ReadRegistersTask(35186, Priority.LOW, //
						m(GoodWe.ChannelId.SAFETY_COUNTRY, new UnsignedWordElement(35186)), //
						m(GoodWe.ChannelId.WORK_MODE, new UnsignedWordElement(35187)), //
						new DummyRegisterElement(35188), //
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

						// The total PV production energy from installation
						m(GoodWe.ChannelId.PV_E_TOTAL, new UnsignedDoublewordElement(35191), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.PV_E_DAY, new UnsignedDoublewordElement(35193), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35195, 35196), //
						m(GoodWe.ChannelId.H_TOTAL, new UnsignedDoublewordElement(35197)), //
						m(GoodWe.ChannelId.E_DAY_SELL, new UnsignedWordElement(35199), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY, new UnsignedDoublewordElement(35200), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DAY_BUY, new UnsignedWordElement(35202), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_LOAD, new UnsignedDoublewordElement(35203), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_LOAD_DAY, new UnsignedWordElement(35205), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_CHARGE, new UnsignedDoublewordElement(35206), //
								SCALE_FACTOR_2), //
						m(GoodWe.ChannelId.E_CHARGE_DAY, new UnsignedWordElement(35208), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_DISCHARGE, new UnsignedDoublewordElement(35209), SCALE_FACTOR_2), //
						m(GoodWe.ChannelId.E_DISCHARGE_DAY, new UnsignedWordElement(35211), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BATTERY_STRINGS, new UnsignedWordElement(35212)), //
						m(GoodWe.ChannelId.CPLD_WARNING_CODE, new UnsignedWordElement(35213)), //
						new DummyRegisterElement(35214, 35217), //
						m(GoodWe.ChannelId.DIAG_STATUS_H, new UnsignedDoublewordElement(35218)), //
						m(GoodWe.ChannelId.DIAG_STATUS_L, new UnsignedDoublewordElement(35220)), //
						new DummyRegisterElement(35222, 35224), //
						m(GoodWe.ChannelId.EH_BATTERY_FUNCTION_ACTIVE, new UnsignedWordElement(35225)), //
						m(GoodWe.ChannelId.ARC_SELF_CHECK_STATUS, new UnsignedWordElement(35226)) //
				),

				new FC3ReadRegistersTask(35250, Priority.LOW, //
						m(new BitsWordElement(35250, this) //
								.bit(0, GoodWe.ChannelId.STATE_70) //
								.bit(1, GoodWe.ChannelId.STATE_71) //
								.bit(2, GoodWe.ChannelId.STATE_72) //
								.bit(3, GoodWe.ChannelId.STATE_73) //
								.bit(4, GoodWe.ChannelId.STATE_74) //
								.bit(5, GoodWe.ChannelId.STATE_75) //
								.bit(6, GoodWe.ChannelId.STATE_76) //
								.bit(7, GoodWe.ChannelId.STATE_77) //
								.bit(8, GoodWe.ChannelId.STATE_78) //
								.bit(9, GoodWe.ChannelId.STATE_79) //
								.bit(10, GoodWe.ChannelId.STATE_80) //
								.bit(11, GoodWe.ChannelId.STATE_81) //
								.bit(12, GoodWe.ChannelId.STATE_82) //
								.bit(13, GoodWe.ChannelId.STATE_83) //
								.bit(14, GoodWe.ChannelId.STATE_84) //
								.bit(15, GoodWe.ChannelId.STATE_85)), //
						m(new BitsWordElement(35251, this) //
								.bit(0, GoodWe.ChannelId.STATE_86) //
								.bit(1, GoodWe.ChannelId.STATE_87) //
								.bit(2, GoodWe.ChannelId.STATE_88) //
								.bit(3, GoodWe.ChannelId.STATE_89) //
								.bit(4, GoodWe.ChannelId.STATE_90) //
								.bit(5, GoodWe.ChannelId.STATE_91) //
								.bit(6, GoodWe.ChannelId.STATE_92) //
								.bit(7, GoodWe.ChannelId.STATE_93) //
						), //
						new DummyRegisterElement(35252, 35253), //
						m(new BitsWordElement(35254, this) //
								.bit(0, GoodWe.ChannelId.STATE_94) //
								.bit(1, GoodWe.ChannelId.STATE_95) //
								.bit(2, GoodWe.ChannelId.STATE_96) //
								.bit(3, GoodWe.ChannelId.STATE_97) //
								.bit(4, GoodWe.ChannelId.STATE_98) //
								.bit(5, GoodWe.ChannelId.STATE_99) //
								.bit(6, GoodWe.ChannelId.STATE_100) //
								.bit(7, GoodWe.ChannelId.STATE_101) //
								.bit(8, GoodWe.ChannelId.STATE_102) //
								.bit(9, GoodWe.ChannelId.STATE_103) //
								.bit(10, GoodWe.ChannelId.STATE_104) //
								.bit(11, GoodWe.ChannelId.STATE_105) //
								.bit(12, GoodWe.ChannelId.STATE_106) //
								.bit(13, GoodWe.ChannelId.STATE_107) //
								.bit(14, GoodWe.ChannelId.STATE_108) //
								.bit(15, GoodWe.ChannelId.STATE_109)), //
						m(new BitsWordElement(35255, this) //
								.bit(0, GoodWe.ChannelId.STATE_110) //
								.bit(1, GoodWe.ChannelId.STATE_111) //
								.bit(2, GoodWe.ChannelId.STATE_112) //
								.bit(3, GoodWe.ChannelId.STATE_113) //
								.bit(4, GoodWe.ChannelId.STATE_114) //
								.bit(5, GoodWe.ChannelId.STATE_115) //
								.bit(6, GoodWe.ChannelId.STATE_116) //
						), //
						new DummyRegisterElement(35256, 35257), //
						m(new BitsWordElement(35258, this) //
								.bit(0, GoodWe.ChannelId.STATE_117) //
								.bit(1, GoodWe.ChannelId.STATE_118) //
								.bit(2, GoodWe.ChannelId.STATE_119) //
								.bit(3, GoodWe.ChannelId.STATE_120) //
								.bit(4, GoodWe.ChannelId.STATE_121) //
								.bit(5, GoodWe.ChannelId.STATE_122) //
								.bit(6, GoodWe.ChannelId.STATE_123) //
								.bit(7, GoodWe.ChannelId.STATE_124) //
								.bit(8, GoodWe.ChannelId.STATE_125) //
								.bit(9, GoodWe.ChannelId.STATE_126) //
								.bit(10, GoodWe.ChannelId.STATE_127) //
								.bit(11, GoodWe.ChannelId.STATE_128) //
								.bit(12, GoodWe.ChannelId.STATE_129) //
								.bit(13, GoodWe.ChannelId.STATE_130) //
								.bit(14, GoodWe.ChannelId.STATE_131) //
								.bit(15, GoodWe.ChannelId.STATE_132)), //
						m(new BitsWordElement(35259, this) //
								.bit(0, GoodWe.ChannelId.STATE_133) //
								.bit(1, GoodWe.ChannelId.STATE_134) //
								.bit(2, GoodWe.ChannelId.STATE_135) //
								.bit(3, GoodWe.ChannelId.STATE_136) //
								.bit(4, GoodWe.ChannelId.STATE_137) //
						), //
						new DummyRegisterElement(35260, 35267), //
						m(GoodWe.ChannelId.MAX_GRID_FREQ_WITHIN_1_MINUTE, new UnsignedWordElement(35268),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.MIN_GRID_FREQ_WITHIN_1_MINUTE, new UnsignedWordElement(35269),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.MAX_GRID_VOLTAGE_WITHIN_1_MINUTE_R, new UnsignedWordElement(35270),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MIN_GRID_VOLTAGE_WITHIN_1_MINUTE_R, new UnsignedWordElement(35271),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MAX_GRID_VOLTAGE_WITHIN_1_MINUTE_S, new UnsignedWordElement(35272),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MIN_GRID_VOLTAGE_WITHIN_1_MINUTE_S, new UnsignedWordElement(35273),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MAX_GRID_VOLTAGE_WITHIN_1_MINUTE_T, new UnsignedWordElement(35274),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MIN_GRID_VOLTAGE_WITHIN_1_MINUTE_T, new UnsignedWordElement(35275),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MAX_BACKUP_POWER_WITHIN_1_MINUTE_R, new UnsignedDoublewordElement(35276)), //
						m(GoodWe.ChannelId.MAX_BACKUP_POWER_WITHIN_1_MINUTE_S, new UnsignedDoublewordElement(35278)), //
						m(GoodWe.ChannelId.MAX_BACKUP_POWER_WITHIN_1_MINUTE_T, new UnsignedDoublewordElement(35280)), //
						m(GoodWe.ChannelId.MAX_BACKUP_POWER_WITHIN_1_MINUTE_TOTAL,
								new UnsignedDoublewordElement(35282)), //
						m(GoodWe.ChannelId.GRID_HVRT_EVENT_TIMES, new UnsignedWordElement(35284)), //
						m(GoodWe.ChannelId.GRID_LVRT_EVENT_TIMES, new UnsignedWordElement(35285)), //
						m(GoodWe.ChannelId.INV_ERROR_MSG_RECORD_FOR_EMS, new UnsignedDoublewordElement(35286)), //
						m(GoodWe.ChannelId.INV_WARNING_CODE_RECORD_FOR_EMS, new UnsignedDoublewordElement(35288)), //
						m(GoodWe.ChannelId.INV_CPLD_WARNING_RECORD_FOR_EMS, new UnsignedDoublewordElement(35290)) //
				),

				// Registers 36066 to 36120 throw "Illegal Data Address"

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
						m(GoodWe.ChannelId.BMS_PACK_TEMPERATURE, new UnsignedWordElement(37003), SCALE_FACTOR_MINUS_1), //
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
						this.getSocModbusElement(37007), //
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
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MINIMUM_CELL_TEMPERATURE, new UnsignedWordElement(37021),
								SCALE_FACTOR_MINUS_1), //
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

				// Registers 40000 to 42011 for BTC and ETC throw "Illegal Data Address"

				// Setting and Controlling Data Registers
				new FC3ReadRegistersTask(45127, Priority.LOW, //
						m(GoodWe.ChannelId.INVERTER_UNIT_ID, new UnsignedWordElement(45127)), //
						new DummyRegisterElement(45128, 45131), //
						m(GoodWe.ChannelId.MODBUS_BAUDRATE, new UnsignedDoublewordElement(45132))), //

				new FC3ReadRegistersTask(45222, Priority.LOW, //
						// to read or write the accumulated energy battery discharged, of the day Not
						// from BMS
						m(GoodWe.ChannelId.PV_E_TOTAL_2, new UnsignedDoublewordElement(45222), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.PV_E_DAY_2, new UnsignedDoublewordElement(45224), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_SELL_2, new UnsignedDoublewordElement(45226), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.H_TOTAL_2, new UnsignedDoublewordElement(45228)), //
						m(GoodWe.ChannelId.E_DAY_SELL_2, new UnsignedWordElement(45230), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY_2, new UnsignedDoublewordElement(45231), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DAY_BUY_2, new UnsignedWordElement(45233), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_LOAD_2, new UnsignedDoublewordElement(45234), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_LOAD_DAY_2, new UnsignedWordElement(45236), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_CHARGE_2, new UnsignedDoublewordElement(45237),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_CHARGE_DAY_2, new UnsignedWordElement(45239), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_BATTERY_DISCHARGE_2, new UnsignedDoublewordElement(45240),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_DISCHARGE_DAY_2, new UnsignedWordElement(45242), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(45243), //
						// to set safety code for inverter or read the preset safety code for the
						// inverter
						m(GoodWe.ChannelId.SAFETY_COUNTRY_CODE, new UnsignedWordElement(45244)), //
						m(GoodWe.ChannelId.ISO_LIMIT, new UnsignedWordElement(45245)), //
						m(GoodWe.ChannelId.LVRT_HVRT, new UnsignedWordElement(45246))), //

				new FC3ReadRegistersTask(45250, Priority.LOW, //
						m(GoodWe.ChannelId.PV_START_VOLTAGE, new UnsignedWordElement(45250), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.MPPT_FOR_SHADOW_ENABLE, new UnsignedWordElement(45251)), //
						m(GoodWe.ChannelId.BACK_UP_ENABLE, new UnsignedWordElement(45252)), //
						m(GoodWe.ChannelId.AUTO_START_BACKUP, new UnsignedWordElement(45253)), //
						m(GoodWe.ChannelId.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(45254)), //
						new DummyRegisterElement(45255), //
						m(GoodWe.ChannelId.BACKUP_START_DLY, new UnsignedWordElement(45256)), //
						m(GoodWe.ChannelId.UPS_STD_VOLT_TYPE, new UnsignedWordElement(45257)), //
						new DummyRegisterElement(45258, 45262), //
						// Only can set 70, only for German
						m(GoodWe.ChannelId.DERATE_RATE_VDE, new UnsignedWordElement(45263)), //
						// this function is deactivated as default, set "1" to activate. After
						// activated, All power needs to be turned off and restarted
						m(GoodWe.ChannelId.THREE_PHASE_UNBALANCED_OUTPUT, new UnsignedWordElement(45264)), //
						new DummyRegisterElement(45265), //
						// For weak grid area
						m(GoodWe.ChannelId.HIGH_IMP_MODE, new UnsignedWordElement(45266)), //
						new DummyRegisterElement(45267, 45274), //
						// 0:Normal mode 1: cancel ISO test when offgrid to ongrid
						m(GoodWe.ChannelId.ISO_CHECK_MODE, new UnsignedWordElement(45275)), //
						// The delay time when grid is available
						m(GoodWe.ChannelId.OFF_GRID_TO_ON_GRID_DELAY, new UnsignedWordElement(45276)), //
						// If set 80%, when offgrid output voltage less than 230*80%=184V, inverter will
						// have the error.
						m(GoodWe.ChannelId.OFF_GRID_UNDER_VOLTAGE_PROTECT_COEFFICIENT, new UnsignedWordElement(45277)), //
						// When offgrid and the battery SOC is low, PV charge the battery
						m(GoodWe.ChannelId.BATTERY_MODE_PV_CHARGE_ENABLE, new UnsignedWordElement(45278)), //
						// Default fisresttt.ing is 1
						m(GoodWe.ChannelId.DCV_CHECK_OFF, new UnsignedWordElement(45279))//

				), //

				// Registers 45333 to 45339 for License throw "Illegal Data Address"

				new FC3ReadRegistersTask(45352, Priority.LOW, //
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(45352),
								SCALE_FACTOR_MINUS_1), // [500*N,600*N]
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(45353),
								SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45354),
								SCALE_FACTOR_MINUS_1), // [400*N,480*N]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(45355),
								SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_SOC_UNDER_MIN, new UnsignedWordElement(45356)), // [0,100]
						m(GoodWe.ChannelId.BMS_OFFLINE_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45357),
								SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(45358))), //

				// Safety
				new FC3ReadRegistersTask(45400, Priority.LOW, //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GoodWe.ChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45421)), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(45426)), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_TIME, new UnsignedWordElement(45427)), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428)), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429)), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430)), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431)) //
				), //

				new FC3ReadRegistersTask(45428, Priority.LOW, //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431))), //

				// Cos Phi Curve
				new FC3ReadRegistersTask(45432, Priority.LOW, //
						m(GoodWe.ChannelId.POWER_SLOPE_ENABLE, new UnsignedWordElement(45432)), //
						m(GoodWe.ChannelId.ENABLE_CURVE_PU, new UnsignedWordElement(45433)), //
						m(GoodWe.ChannelId.A_POINT_POWER, new SignedWordElement(45434)), //
						m(GoodWe.ChannelId.A_POINT_COS_PHI, new SignedWordElement(45435), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.B_POINT_POWER, new SignedWordElement(45436)), //
						m(GoodWe.ChannelId.B_POINT_COS_PHI, new SignedWordElement(45437), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_POINT_POWER, new SignedWordElement(45438)), //
						m(GoodWe.ChannelId.C_POINT_COS_PHI, new SignedWordElement(45439)),
						m(GoodWe.ChannelId.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER, new SignedWordElement(45442)), //

						// Power and frequency curve
						m(new BitsWordElement(45443, this)//
								.bit(0, GoodWe.ChannelId.POWER_FREQUENCY_ENABLED)//
								.bit(1, GoodWe.ChannelId.POWER_FREQUENCY_RESPONSE_MODE)//
						), //
						m(GoodWe.ChannelId.FFROZEN_DCH, new UnsignedWordElement(45444), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH, new UnsignedWordElement(45445), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_DCH, new UnsignedWordElement(45446), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_CH, new UnsignedWordElement(45447), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.OF_RECOVERY_WAITING_TIME, new UnsignedWordElement(45448),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.OF_RECOVERY_SLOPE, new UnsignedWordElement(45451), //
								new ChannelMetaInfoReadAndWrite(45451, 45452)), //
						m(GoodWe.ChannelId.CFP_SETTINGS, new UnsignedWordElement(45452), //
								new ChannelMetaInfoReadAndWrite(45452, 45451)), //
						m(GoodWe.ChannelId.CFP_OF_SLOPE_PERCENT, new UnsignedWordElement(45453), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_UF_SLOPE_PERCENT, new UnsignedWordElement(45454), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_OF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45455))), //

				// QU Curve
				new FC3ReadRegistersTask(45456, Priority.LOW, //
						m(GoodWe.ChannelId.QU_CURVE, new UnsignedWordElement(45456)), //
						m(GoodWe.ChannelId.LOCK_IN_POWER_QU, new SignedWordElement(45457)), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER_QU, new SignedWordElement(45458)), //
						m(GoodWe.ChannelId.V1_VOLTAGE, new UnsignedWordElement(45459), SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.V1_VALUE, new SignedWordElement(45460)), //
						m(GoodWe.ChannelId.V2_VOLTAGE, new UnsignedWordElement(45461), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE, new SignedWordElement(45462)), //
						m(GoodWe.ChannelId.V3_VOLTAGE, new UnsignedWordElement(45463), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE, new SignedWordElement(45464)), //
						m(GoodWe.ChannelId.V4_VOLTAGE, new UnsignedWordElement(45465), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE, new SignedWordElement(45466)), //
						m(GoodWe.ChannelId.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodWe.ChannelId.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodWe.ChannelId.MISCELLANEA, new UnsignedWordElement(45469))), //

				// PU Curve
				new FC3ReadRegistersTask(45472, Priority.LOW, //
						m(GoodWe.ChannelId.PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodWe.ChannelId.POWER_CHANGE_RATE, new UnsignedWordElement(45473)), //
						m(GoodWe.ChannelId.V1_VOLTAGE_PU, new UnsignedWordElement(45474), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE_PU, new SignedWordElement(45475), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VOLTAGE_PU, new UnsignedWordElement(45476), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE_PU, new SignedWordElement(45477), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VOLTAGE_PU, new UnsignedWordElement(45478), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE_PU, new SignedWordElement(45479), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VOLTAGE_PU, new UnsignedWordElement(45480), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE_PU, new SignedWordElement(45481), SCALE_FACTOR_MINUS_1), //
						// 80=Pf 0.8, 20= -0.8Pf
						m(GoodWe.ChannelId.FIXED_POWER_FACTOR, new UnsignedWordElement(45482), SCALE_FACTOR_MINUS_2), //
						// Set the percentage of rated power of the inverter
						m(GoodWe.ChannelId.FIXED_REACTIVE_POWER, new SignedWordElement(45483), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484), SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(45488, Priority.LOW, //
						m(GoodWe.ChannelId.AUTO_TEST_ENABLE, new UnsignedWordElement(45488)), //
						m(GoodWe.ChannelId.AUTO_TEST_STEP, new UnsignedWordElement(45489)), //
						m(GoodWe.ChannelId.UW_ITALY_FREQ_MODE, new UnsignedWordElement(45490)), //
						// this must be turned off to do Meter test . "1" means Off
						m(GoodWe.ChannelId.ALL_POWER_CURVE_DISABLE, new UnsignedWordElement(45491)), //
						m(GoodWe.ChannelId.R_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45492)), //
						m(GoodWe.ChannelId.S_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45493)), //
						m(GoodWe.ChannelId.T_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45494)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3, new UnsignedWordElement(45495), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3_TIME, new UnsignedWordElement(45496)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3, new UnsignedWordElement(45497), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3_TIME, new UnsignedWordElement(45498)), //
						m(GoodWe.ChannelId.ZVRT_CONFIG, new UnsignedWordElement(45499)), //
						m(GoodWe.ChannelId.LVRT_START_VOLT, new UnsignedWordElement(45500), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_END_VOLT, new UnsignedWordElement(45501), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_START_TRIP_TIME, new UnsignedWordElement(45502)), //
						m(GoodWe.ChannelId.LVRT_END_TRIP_TIME, new UnsignedWordElement(45503)), //
						m(GoodWe.ChannelId.LVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45504), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_VOLT, new UnsignedWordElement(45505), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_END_VOLT, new UnsignedWordElement(45506), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_TRIP_TIME, new UnsignedWordElement(45507)), //
						m(GoodWe.ChannelId.HVRT_END_TRIP_TIME, new UnsignedWordElement(45508)), //
						m(GoodWe.ChannelId.HVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45509), SCALE_FACTOR_MINUS_1)//
				), //

				// Additional settings for PF/PU/UF
				new FC3ReadRegistersTask(45510, Priority.LOW, //
						m(GoodWe.ChannelId.PF_TIME_CONSTANT, new UnsignedWordElement(45510)), //
						m(GoodWe.ChannelId.POWER_FREQ_TIME_CONSTANT, new UnsignedWordElement(45511)), //
						// Additional settings for P(U) Curve
						m(GoodWe.ChannelId.PU_TIME_CONSTANT, new UnsignedWordElement(45512)), //
						m(GoodWe.ChannelId.D_POINT_POWER, new SignedWordElement(45513)), //
						m(GoodWe.ChannelId.D_POINT_COS_PHI, new SignedWordElement(45514)), //
						// Additional settings for UF Curve
						m(GoodWe.ChannelId.UF_RECOVERY_WAITING_TIME, new UnsignedWordElement(45515),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.UF_RECOVER_SLOPE, new UnsignedWordElement(45516)), //
						m(GoodWe.ChannelId.CFP_UF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45517)), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT, new UnsignedWordElement(45518), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT_RECONNECT, new UnsignedWordElement(45519),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_UF_CHARGE_STOP, new UnsignedWordElement(45520), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_OF_DISCHARGE_STOP, new UnsignedWordElement(45521),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_TWOSSTEPF_FLG, new UnsignedWordElement(45522))//
				), //

				new FC3ReadRegistersTask(47500, Priority.LOW, //
						m(GoodWe.ChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(47500)), //
						m(GoodWe.ChannelId.BMS_FLOAT_VOLT, new UnsignedWordElement(47501), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_FLOAT_CURRENT, new UnsignedWordElement(47502)), //
						m(GoodWe.ChannelId.BMS_FLOAT_TIME, new UnsignedWordElement(47503)), //
						m(GoodWe.ChannelId.BMS_TYPE_INDEX_ARM, new UnsignedWordElement(47504)), //
						m(GoodWe.ChannelId.MANUFACTURE_CODE, new UnsignedWordElement(47505)), //
						m(GoodWe.ChannelId.DC_VOLT_OUTPUT, new UnsignedWordElement(47506)), //
						m(GoodWe.ChannelId.BMS_AVG_CHG_VOLT, new UnsignedWordElement(47507), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.BMS_AVG_CHG_HOURS, new UnsignedWordElement(47508)), //
						m(GoodWe.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWe.ChannelId.FEED_POWER_PARA_SET, new UnsignedWordElement(47510)), //
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

				new FC16WriteRegistersTask(45216, //
						// Choose "Warehouse" safety code first and then Set "1" to factory settings
						m(GoodWe.ChannelId.FACTORY_SETTING, new UnsignedWordElement(45216)), //
						// Reset inverter accumulated data like E-total, E-day, error log running data
						// etc
						m(GoodWe.ChannelId.CLEAR_DATA, new UnsignedWordElement(45217)), //
						new DummyRegisterElement(45218, 45219), //
						// Inverter will re-check and reconnect to utility again. Inverter does not
						// shutdown
						m(GoodWe.ChannelId.RESTART, new UnsignedWordElement(45220)), //
						// inverter will total shutdown and wake up again
						m(GoodWe.ChannelId.RESET_SPS, new UnsignedWordElement(45221)), //
						m(GoodWe.ChannelId.PV_E_TOTAL_2, new UnsignedDoublewordElement(45222), SCALE_FACTOR_MINUS_1), //
						// to read or write the total PV production energy of the day
						m(GoodWe.ChannelId.PV_E_DAY_2, new UnsignedDoublewordElement(45224), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated exporting energy to grid from the
						// installation date
						m(GoodWe.ChannelId.E_TOTAL_SELL_2, new UnsignedDoublewordElement(45226), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated operation hours from the installation date
						m(GoodWe.ChannelId.H_TOTAL_2, new UnsignedDoublewordElement(45228)), //
						// to read or write the accumulated exporting energy to grid of the day
						m(GoodWe.ChannelId.E_DAY_SELL_2, new UnsignedWordElement(45230), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated energy imported from grid from the
						// installation date
						m(GoodWe.ChannelId.E_TOTAL_BUY_2, new UnsignedDoublewordElement(45231), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated energy imported from grid of the day
						m(GoodWe.ChannelId.E_DAY_BUY_2, new UnsignedWordElement(45233), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated load consumption energy from the
						// installation date, not include backup load.
						m(GoodWe.ChannelId.E_TOTAL_LOAD_2, new UnsignedDoublewordElement(45234), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated load consumption energy of the day Not
						// include backup loads
						m(GoodWe.ChannelId.E_LOAD_DAY_2, new UnsignedWordElement(45236), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated energy charged to battery from the
						// installation date Not from BMS
						m(GoodWe.ChannelId.E_BATTERY_CHARGE_2, new UnsignedDoublewordElement(45237),
								SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated energy charged to battery of the day Not
						// from BMS
						m(GoodWe.ChannelId.E_CHARGE_DAY_2, new UnsignedWordElement(45239), SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated energy battery discharged, from the
						// installation date Not from BMS
						m(GoodWe.ChannelId.E_BATTERY_DISCHARGE_2, new UnsignedDoublewordElement(45240),
								SCALE_FACTOR_MINUS_1), //
						// to read or write the accumulated energy battery discharged, of the day Not
						// from BMS
						m(GoodWe.ChannelId.E_DISCHARGE_DAY_2, new UnsignedWordElement(45242), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(45243), //
						// to set safety code for inverter or read the preset safety code for the
						// inverter
						m(GoodWe.ChannelId.SAFETY_COUNTRY_CODE, new UnsignedWordElement(45244)), //
						// default 100 kilo Ohm, to read or set Isolation protection threshold for the
						// inverter
						m(GoodWe.ChannelId.ISO_LIMIT, new UnsignedWordElement(45245)), //
						// as default is deactivated, set "1" to activate LVRT function, Set "2" to
						// activate HVRT, The same as 45499
						m(GoodWe.ChannelId.LVRT_HVRT, new UnsignedWordElement(45246))), //
				new FC16WriteRegistersTask(45250, //
						// to write or read the start up PV voltage of the inverter.Please refer to the
						// user manual
						m(GoodWe.ChannelId.PV_START_VOLTAGE, new UnsignedWordElement(45250), SCALE_FACTOR_MINUS_1), //
						// as default is deactivated, set "1" to activate "Shadow Scan" function
						m(GoodWe.ChannelId.MPPT_FOR_SHADOW_ENABLE, new UnsignedWordElement(45251)), //
						// as default is deactivated, set "1" to activate "Shadow Scan" function
						m(GoodWe.ChannelId.BACK_UP_ENABLE, new UnsignedWordElement(45252)), //
						// Off-Grid Auto startup, as default is deactivated, set "1" to activate "Shadow
						// Scan" function
						m(GoodWe.ChannelId.AUTO_START_BACKUP, new UnsignedWordElement(45253)), //
						// As default is "0"
						m(GoodWe.ChannelId.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(45254)), //
						new DummyRegisterElement(45255), //
						// Default is 1500 (30s)
						m(GoodWe.ChannelId.BACKUP_START_DLY, new UnsignedWordElement(45256)), //
						m(GoodWe.ChannelId.UPS_STD_VOLT_TYPE, new UnsignedWordElement(45257)), //
						new DummyRegisterElement(45258, 45262), //
						// Only can set 70, only for German
						m(GoodWe.ChannelId.DERATE_RATE_VDE, new UnsignedWordElement(45263)), //
						// This function is deactivated as default, set "1" to activate. After
						// activated, All power needs to be turned off and restarted
						m(GoodWe.ChannelId.THREE_PHASE_UNBALANCED_OUTPUT, new UnsignedWordElement(45264)), //
						new DummyRegisterElement(45265), //
						// For weak grid area
						m(GoodWe.ChannelId.HIGH_IMP_MODE, new UnsignedWordElement(45266)), //
						new DummyRegisterElement(45267, 45270), //
						// only for inverters with AFCI function
						m(GoodWe.ChannelId.ARC_SELF_CHECK, new UnsignedWordElement(45271)), //
						// only for inverters with AFCI function
						m(GoodWe.ChannelId.ARC_FAULT_REMOVE, new UnsignedWordElement(45272)), //
						new DummyRegisterElement(45273, 45274), //
						// 0:Normal mode 1: cancel ISO test when offgrid to ongrid
						m(GoodWe.ChannelId.ISO_CHECK_MODE, new UnsignedWordElement(45275)), //
						// The delay time when grid is available
						m(GoodWe.ChannelId.OFF_GRID_TO_ON_GRID_DELAY, new UnsignedWordElement(45276)), //
						// If set 80%, when offgrid output voltage less than 230*80%=184V, inverter will
						// have the error.Default setting is
						m(GoodWe.ChannelId.OFF_GRID_UNDER_VOLTAGE_PROTECT_COEFFICIENT, new UnsignedWordElement(45277)), //
						// When offgrid and the battery SOC is low, PV charge the battery
						m(GoodWe.ChannelId.BATTERY_MODE_PV_CHARGE_ENABLE, new UnsignedWordElement(45278)), //
						// Default setting 1, [1,20]
						m(GoodWe.ChannelId.DCV_CHECK_OFF, new UnsignedWordElement(45279))), //

				// These registers is to set the protection parameters on battery
				// charge/discharge operation ON INVERTER SIDE. The real
				// operation will still follow battery BMS limitations (or registers
				// 47900~47916) if it is not out of the range.Eg. Set BattChargeCurrMax (45353)
				// as 25A, but battery BMS limit the max charge current as 20A, then the battery
				// charge at max 20A. but if battery BMS limit max charge current as 50A,then
				// the real charge current of the battery will exceed 25A.
				new FC16WriteRegistersTask(45352, //
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(45352),
								SCALE_FACTOR_MINUS_1), // [500*N,600*N]
						m(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(45353),
								SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45354),
								SCALE_FACTOR_MINUS_1), // [400*N,480*N]
						m(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(45355),
								SCALE_FACTOR_MINUS_1), // [0,1000]
						m(GoodWe.ChannelId.BMS_SOC_UNDER_MIN, new UnsignedWordElement(45356)), // [0,100]
						m(GoodWe.ChannelId.BMS_OFFLINE_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(45357),
								SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(45358))), //

				// Safety Parameters
				new FC16WriteRegistersTask(45400, //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GoodWe.ChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						// Connect voltage
						m(GoodWe.ChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45421)), //
						// Reconnect voltage
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(45426)), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_TIME, new UnsignedWordElement(45427)), //
						// Power rate limit
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //
						m(GoodWe.ChannelId.POWER_SLOPE_ENABLE, new UnsignedWordElement(45432))), //

				// Cos Phi Curve
				new FC16WriteRegistersTask(45433, //
						m(GoodWe.ChannelId.ENABLE_CURVE_PU, new UnsignedWordElement(45433)), //
						m(GoodWe.ChannelId.A_POINT_POWER, new SignedWordElement(45434)), //
						m(GoodWe.ChannelId.A_POINT_COS_PHI, new SignedWordElement(45435), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.B_POINT_POWER, new SignedWordElement(45436)), //
						m(GoodWe.ChannelId.B_POINT_COS_PHI, new SignedWordElement(45437), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_POINT_POWER, new SignedWordElement(45438)), //
						m(GoodWe.ChannelId.C_POINT_COS_PHI, new SignedWordElement(45439), SCALE_FACTOR_MINUS_2), // ),
																													// //
						m(GoodWe.ChannelId.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER, new SignedWordElement(45442))), //

				// Power and frequency curve
				// m(new BitsWordElement(45443, this)//
				// .bit(0, GoodWe.ChannelId.STATE_70)//
				// .bit(1, GoodWe.ChannelId.STATE_71)), //

				// Power and frequency curve
				new FC16WriteRegistersTask(45444, //
						m(GoodWe.ChannelId.FFROZEN_DCH, new UnsignedWordElement(45444), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH, new UnsignedWordElement(45445), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_DCH, new UnsignedWordElement(45446), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_CH, new UnsignedWordElement(45447), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_WAITING_TIME, new UnsignedWordElement(45448)), //
						m(GoodWe.ChannelId.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_SETTINGS, new UnsignedWordElement(45451), //
								new ChannelMetaInfoReadAndWrite(45452, 45451)), //
						m(GoodWe.ChannelId.OF_RECOVERY_SLOPE, new UnsignedWordElement(45452), //
								new ChannelMetaInfoReadAndWrite(45451, 45452)), //
						m(GoodWe.ChannelId.CFP_OF_SLOPE_PERCENT, new UnsignedWordElement(45453), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_UF_SLOPE_PERCENT, new UnsignedWordElement(45454), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_OF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45455)), //

						// QU Curve
						m(GoodWe.ChannelId.QU_CURVE, new UnsignedWordElement(45456)), //
						m(GoodWe.ChannelId.LOCK_IN_POWER_QU, new SignedWordElement(45457)), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER_QU, new SignedWordElement(45458)), //
						m(GoodWe.ChannelId.V1_VOLTAGE, new UnsignedWordElement(45459), SCALE_FACTOR_MINUS_1), // ), //
						m(GoodWe.ChannelId.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodWe.ChannelId.V2_VOLTAGE, new UnsignedWordElement(45461), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodWe.ChannelId.V3_VOLTAGE, new UnsignedWordElement(45463), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodWe.ChannelId.V4_VOLTAGE, new UnsignedWordElement(45465), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE, new SignedWordElement(45466)), //
						m(GoodWe.ChannelId.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodWe.ChannelId.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodWe.ChannelId.MISCELLANEA, new UnsignedWordElement(45469))), //

				// PU Curve
				new FC16WriteRegistersTask(45472, //
						m(GoodWe.ChannelId.PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodWe.ChannelId.POWER_CHANGE_RATE, new UnsignedWordElement(45473), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.V1_VOLTAGE_PU, new UnsignedWordElement(45474), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE_PU, new SignedWordElement(45475)), //
						m(GoodWe.ChannelId.V2_VOLTAGE_PU, new UnsignedWordElement(45476), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE_PU, new SignedWordElement(45477)), //
						m(GoodWe.ChannelId.V3_VOLTAGE_PU, new UnsignedWordElement(45478), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE_PU, new SignedWordElement(45479)), //
						m(GoodWe.ChannelId.V4_VOLTAGE_PU, new UnsignedWordElement(45480), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE_PU, new SignedWordElement(45481)), //
						// 80=Pf 0.8, 20= -0.8Pf
						m(GoodWe.ChannelId.FIXED_POWER_FACTOR, new UnsignedWordElement(45482)), // [0,20]||[80,100]
						// Set the percentage of rated power of the inverter
						m(GoodWe.ChannelId.FIXED_REACTIVE_POWER, new SignedWordElement(45483)), // [-600,600]
						m(GoodWe.ChannelId.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484)), // [0,1000]
						new DummyRegisterElement(45485, 45490), //
						// This must be turned off to do Meter test . "1" means Off
						m(GoodWe.ChannelId.ALL_POWER_CURVE_DISABLE, new UnsignedWordElement(45491)), //
						// if it is 1-phase inverter, then use only R phase. Unbalance output function
						// must be turned on to set different values for R/S/T phases
						m(GoodWe.ChannelId.R_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45492)), //
						m(GoodWe.ChannelId.S_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45493)), //
						m(GoodWe.ChannelId.T_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45494)), //
						// only for countries where it needs 3-stage grid voltage
						// protection, Eg. Czech Republic
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3, new UnsignedWordElement(45495), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3_TIME, new UnsignedWordElement(45496)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3, new UnsignedWordElement(45497), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3_TIME, new UnsignedWordElement(45498)), //

						// For ZVRT, LVRT, HVRT
						m(GoodWe.ChannelId.ZVRT_CONFIG, new UnsignedWordElement(45499)), //
						m(GoodWe.ChannelId.LVRT_START_VOLT, new UnsignedWordElement(45500), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_END_VOLT, new UnsignedWordElement(45501), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_START_TRIP_TIME, new UnsignedWordElement(45502)), //
						m(GoodWe.ChannelId.LVRT_END_TRIP_TIME, new UnsignedWordElement(45503)), //
						m(GoodWe.ChannelId.LVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45504), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_VOLT, new UnsignedWordElement(45505), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_END_VOLT, new UnsignedWordElement(45506), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_TRIP_TIME, new UnsignedWordElement(45507)), //
						m(GoodWe.ChannelId.HVRT_END_TRIP_TIME, new UnsignedWordElement(45508)), //
						m(GoodWe.ChannelId.HVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45509), SCALE_FACTOR_MINUS_1)//
				), //

				// Additional settings for PF/PU/UF
				new FC16WriteRegistersTask(45510, //
						m(GoodWe.ChannelId.PF_TIME_CONSTANT, new UnsignedWordElement(45510)), //
						m(GoodWe.ChannelId.POWER_FREQ_TIME_CONSTANT, new UnsignedWordElement(45511)), //
						// Additional settings for P(U) Curve
						m(GoodWe.ChannelId.PU_TIME_CONSTANT, new UnsignedWordElement(45512)), //
						m(GoodWe.ChannelId.D_POINT_POWER, new SignedWordElement(45513)), //
						m(GoodWe.ChannelId.D_POINT_COS_PHI, new SignedWordElement(45514)), //
						// Additional settings for UF Curve
						m(GoodWe.ChannelId.UF_RECOVERY_WAITING_TIME, new UnsignedWordElement(45515),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.UF_RECOVER_SLOPE, new UnsignedWordElement(45516)), //
						m(GoodWe.ChannelId.CFP_UF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45517)), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT, new UnsignedWordElement(45518), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT_RECONNECT, new UnsignedWordElement(45519),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_UF_CHARGE_STOP, new UnsignedWordElement(45520), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_OF_DISCHARGE_STOP, new UnsignedWordElement(45521),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_TWOSSTEPF_FLG, new UnsignedWordElement(45522))//
				), //

				new FC16WriteRegistersTask(47505, //
						// If using EMS, must set to "2"
						m(GoodWe.ChannelId.MANUFACTURE_CODE, new UnsignedWordElement(47505)), //
						new DummyRegisterElement(47506, 47508), //
						m(GoodWe.ChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(47509)), //
						m(GoodWe.ChannelId.FEED_POWER_PARA_SET, new SignedWordElement(47510)), //
						m(GoodWe.ChannelId.EMS_POWER_MODE, new UnsignedWordElement(47511)), //
						m(GoodWe.ChannelId.EMS_POWER_SET, new UnsignedWordElement(47512)), //
						new DummyRegisterElement(47513), //
						m(GoodWe.ChannelId.BATTERY_PROTOCOL_ARM, new UnsignedWordElement(47514)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_START_TIME, new UnsignedWordElement(47515)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_END_TIME, new UnsignedWordElement(47516)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_BAT_POWER_PERCENT, new SignedWordElement(47517)), //
						m(GoodWe.ChannelId.WORK_WEEK_1, new UnsignedWordElement(47518)) //
				// TODO .debug()
				), //

				// Real-Time BMS Data for EMS Control (the data directly from BMS. Please refer
				// to the comments on registers 45352~45358)
				new FC16WriteRegistersTask(47900, //
						m(GoodWe.ChannelId.WBMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodWe.ChannelId.WBMS_STRINGS, new UnsignedWordElement(47901)), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(47902),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(47903),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(47904),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(47905),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_VOLTAGE, new UnsignedWordElement(47906), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CURRENT, new UnsignedWordElement(47907), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_SOC, new UnsignedWordElement(47908)), //
						m(GoodWe.ChannelId.WBMS_SOH, new UnsignedWordElement(47909)), //
						m(GoodWe.ChannelId.WBMS_TEMPERATURE, new SignedWordElement(47910), SCALE_FACTOR_MINUS_1), //
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
						m(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, new UnsignedWordElement(47916))), //

				new FC3ReadRegistersTask(47900, Priority.LOW, //
						m(GoodWe.ChannelId.WBMS_VERSION, new UnsignedWordElement(47900)), //
						m(GoodWe.ChannelId.WBMS_STRINGS, new UnsignedWordElement(47901)), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, new UnsignedWordElement(47902),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT, new UnsignedWordElement(47903),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(47904),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(47905),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_VOLTAGE, new UnsignedWordElement(47906), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_CURRENT, new UnsignedWordElement(47907)), //
						m(GoodWe.ChannelId.WBMS_SOC, new UnsignedWordElement(47908)), //
						m(GoodWe.ChannelId.WBMS_SOH, new UnsignedWordElement(47909)), //
						m(GoodWe.ChannelId.WBMS_TEMPERATURE, new SignedWordElement(47910), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.WBMS_WARNING_CODE, new UnsignedDoublewordElement(47911)), //
						m(GoodWe.ChannelId.WBMS_ALARM_CODE, new UnsignedDoublewordElement(47913)), //
						// TODO reset to individual states

						m(GoodWe.ChannelId.WBMS_STATUS, new UnsignedWordElement(47915)), //
						m(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, new UnsignedWordElement(47916))) //
		);

		// Handles different DSP versions
		ModbusUtils.readELementOnce(protocol, new UnsignedWordElement(35016), true) //
				.thenAccept(dspVersion -> {
					try {
						if (dspVersion >= 5) {
							this.handleDspVersion5(protocol);
						}
						if (dspVersion >= 6) {
							this.handleDspVersion6(protocol);
						}
						if (dspVersion >= 7) {
							this.handleDspVersion7(protocol);
						}
					} catch (OpenemsException e) {
						this.logError(this.log, "Unable to add task for modbus protocol");
					}
				});

		return protocol;
	}

	private void handleDspVersion7(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
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
								.bit(8, GoodWe.ChannelId.WORK_WEEK_2_ENABLED)), //
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
								.bit(8, GoodWe.ChannelId.WORK_WEEK_3_ENABLED)), //
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
								.bit(8, GoodWe.ChannelId.WORK_WEEK_4_ENABLED)), //
						m(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531)), //
						m(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532)), //
						new DummyRegisterElement(47533, 47541), //
						m(GoodWe.ChannelId.PEAK_SHAVING_POWER_LIMIT, new UnsignedWordElement(47542)), //
						new DummyRegisterElement(47543), //
						m(GoodWe.ChannelId.PEAK_SHAVING_SOC, new UnsignedWordElement(47544)), //
						m(GoodWe.ChannelId.FAST_CHARGE_ENABLE, new UnsignedWordElement(47545)), // [0,1]
						m(GoodWe.ChannelId.FAST_CHARGE_STOP_SOC, new UnsignedWordElement(47546))) // [0,100]
		);

		protocol.addTask(//
				new FC16WriteRegistersTask(47519, //
						m(GoodWe.ChannelId.WORK_WEEK_2_START_TIME, new UnsignedWordElement(47519)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_END_TIME, new UnsignedWordElement(47520)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_BAT_POWER_PERCENT, new SignedWordElement(47521)), //
						m(GoodWe.ChannelId.WORK_WEEK_2, new UnsignedWordElement(47522)), //

						m(GoodWe.ChannelId.WORK_WEEK_3_START_TIME, new UnsignedWordElement(47523)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_END_TIME, new UnsignedWordElement(47524)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_BAT_POWER_PERCENT, new SignedWordElement(47525)), //
						m(GoodWe.ChannelId.WORK_WEEK_3, new UnsignedWordElement(47526)), //

						m(GoodWe.ChannelId.WORK_WEEK_4_START_TIME, new UnsignedWordElement(47527)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_END_TIME, new UnsignedWordElement(47528)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_BMS_POWER_PERCENT, new SignedWordElement(47529)), //
						m(GoodWe.ChannelId.WORK_WEEK_4, new UnsignedWordElement(47530)), //

						// To set the SOC level to start/stop battery force charge.(this is not the
						// command from BMS, but the protection on inverter side. Eg. StartchgSOC
						// (47531) is set as 5%, but the battery BMS gives a force charge signal at
						// SOC
						// 6%, then battery will start force charge at 6% SOC; if BMS does not send
						// force charge command at 5% SOC, then battery will still start force
						// charge at
						// 5% SOC. ) Note: the default setting is 5% SOC to start and 10% to stop.
						// force
						// charge power is 1000W from PV or Grid as well
						m(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, new UnsignedWordElement(47531),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, new UnsignedWordElement(47532),
								SCALE_FACTOR_MINUS_1), //
						// to clear all economical mode settings (47515-47530) enter self Use Mode
						m(GoodWe.ChannelId.CLEAR_ALL_ECONOMIC_MODE, new UnsignedWordElement(47533)), //
						new DummyRegisterElement(47534, 47538), //
						m(GoodWe.ChannelId.WIFI_RESET, new UnsignedWordElement(47539)), //
						new DummyRegisterElement(47540), //
						m(GoodWe.ChannelId.WIFI_RELOAD, new UnsignedWordElement(47541)), //
						// to set the threshold of importing power, where peak-shaving acts. Eg. If
						// set
						// peak-shaving power as 20kW, then battery will only discharge when
						// imported
						// power from grid exceed 20kW to make sure the importing power keeps below
						// 20kW
						m(GoodWe.ChannelId.PEAK_SHAVING_POWER_LIMIT, new UnsignedDoublewordElement(47542)), //
						m(GoodWe.ChannelId.PEAK_SHAVING_SOC, new UnsignedWordElement(47544)), //
						// 0: Disable 1:Enable
						m(GoodWe.ChannelId.FAST_CHARGE_ENABLE, new UnsignedWordElement(47545)), //
						m(GoodWe.ChannelId.FAST_CHARGE_STOP_SOC, new UnsignedWordElement(47546))) //
		);

		protocol.addTask(//
				// Economic mode setting for ARM version => 18
				new FC3ReadRegistersTask(47547, Priority.LOW, //
						m(GoodWe.ChannelId.WORK_WEEK_1_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47547)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47548)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47549)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_PARAMETER1_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47550)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_PARAMETER1_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47551)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_PARAMETER1_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47552)), //

						m(GoodWe.ChannelId.WORK_WEEK_2_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47553)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47554)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47555)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_PARAMETER2_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47556)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_PARAMETER2_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47557)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_PARAMETER2_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47558)), //

						m(GoodWe.ChannelId.WORK_WEEK_3_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47559)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47560)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47561)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_PARAMETER3_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47562)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_PARAMETER3_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47563)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_PARAMETER3_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47564)), //

						m(GoodWe.ChannelId.WORK_WEEK_4_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47565)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47566)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47567)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_PARAMETER4_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47568)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_PARAMETER4_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47569)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_PARAMETER4_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47570)), //

						m(GoodWe.ChannelId.WORK_WEEK_5_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47571)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47572)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47573)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_PARAMETER5_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47574)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_PARAMETER5_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47575)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_PARAMETER5_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47576)), //

						m(GoodWe.ChannelId.WORK_WEEK_6_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47577)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47578)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47579)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_PARAMETER6_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47580)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_PARAMETER6_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47581)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_PARAMETER6_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47582)), //

						m(GoodWe.ChannelId.WORK_WEEK_7_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47583)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47584)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47585)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_PARAMETER7_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47586)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_PARAMETER7_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47587)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_PARAMETER7_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47588)), //

						m(GoodWe.ChannelId.WORK_WEEK_8_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47589)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47590)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47591)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_PARAMETER8_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47592)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_PARAMETER8_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47593)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_PARAMETER8_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47594)), //
						// 0,Disable 1,switching mode 2,Time manage mode
						// Only for inverter with ARM version equal or greater 18 To select Load
						// control
						// mode
						m(GoodWe.ChannelId.LOAD_REGULATION_INDEX, new UnsignedWordElement(47595)), //
						m(GoodWe.ChannelId.LOAD_SWITCH_STATUS, new UnsignedWordElement(47596)), //
						// For load control function, if the controlled load on Backup side, use
						// this to
						// switch the load off when battery reaches the SOC set
						m(GoodWe.ChannelId.BACKUP_SWITCH_SOC_MIN, new UnsignedWordElement(47597)), //
						new DummyRegisterElement(47598), //
						m(GoodWe.ChannelId.HARDWARE_FEED_POWER, new UnsignedWordElement(47599))) //
		);

		protocol.addTask(//
				new FC16WriteRegistersTask(47547, //
						// Economic mode setting for ARM version => 18
						m(GoodWe.ChannelId.WORK_WEEK_1_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47547)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47548)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47549)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_PARAMETER1_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47550)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_PARAMETER1_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47551)), //
						m(GoodWe.ChannelId.WORK_WEEK_1_PARAMETER1_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47552)), //

						m(GoodWe.ChannelId.WORK_WEEK_2_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47553)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47554)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47555)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_PARAMETER2_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47556)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_PARAMETER2_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47557)), //
						m(GoodWe.ChannelId.WORK_WEEK_2_PARAMETER2_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47558)), //

						m(GoodWe.ChannelId.WORK_WEEK_3_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47559)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47560)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47561)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_PARAMETER3_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47562)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_PARAMETER3_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47563)), //
						m(GoodWe.ChannelId.WORK_WEEK_3_PARAMETER3_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47564)), //

						m(GoodWe.ChannelId.WORK_WEEK_4_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47565)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47566)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47567)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_PARAMETER4_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47568)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_PARAMETER4_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47569)), //
						m(GoodWe.ChannelId.WORK_WEEK_4_PARAMETER4_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47570)), //

						m(GoodWe.ChannelId.WORK_WEEK_5_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47571)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47572)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47573)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_PARAMETER5_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47574)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_PARAMETER5_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47575)), //
						m(GoodWe.ChannelId.WORK_WEEK_5_PARAMETER5_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47576)), //

						m(GoodWe.ChannelId.WORK_WEEK_6_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47577)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47578)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47579)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_PARAMETER6_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47580)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_PARAMETER6_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47581)), //
						m(GoodWe.ChannelId.WORK_WEEK_6_PARAMETER6_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47582)), //

						m(GoodWe.ChannelId.WORK_WEEK_7_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47583)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47584)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47585)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_PARAMETER7_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47586)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_PARAMETER7_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47587)), //
						m(GoodWe.ChannelId.WORK_WEEK_7_PARAMETER7_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47588)), //

						m(GoodWe.ChannelId.WORK_WEEK_8_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47589)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47590)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_ECO_MODE_FOR_ARM_18_AND_GREATER, new UnsignedWordElement(47591)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_PARAMETER8_1_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47592)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_PARAMETER8_2_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47593)), //
						m(GoodWe.ChannelId.WORK_WEEK_8_PARAMETER8_3_ECO_MODE_FOR_ARM_18_AND_GREATER,
								new UnsignedWordElement(47594)), //
						// 0,Disable 1,switching mode 2,Time manage mode
						// Only for inverter with ARM version equal or greater 18 To select Load
						// control
						// mode
						m(GoodWe.ChannelId.LOAD_REGULATION_INDEX, new UnsignedWordElement(47595)), //
						m(GoodWe.ChannelId.LOAD_SWITCH_STATUS, new UnsignedWordElement(47596)), //
						// For load control function, if the controlled load on Backup side, use
						// this to
						// switch the load off when battery reaches the SOC set
						m(GoodWe.ChannelId.BACKUP_SWITCH_SOC_MIN, new UnsignedWordElement(47597)), //
						new DummyRegisterElement(47598), //
						m(GoodWe.ChannelId.HARDWARE_FEED_POWER, new UnsignedWordElement(47599)))//
		);
	}

	private void handleDspVersion6(ModbusProtocol protocol) throws OpenemsException {
		// Registers 36000 for COM_MODE throw "Illegal Data Address"

		protocol.addTask(//
				new FC3ReadRegistersTask(36001, Priority.LOW, //
						// External Communication Data(ARM)
						m(GoodWe.ChannelId.RSSI, new UnsignedWordElement(36001)), //
						new DummyRegisterElement(36002, 36003), //
						m(GoodWe.ChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004)), //
						// Registers for Grid Smart-Meter (36005 to 36014) are read via GridMeter
						// implementation
						new DummyRegisterElement(36005, 36014),
						m(GoodWe.ChannelId.E_TOTAL_SELL, new FloatDoublewordElement(36015), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY_F, new FloatDoublewordElement(36017), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.METER_ACTIVE_POWER_R, new SignedDoublewordElement(36019)), //
						m(GoodWe.ChannelId.METER_ACTIVE_POWER_S, new SignedDoublewordElement(36021)), //
						m(GoodWe.ChannelId.METER_ACTIVE_POWER_T, new SignedDoublewordElement(36023)), //
						m(GoodWe.ChannelId.METER_TOTAL_ACTIVE_POWER, new SignedDoublewordElement(36025)), //
						m(GoodWe.ChannelId.METER_REACTIVE_POWER_R, new SignedDoublewordElement(36027)), //
						m(GoodWe.ChannelId.METER_REACTIVE_POWER_S, new SignedDoublewordElement(36029)), //
						m(GoodWe.ChannelId.METER_REACTIVE_POWER_T, new SignedDoublewordElement(36031)), //
						m(GoodWe.ChannelId.METER_TOTAL_REACTIVE_POWER, new SignedDoublewordElement(36033)), //
						m(GoodWe.ChannelId.METER_APPARENT_POWER_R, new SignedDoublewordElement(36035)), //
						m(GoodWe.ChannelId.METER_APPARENT_POWER_S, new SignedDoublewordElement(36037)), //
						m(GoodWe.ChannelId.METER_APPARENT_POWER_T, new SignedDoublewordElement(36039)), //
						m(GoodWe.ChannelId.METER_TOTAL_APPARENT_POWER, new SignedDoublewordElement(36041)), //
						// Only for GoodWe smart meter
						m(GoodWe.ChannelId.METER_TYPE, new UnsignedWordElement(36043)), //
						m(GoodWe.ChannelId.METER_SOFTWARE_VERSION, new UnsignedWordElement(36044)), //
						// Only for AC coupled inverter. Detect Pv Meter
						m(GoodWe.ChannelId.METER_CT2_ACTIVE_POWER, new SignedDoublewordElement(36045)), //
						m(GoodWe.ChannelId.CT2_E_TOTAL_SELL, new UnsignedDoublewordElement(36047),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CT2_E_TOTAL_BUY, new UnsignedDoublewordElement(36049), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.METER_CT2_STATUS, new UnsignedWordElement(36051))) //
		);

		protocol.addTask(//
				new FC3ReadRegistersTask(47000, Priority.LOW, //
						m(GoodWe.ChannelId.SELECT_WORK_MODE, new UnsignedWordElement(47000)), //
						new DummyRegisterElement(47001), //
						m(GoodWe.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002)), //
						new DummyRegisterElement(47003, 47004), //
						m(GoodWe.ChannelId.LOG_DATA_ENABLE, new UnsignedWordElement(47005)), //
						m(GoodWe.ChannelId.DATA_SEND_INTERVAL, new UnsignedWordElement(47006)), //
						m(GoodWe.ChannelId.DRED_CMD, new UnsignedWordElement(47007)), //
						new DummyRegisterElement(47008), //
						m(GoodWe.ChannelId.WIFI_OR_LAN_SWITCH, new UnsignedWordElement(47009)), //
						new DummyRegisterElement(47010, 47011), //
						m(GoodWe.ChannelId.LED_BLINK_TIME, new UnsignedWordElement(47012)), //
						m(GoodWe.ChannelId.WIFI_LED_STATE, new UnsignedWordElement(47013)), //
						m(GoodWe.ChannelId.COM_LED_STATE, new UnsignedWordElement(47014)), //
						m(GoodWe.ChannelId.METER_CT1_REVERSE_ENABLE, new UnsignedWordElement(47015)), //
						m(GoodWe.ChannelId.ERROR_LOG_READ_PAGE, new UnsignedWordElement(47016)), //
						// 1:on 0:off If not connect to Internet, please set 1
						m(GoodWe.ChannelId.MODBUS_TCP_WITHOUT_INTERNET, new UnsignedWordElement(47017)), //
						// 1: off, 2: on, 3: flash 1x, 4: flash 2x, 5: flash 4x
						m(GoodWe.ChannelId.BACKUP_LED, new UnsignedWordElement(47018)), // [1,5]
						m(GoodWe.ChannelId.GRID_LED, new UnsignedWordElement(47019)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_1, new UnsignedWordElement(47020)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_2, new UnsignedWordElement(47021)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_3, new UnsignedWordElement(47022)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_4, new UnsignedWordElement(47023)), // [1,5]
						m(GoodWe.ChannelId.BATTERY_LED, new UnsignedWordElement(47024)), // [1,5]
						m(GoodWe.ChannelId.SYSTEM_LED, new UnsignedWordElement(47025)), // [1,5]
						m(GoodWe.ChannelId.FAULT_LED, new UnsignedWordElement(47026)), // [1,5]
						m(GoodWe.ChannelId.ENERGY_LED, new UnsignedWordElement(47027)), // [1,5]
						m(GoodWe.ChannelId.LED_EXTERNAL_CONTROL, new UnsignedWordElement(47028)), // [42343]
						new DummyRegisterElement(47029, 47037), //
						// 1 Enable, After restart the inverter, setting saved
						m(GoodWe.ChannelId.STOP_MODE_SAVE_ENABLE, new UnsignedWordElement(47038))) //
		);

		protocol.addTask(//
				// The same function as that for Operation Mode on PV Master App
				new FC16WriteRegistersTask(47000, //
						m(GoodWe.ChannelId.SELECT_WORK_MODE, new UnsignedWordElement(47000)), //
						new DummyRegisterElement(47001), //
						m(GoodWe.ChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(47002)), //
						new DummyRegisterElement(47003, 47004), //
						// Breakpoint Resume for Data transferring. Activated as default, time
						// interval 5
						// minutes
						m(GoodWe.ChannelId.LOG_DATA_ENABLE, new UnsignedWordElement(47005)), //
						// Time interval for data send to cloud or EMS,default is 1 minute
						m(GoodWe.ChannelId.DATA_SEND_INTERVAL, new UnsignedWordElement(47006)), //
						// Only for Australia, Refer to Table 8-22
						m(GoodWe.ChannelId.DRED_CMD, new UnsignedWordElement(47007)), //
						new DummyRegisterElement(47008), //
						// For wifi+Lan module, to switch to LAN or WiFi communicaiton
						m(GoodWe.ChannelId.WIFI_OR_LAN_SWITCH, new UnsignedWordElement(47009)), //
						new DummyRegisterElement(47010, 47011), //
						m(GoodWe.ChannelId.LED_BLINK_TIME, new UnsignedWordElement(47012)), //
						// 1: off, 2: on, 3: flash 1x, 4: flash 2x, 5: flash 4x
						m(GoodWe.ChannelId.WIFI_LED_STATE, new UnsignedWordElement(47013)), //
						m(GoodWe.ChannelId.COM_LED_STATE, new UnsignedWordElement(47014)), //
						// 1:on 0:off only for single phase Smart meter
						m(GoodWe.ChannelId.METER_CT1_REVERSE_ENABLE, new UnsignedWordElement(47015)), //
						m(GoodWe.ChannelId.ERROR_LOG_READ_PAGE, new UnsignedWordElement(47016)), //
						// 1:on 0:off If not connect to Internet, please set 1
						m(GoodWe.ChannelId.MODBUS_TCP_WITHOUT_INTERNET, new UnsignedWordElement(47017)), //
						// 1: off, 2: on, 3: flash 1x, 4: flash 2x, 5: flash 4x
						m(GoodWe.ChannelId.BACKUP_LED, new UnsignedWordElement(47018)), // [1,5]
						m(GoodWe.ChannelId.GRID_LED, new UnsignedWordElement(47019)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_1, new UnsignedWordElement(47020)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_2, new UnsignedWordElement(47021)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_3, new UnsignedWordElement(47022)), // [1,5]
						m(GoodWe.ChannelId.SOC_LED_4, new UnsignedWordElement(47023)), // [1,5]
						m(GoodWe.ChannelId.BATTERY_LED, new UnsignedWordElement(47024)), // [1,5]
						m(GoodWe.ChannelId.SYSTEM_LED, new UnsignedWordElement(47025)), // [1,5]
						m(GoodWe.ChannelId.FAULT_LED, new UnsignedWordElement(47026)), // [1,5]
						m(GoodWe.ChannelId.ENERGY_LED, new UnsignedWordElement(47027)), // [1,5]
						m(GoodWe.ChannelId.LED_EXTERNAL_CONTROL, new UnsignedWordElement(47028)), // [42343]
						new DummyRegisterElement(47029, 47037), //
						// 1 Enable, After restart the inverter, setting saved
						m(GoodWe.ChannelId.STOP_MODE_SAVE_ENABLE, new UnsignedWordElement(47038)))//
		);
	}

	private void handleDspVersion5(ModbusProtocol protocol) throws OpenemsException {
		// Registers 36000 for COM_MODE throw "Illegal Data Address"

		protocol.addTask(//
				new FC3ReadRegistersTask(36001, Priority.LOW, //
						m(GoodWe.ChannelId.RSSI, new UnsignedWordElement(36001)), //
						new DummyRegisterElement(36002, 36003), //
						m(GoodWe.ChannelId.METER_COMMUNICATE_STATUS, new UnsignedWordElement(36004)), //
						// Registers for Grid Smart-Meter (36005 to 36014) are read via GridMeter
						// implementation
						new DummyRegisterElement(36005, 36014),
						m(GoodWe.ChannelId.E_TOTAL_SELL, new FloatDoublewordElement(36015), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.E_TOTAL_BUY_F, new FloatDoublewordElement(36017), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.METER_ACTIVE_POWER_R, new SignedDoublewordElement(36019)), //
						m(GoodWe.ChannelId.METER_ACTIVE_POWER_S, new SignedDoublewordElement(36021)), //
						m(GoodWe.ChannelId.METER_ACTIVE_POWER_T, new SignedDoublewordElement(36023)), //
						m(GoodWe.ChannelId.METER_TOTAL_ACTIVE_POWER, new SignedDoublewordElement(36025)), //
						m(GoodWe.ChannelId.METER_REACTIVE_POWER_R, new SignedDoublewordElement(36027)), //
						m(GoodWe.ChannelId.METER_REACTIVE_POWER_S, new SignedDoublewordElement(36029)), //
						m(GoodWe.ChannelId.METER_REACTIVE_POWER_T, new SignedDoublewordElement(36031)), //
						m(GoodWe.ChannelId.METER_TOTAL_REACTIVE_POWER, new SignedDoublewordElement(36033)), //
						m(GoodWe.ChannelId.METER_APPARENT_POWER_R, new SignedDoublewordElement(36035)), //
						m(GoodWe.ChannelId.METER_APPARENT_POWER_S, new SignedDoublewordElement(36037)), //
						m(GoodWe.ChannelId.METER_APPARENT_POWER_T, new SignedDoublewordElement(36039)), //
						m(GoodWe.ChannelId.METER_TOTAL_APPARENT_POWER, new SignedDoublewordElement(36041)), //
						// Only for GoodWe smart meter
						m(GoodWe.ChannelId.METER_TYPE, new UnsignedWordElement(36043)), //
						m(GoodWe.ChannelId.METER_SOFTWARE_VERSION, new UnsignedWordElement(36044)), //
						// Only for AC coupled inverter. Detect Pv Meter
						m(GoodWe.ChannelId.METER_CT2_ACTIVE_POWER, new SignedDoublewordElement(36045)), //
						m(GoodWe.ChannelId.CT2_E_TOTAL_SELL, new UnsignedDoublewordElement(36047),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CT2_E_TOTAL_BUY, new UnsignedDoublewordElement(36049), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.METER_CT2_STATUS, new UnsignedWordElement(36051))) //
		);
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
		}
		if (this instanceof HybridManagedSymmetricBatteryInverter) {
			return new DummyRegisterElement(address);
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
		var productionPower = this.calculatePvProduction();
		final Channel<Integer> pBattery1Channel = this.channel(GoodWe.ChannelId.P_BATTERY1);
		var dcDischargePower = pBattery1Channel.value().get();
		var acActivePower = TypeUtils.sum(productionPower, dcDischargePower);

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
		} else if (dcDischargePower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcDischargePower);
		} else {
			// Charge
			this.calculateDcChargeEnergy.update(dcDischargePower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

	/**
	 * Calculate and store Max-AC-Export and -Import channels.
	 *
	 * @param maxApparentPower the max apparent power
	 */
	protected void calculateMaxAcPower(int maxApparentPower) {
		// Calculate and store Max-AC-Export and -Import for use in
		// getStaticConstraints()
		var maxDcChargePower = /* can be negative for force-discharge */
				TypeUtils.multiply(//
						/* Inverter Charge-Max-Current */ this.getWbmsChargeMaxCurrent().get(), //
						/* Voltage */ this.getWbmsVoltage().orElse(0));
		int pvProduction = TypeUtils.max(0, this.calculatePvProduction());

		// Calculates Max-AC-Import and Max-AC-Export as positive numbers
		var maxAcImport = TypeUtils.subtract(maxDcChargePower,
				TypeUtils.min(maxDcChargePower /* avoid negative number for `subtract` */, pvProduction));
		var maxAcExport = TypeUtils.sum(//
				/* Max DC-Discharge-Power */ TypeUtils.multiply(//
						/* Inverter Discharge-Max-Current */ this.getWbmsDischargeMaxCurrent().get(), //
						/* Voltage */ this.getWbmsVoltage().orElse(0)),
				/* PV Production */ pvProduction);

		// Limit Max-AC-Power to inverter specific limit
		maxAcImport = TypeUtils.min(maxAcImport, maxApparentPower);
		maxAcExport = TypeUtils.min(maxAcExport, maxApparentPower);

		// Set Channels
		this._setMaxAcImport(TypeUtils.multiply(maxAcImport, /* negate */ -1));
		this._setMaxAcExport(maxAcExport);
	}

	/**
	 * Gets Surplus Power.
	 *
	 * @return {@link Integer}
	 */
	public abstract Integer getSurplusPower();

}
