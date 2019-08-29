package io.openems.edge.ess.sinexcel;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.sinexcel.enums.FalseTrue;

public enum SinexcelChannelId implements ChannelId {
	SUNSPEC_DID_0103(Doc.of(OpenemsType.INTEGER)), //
	SET_INTERN_DC_RELAY(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.NONE)),

	MOD_ON_CMD(Doc.of(FalseTrue.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	MOD_OFF_CMD(Doc.of(FalseTrue.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	CLEAR_FAILURE_CMD(Doc.of(FalseTrue.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	ON_GRID_CMD(Doc.of(FalseTrue.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	OFF_GRID_CMD(Doc.of(FalseTrue.values()) //
			.accessMode(AccessMode.READ_WRITE)), //

	SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.READ_WRITE)), //
	SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.READ_WRITE)), //

	SET_ANTI_ISLANDING(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.ON_OFF)),
	SET_CHARGE_DISCHARGE_ACTIVE(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.KILOWATT)), //
	SET_CHARGE_DISCHARGE_REACTIVE(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
	SET_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.AMPERE)),
	SET_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.AMPERE)),
	SET_SLOW_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT)),
	SET_FLOAT_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT)),
	SET_UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT)),
	SET_LOWER_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT)),
	SET_ANALOG_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.KILOWATT_HOURS)),
	SET_ANALOG_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.KILOWATT_HOURS)),
	SET_ANALOG_DC_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.KILOWATT_HOURS)),
	SET_ANALOG_DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.KILOWATT_HOURS)),
	BAT_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)),
	BAT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)),
	BAT_TEMP(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.DEGREE_CELSIUS)),
	BAT_SOC(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.PERCENT)),
	BAT_SOH(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.PERCENT)),
	DEBUG_DIS_MIN_V(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	DIS_MIN_V(new IntegerDoc() //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT) //
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(SinexcelChannelId.DEBUG_DIS_MIN_V))), //
	DEBUG_CHA_MAX_V(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	CHA_MAX_V(new IntegerDoc() //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT) //
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(SinexcelChannelId.DEBUG_CHA_MAX_V))), //
	DEBUG_DIS_MAX_A(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	DIS_MAX_A(new IntegerDoc() //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.AMPERE) //
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(SinexcelChannelId.DEBUG_DIS_MAX_A))), //
	DEBUG_CHA_MAX_A(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	CHA_MAX_A(new IntegerDoc() //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.AMPERE) //
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(SinexcelChannelId.DEBUG_CHA_MAX_A))), //
	DEBUG_EN_LIMIT(Doc.of(OpenemsType.INTEGER)), //
	EN_LIMIT(new IntegerDoc() //
			.accessMode(AccessMode.WRITE_ONLY) //
			.text("new battery limits are activated when EnLimit is 1") //
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(SinexcelChannelId.DEBUG_EN_LIMIT))), //
	ANTI_ISLANDING(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.ON_OFF)),
	FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ)), //
	TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	SERIAL(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE)), //
	MODEL(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE)), //
	MANUFACTURER(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE)),
	MODEL_2(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE)), //
	VERSION(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE)), //
	SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE)), //
	ANALOG_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOWATT_HOURS)),
	ANALOG_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOWATT_HOURS)), //
	TARGET_OFFGRID_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)),
	TARGET_OFFGRID_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ)),
	ANALOG_DC_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOVOLT_AMPERE)),
	ANALOG_DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOVOLT_AMPERE)),
	AC_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE)), //
	AC_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE)), //
	AC_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	INVOUTVOLT_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	INVOUTVOLT_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)),
	INVOUTVOLT_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	INVOUTCURRENT_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	INVOUTCURRENT_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	INVOUTCURRENT_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	DC_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	SINEXCEL_STATE(Doc.of(CurrentState.values())), //
	TARGET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOWATT)), //
	TARGET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOWATT)), //
	MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)),
	LOWER_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	UPPER_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)),

	SINEXCEL_STATE_1(Doc.of(Level.INFO) //
			.text("OFF")), //
	SINEXCEL_STATE_2(Doc.of(Level.INFO) //
			.text("Sleeping")), //
	SINEXCEL_STATE_3(Doc.of(Level.INFO) //
			.text("Starting")), //
	SINEXCEL_STATE_4(Doc.of(Level.INFO) //
			.text("MPPT")), //
	SINEXCEL_STATE_5(Doc.of(Level.INFO) //
			.text("Throttled")), //
	SINEXCEL_STATE_6(Doc.of(Level.INFO) //
			.text("Shutting down")), //
	SINEXCEL_STATE_7(Doc.of(Level.INFO) //
			.text("Fault")), //
	SINEXCEL_STATE_8(Doc.of(Level.INFO) //
			.text("Standby")), //
	SINEXCEL_STATE_9(Doc.of(Level.INFO) //
			.text("Started")), //

	// EVENT Bitfield 32
	STATE_0(Doc.of(Level.FAULT) //
			.text("Ground fault")), //
	STATE_1(Doc.of(Level.WARNING) //
			.text("DC over Voltage")), //
	STATE_2(Doc.of(Level.WARNING) //
			.text("AC disconnect open")), //
	STATE_3(Doc.of(Level.WARNING) //
			.text("DC disconnect open")), //
	STATE_4(Doc.of(Level.WARNING) //
			.text("Grid shutdown")), //
	STATE_5(Doc.of(Level.WARNING) //
			.text("Cabinet open")), //
	STATE_6(Doc.of(Level.WARNING) //
			.text("Manual shutdown")), //
	STATE_7(Doc.of(Level.WARNING) //
			.text("Over temperature")), //
	STATE_8(Doc.of(Level.WARNING) //
			.text("AC Frequency above limit")), //
	STATE_9(Doc.of(Level.WARNING) //
			.text("AC Frequnecy under limit")), //
	STATE_10(Doc.of(Level.WARNING) //
			.text("AC Voltage above limit")), //
	STATE_11(Doc.of(Level.WARNING) //
			.text("AC Voltage under limit")), //
	STATE_12(Doc.of(Level.WARNING) //
			.text("Blown String fuse on input")), //
	STATE_13(Doc.of(Level.WARNING) //
			.text("Under temperature")), //
	STATE_14(Doc.of(Level.WARNING) //
			.text("Generic Memory or Communication error (internal)")), //
	STATE_15(Doc.of(Level.FAULT) //
			.text("Hardware test failure")), //

	// FAULT LIST
	STATE_16(Doc.of(Level.FAULT) //
			.text("Fault Status")), //
	STATE_17(Doc.of(Level.WARNING) //
			.text("Alert Status")), //
	STATE_18(Doc.of(Level.INFO) //
			.text("On/Off Status")), //
	STATE_19(Doc.of(Level.INFO) //
			.text("On Grid") //
			.onInit(c -> { //
				StateChannel channel = (StateChannel) c;
				EssSinexcel self = (EssSinexcel) channel.getComponent();
				((StateChannel) channel).onChange((oldValue, newValue) -> {
					Optional<Boolean> value = newValue.asOptional();
					if (!value.isPresent()) {
						self.getGridMode().setNextValue(GridMode.UNDEFINED);
					} else {
						if (value.get()) {
							self.getGridMode().setNextValue(GridMode.ON_GRID);
						} else {
							self.getGridMode().setNextValue(GridMode.OFF_GRID);
						}
					}
				});
			})),
	STATE_20(Doc.of(Level.INFO) //
			.text("Off Grid")), //
	STATE_21(Doc.of(Level.WARNING) //
			.text("AC OVP")), //
	STATE_22(Doc.of(Level.WARNING) //
			.text("AC UVP")), //
	STATE_23(Doc.of(Level.WARNING) //
			.text("AC OFP")), //
	STATE_24(Doc.of(Level.WARNING) //
			.text("AC UFP")), //
	STATE_25(Doc.of(Level.WARNING) //
			.text("Grid Voltage Unbalance")), //
	STATE_26(Doc.of(Level.WARNING) //
			.text("Grid Phase reserve")), //
	STATE_27(Doc.of(Level.INFO) //
			.text("Islanding")), //
	STATE_28(Doc.of(Level.WARNING) //
			.text("On/ Off Grid Switching Error")), //
	STATE_29(Doc.of(Level.WARNING) //
			.text("Output Grounding Error")), //
	STATE_30(Doc.of(Level.WARNING) //
			.text("Output Current Abnormal")), //
	STATE_31(Doc.of(Level.WARNING) //
			.text("Grid Phase Lock Fails")), //
	STATE_32(Doc.of(Level.WARNING) //
			.text("Internal Air Over-Temp")), //
	STATE_33(Doc.of(Level.WARNING) //
			.text("Zeitueberschreitung der Netzverbindung")), //
	STATE_34(Doc.of(Level.INFO) //
			.text("EPO")), //
	STATE_35(Doc.of(Level.FAULT) //
			.text("HMI Parameters Fault")), //
	STATE_36(Doc.of(Level.WARNING) //
			.text("DSP Version Error")), //
	STATE_37(Doc.of(Level.WARNING) //
			.text("CPLD Version Error")), //
	STATE_38(Doc.of(Level.WARNING) //
			.text("Hardware Version Error")), //
	STATE_39(Doc.of(Level.WARNING) //
			.text("Communication Error")), //
	STATE_40(Doc.of(Level.WARNING) //
			.text("AUX Power Error")), //
	STATE_41(Doc.of(Level.FAULT) //
			.text("Fan Failure")), //
	STATE_42(Doc.of(Level.WARNING) //
			.text("BUS Over Voltage")), //
	STATE_43(Doc.of(Level.WARNING) //
			.text("BUS Low Voltage")), //
	STATE_44(Doc.of(Level.WARNING) //
			.text("BUS Voltage Unbalanced")), //
	STATE_45(Doc.of(Level.WARNING) //
			.text("AC Soft Start Failure")), //
	STATE_46(Doc.of(Level.WARNING) //
			.text("Reserved")), //
	STATE_47(Doc.of(Level.WARNING) //
			.text("Output Voltage Abnormal")), //
	STATE_48(Doc.of(Level.WARNING) //
			.text("Output Current Unbalanced")), //
	STATE_49(Doc.of(Level.WARNING) //
			.text("Over Temperature of Heat Sink")), //
	STATE_50(Doc.of(Level.WARNING) //
			.text("Output Overload")), //
	STATE_51(Doc.of(Level.WARNING) //
			.text("Reserved")), //
	STATE_52(Doc.of(Level.WARNING) //
			.text("AC Breaker Short-Circuit")), //
	STATE_53(Doc.of(Level.WARNING) //
			.text("Inverter Start Failure")), //
	STATE_54(Doc.of(Level.WARNING) //
			.text("AC Breaker is open")), //
	STATE_55(Doc.of(Level.WARNING) //
			.text("EE Reading Error 1")), //
	STATE_56(Doc.of(Level.WARNING) //
			.text("EE Reading Error 2")), //
	STATE_57(Doc.of(Level.FAULT) //
			.text("SPD Failure  ")), //
	STATE_58(Doc.of(Level.WARNING) //
			.text("Inverter over load")), //
	STATE_59(Doc.of(Level.INFO) //
			.text("DC Charging")), //
	STATE_60(Doc.of(Level.INFO) //
			.text("DC Discharging")), //
	STATE_61(Doc.of(Level.INFO) //
			.text("Battery fully charged")), //
	STATE_62(Doc.of(Level.INFO) //
			.text("Battery empty")), //
	STATE_63(Doc.of(Level.FAULT) //
			.text("Fault Status")), //
	STATE_64(Doc.of(Level.WARNING) //
			.text("Alert Status")), //
	STATE_65(Doc.of(Level.WARNING) //
			.text("DC input OVP")), //
	STATE_66(Doc.of(Level.WARNING) //
			.text("DC input UVP")), //
	STATE_67(Doc.of(Level.WARNING) //
			.text("DC Groundig Error")), //
	STATE_68(Doc.of(Level.WARNING) //
			.text("BMS alerts")), //
	STATE_69(Doc.of(Level.FAULT) //
			.text("DC Soft-Start failure")), //
	STATE_70(Doc.of(Level.WARNING) //
			.text("DC relay short-circuit")), //
	STATE_71(Doc.of(Level.WARNING) //
			.text("DC realy short open")), //
	STATE_72(Doc.of(Level.WARNING) //
			.text("Battery power over load")), //
	STATE_73(Doc.of(Level.FAULT) //
			.text("BUS start fails")), //
	STATE_74(Doc.of(Level.WARNING) //
			.text("DC OCP"));

	private final Doc doc;

	private SinexcelChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}