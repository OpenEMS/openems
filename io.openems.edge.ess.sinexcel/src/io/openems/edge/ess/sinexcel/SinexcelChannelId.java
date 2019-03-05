package io.openems.edge.ess.sinexcel;

import java.util.Optional;

import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.sum.GridMode;

public enum SinexcelChannelId implements io.openems.edge.common.channel.doc.ChannelId {
	SUNSPEC_DID_0103(new Doc()), //
	SET_INTERN_DC_RELAY(new Doc() //
			.unit(Unit.NONE)),
	SETDATA_MOD_ON_CMD(new Doc() //
			.unit(Unit.ON_OFF)),
	SETDATA_MOD_OFF_CMD(new Doc() //
			.unit(Unit.ON_OFF)),
	SETDATA_GRID_ON_CMD(new Doc() //
			.unit(Unit.ON_OFF)),
	SETDATA_GRID_OFF_CMD(new Doc() //
			.unit(Unit.ON_OFF)),
	SET_ANTI_ISLANDING(new Doc() //
			.unit(Unit.ON_OFF)),
	SET_CHARGE_DISCHARGE_ACTIVE(new Doc() //
			.unit(Unit.KILOWATT)), //
	SET_CHARGE_DISCHARGE_REACTIVE(new Doc() //
			.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
	SET_CHARGE_CURRENT(new Doc() //
			.unit(Unit.AMPERE)),
	SET_DISCHARGE_CURRENT(new Doc() //
			.unit(Unit.AMPERE)),
	SET_SLOW_CHARGE_VOLTAGE(new Doc() //
			.unit(Unit.VOLT)),
	SET_FLOAT_CHARGE_VOLTAGE(new Doc() //
			.unit(Unit.VOLT)),
	SET_UPPER_VOLTAGE(new Doc() //
			.unit(Unit.VOLT)),
	SET_LOWER_VOLTAGE(new Doc() //
			.unit(Unit.VOLT)),
	SET_ANALOG_CHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOWATT_HOURS)),
	SET_ANALOG_DISCHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOWATT_HOURS)),
	SET_ANALOG_DC_CHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOWATT_HOURS)),
	SET_ANALOG_DC_DISCHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOWATT_HOURS)),
	BAT_MIN_CELL_VOLTAGE(new Doc() //
			.unit(Unit.MILLIVOLT)),
	BAT_VOLTAGE(new Doc() //
			.unit(Unit.VOLT)),
	BAT_TEMP(new Doc() //
			.unit(Unit.DEGREE_CELSIUS)),
	BAT_SOC(new Doc() //
			.unit(Unit.PERCENT)),
	BAT_SOH(new Doc() //
			.unit(Unit.PERCENT)),
	DEBUG_DIS_MIN_V(new Doc() //
			.unit(Unit.VOLT)), //
	@SuppressWarnings("unchecked")
	DIS_MIN_V(new Doc() //
			.unit(Unit.VOLT) //
			.onInit(channel -> { //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
					channel.getComponent().channel(SinexcelChannelId.DEBUG_DIS_MIN_V).setNextValue(value);
				});
			})), //
	DEBUG_CHA_MAX_V(new Doc() //
			.unit(Unit.VOLT)), //
	@SuppressWarnings("unchecked")
	CHA_MAX_V(new Doc() //
			.unit(Unit.VOLT) //
			.onInit(channel -> { //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
					channel.getComponent().channel(SinexcelChannelId.DEBUG_CHA_MAX_V).setNextValue(value);
				});
			})),
	DEBUG_DIS_MAX_A(new Doc() //
			.unit(Unit.AMPERE)), //
	@SuppressWarnings("unchecked")
	DIS_MAX_A(new Doc() //
			.unit(Unit.AMPERE) //
			.onInit(channel -> { //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
					channel.getComponent().channel(SinexcelChannelId.DEBUG_DIS_MAX_A).setNextValue(value);
				});
			})),
	DEBUG_CHA_MAX_A(new Doc() //
			.unit(Unit.AMPERE)), //
	@SuppressWarnings("unchecked")
	CHA_MAX_A(new Doc() //
			.unit(Unit.AMPERE) //
			.onInit(channel -> { //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
					channel.getComponent().channel(SinexcelChannelId.DEBUG_CHA_MAX_A).setNextValue(value);
				});
			})),
	DEBUG_EN_LIMIT(new Doc()), //
	@SuppressWarnings("unchecked")
	EN_LIMIT(new Doc() //
			.text("new battery limits are activated when EnLimit is 1") //
			.onInit(channel -> { //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
					channel.getComponent().channel(SinexcelChannelId.DEBUG_EN_LIMIT).setNextValue(value);
				});
			})),
	ANTI_ISLANDING(new Doc() //
			.unit(Unit.ON_OFF)),
	MOD_ON_CMD(new Doc() //
			.unit(Unit.ON_OFF)), //
	MOD_OFF_CMD(new Doc() //
			.unit(Unit.ON_OFF)), //
	GRID_ON_CMD(new Doc() //
			.unit(Unit.ON_OFF)), //
	GRID_OFF_CMD(new Doc() //
			.unit(Unit.ON_OFF)), //
	FREQUENCY(new Doc() //
			.unit(Unit.HERTZ)), //
	TEMPERATURE(new Doc() //
			.unit(Unit.DEGREE_CELSIUS)), //
	SERIAL(new Doc() //
			.unit(Unit.NONE)), //
	MODEL(new Doc() //
			.unit(Unit.NONE)), //
	MANUFACTURER(new Doc() //
			.unit(Unit.NONE)),
	MODEL_2(new Doc(). //
			unit(Unit.NONE)), //
	VERSION(new Doc() //
			.unit(Unit.NONE)), //
	SERIAL_NUMBER(new Doc() //
			.unit(Unit.NONE)), //
	ANALOG_CHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOWATT_HOURS)),
	ANALOG_DISCHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOWATT_HOURS)), //
	TARGET_OFFGRID_VOLTAGE(new Doc() //
			.unit(Unit.NONE)),
	TARGET_OFFGRID_FREQUENCY(new Doc() //
			.unit(Unit.HERTZ)),
	ANALOG_DC_CHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOVOLT_AMPERE)),
	ANALOG_DC_DISCHARGE_ENERGY(new Doc() //
			.unit(Unit.KILOVOLT_AMPERE)),
	AC_APPARENT_POWER(new Doc() //
			.unit(Unit.VOLT_AMPERE)), //
	AC_REACTIVE_POWER(new Doc() //
			.unit(Unit.VOLT_AMPERE_REACTIVE)), //
	AC_POWER(new Doc() //
			.unit(Unit.WATT)), //
	INVOUTVOLT_L1(new Doc() //
			.unit(Unit.VOLT)), //
	INVOUTVOLT_L2(new Doc() //
			.unit(Unit.VOLT)),
	INVOUTVOLT_L3(new Doc() //
			.unit(Unit.VOLT)), //
	INVOUTCURRENT_L1(new Doc() //
			.unit(Unit.AMPERE)), //
	INVOUTCURRENT_L2(new Doc() //
			.unit(Unit.AMPERE)), //
	INVOUTCURRENT_L3(new Doc() //
			.unit(Unit.AMPERE)), //
	DC_POWER(new Doc() //
			.unit(Unit.WATT)), //
	DC_CURRENT(new Doc() //
			.unit(Unit.AMPERE)), //
	DC_VOLTAGE(new Doc() //
			.unit(Unit.VOLT)), //
	SINEXCEL_STATE(new Doc() //
			.options(CurrentState.values())), //
	TARGET_ACTIVE_POWER(new Doc() //
			.unit(Unit.KILOWATT)), //
	TARGET_REACTIVE_POWER(new Doc() //
			.unit(Unit.KILOWATT)), //
	MAX_CHARGE_CURRENT(new Doc() //
			.unit(Unit.AMPERE)), //
	MAX_DISCHARGE_CURRENT(new Doc() //
			.unit(Unit.AMPERE)),
	LOWER_VOLTAGE_LIMIT(new Doc() //
			.unit(Unit.VOLT)), //
	UPPER_VOLTAGE_LIMIT(new Doc() //
			.unit(Unit.VOLT)),

	SINEXCEL_STATE_1(new Doc() //
			.level(Level.INFO) //
			.text("OFF")), //
	SINEXCEL_STATE_2(new Doc() //
			.level(Level.INFO) //
			.text("Sleeping")), //
	SINEXCEL_STATE_3(new Doc() //
			.level(Level.INFO) //
			.text("Starting")), //
	SINEXCEL_STATE_4(new Doc() //
			.level(Level.INFO) //
			.text("MPPT")), //
	SINEXCEL_STATE_5(new Doc() //
			.level(Level.INFO) //
			.text("Throttled")), //
	SINEXCEL_STATE_6(new Doc() //
			.level(Level.INFO) //
			.text("Shutting down")), //
	SINEXCEL_STATE_7(new Doc(). //
			level(Level.INFO) //
			.text("Fault")), //
	SINEXCEL_STATE_8(new Doc() //
			.level(Level.INFO) //
			.text("Standby")), //
	SINEXCEL_STATE_9(new Doc() //
			.level(Level.INFO) //
			.text("Started")), //

	// EVENT Bitfield 32
	STATE_0(new Doc() //
			.level(Level.FAULT) //
			.text("Ground fault")), //
	STATE_1(new Doc() //
			.level(Level.WARNING) //
			.text("DC over Voltage")), //
	STATE_2(new Doc() //
			.level(Level.WARNING) //
			.text("AC disconnect open")), //
	STATE_3(new Doc() //
			.level(Level.WARNING) //
			.text("DC disconnect open")), //
	STATE_4(new Doc() //
			.level(Level.WARNING) //
			.text("Grid shutdown")), //
	STATE_5(new Doc() //
			.level(Level.WARNING) //
			.text("Cabinet open")), //
	STATE_6(new Doc() //
			.level(Level.WARNING) //
			.text("Manual shutdown")), //
	STATE_7(new Doc() //
			.level(Level.WARNING) //
			.text("Over temperature")), //
	STATE_8(new Doc() //
			.level(Level.WARNING) //
			.text("AC Frequency above limit")), //
	STATE_9(new Doc() //
			.level(Level.WARNING) //
			.text("AC Frequnecy under limit")), //
	STATE_10(new Doc() //
			.level(Level.WARNING) //
			.text("AC Voltage above limit")), //
	STATE_11(new Doc() //
			.level(Level.WARNING) //
			.text("AC Voltage under limit")), //
	STATE_12(new Doc() //
			.level(Level.WARNING) //
			.text("Blown String fuse on input")), //
	STATE_13(new Doc() //
			.level(Level.WARNING) //
			.text("Under temperature")), //
	STATE_14(new Doc() //
			.level(Level.WARNING) //
			.text("Generic Memory or Communication error (internal)")), //
	STATE_15(new Doc() //
			.level(Level.FAULT) //
			.text("Hardware test failure")), //

	// FAULT LIST
	STATE_16(new Doc() //
			.level(Level.FAULT) //
			.text("Fault Status")), //
	STATE_17(new Doc() //
			.level(Level.WARNING) //
			.text("Alert Status")), //
	STATE_18(new Doc() //
			.level(Level.INFO) //
			.text("On/Off Status")), //
	STATE_19(new Doc() //
			.level(Level.INFO) //
			.text("On Grid") //
			.onInit(c -> { //
				StateChannel channel = (StateChannel) c;
				EssSinexcel self = (EssSinexcel) channel.getComponent();
				((StateChannel) channel).onChange(v -> {
					Optional<Boolean> value = v.asOptional();
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
	STATE_20(new Doc() //
			.level(Level.INFO) //
			.text("Off Grid")), //
	STATE_21(new Doc() //
			.level(Level.WARNING) //
			.text("AC OVP")), //
	STATE_22(new Doc() //
			.level(Level.WARNING) //
			.text("AC UVP")), //
	STATE_23(new Doc() //
			.level(Level.WARNING) //
			.text("AC OFP")), //
	STATE_24(new Doc() //
			.level(Level.WARNING) //
			.text("AC UFP")), //
	STATE_25(new Doc() //
			.level(Level.WARNING) //
			.text("Grid Voltage Unbalance")), //
	STATE_26(new Doc() //
			.level(Level.WARNING) //
			.text("Grid Phase reserve")), //
	STATE_27(new Doc() //
			.level(Level.INFO) //
			.text("Islanding")), //
	STATE_28(new Doc() //
			.level(Level.WARNING) //
			.text("On/ Off Grid Switching Error")), //
	STATE_29(new Doc() //
			.level(Level.WARNING) //
			.text("Output Grounding Error")), //
	STATE_30(new Doc() //
			.level(Level.WARNING) //
			.text("Output Current Abnormal")), //
	STATE_31(new Doc() //
			.level(Level.WARNING) //
			.text("Grid Phase Lock Fails")), //
	STATE_32(new Doc() //
			.level(Level.WARNING) //
			.text("Internal Air Over-Temp")), //
	STATE_33(new Doc() //
			.level(Level.WARNING) //
			.text("Zeitueberschreitung der Netzverbindung")), //
	STATE_34(new Doc() //
			.level(Level.INFO) //
			.text("EPO")), //
	STATE_35(new Doc() //
			.level(Level.FAULT) //
			.text("HMI Parameters Fault")), //
	STATE_36(new Doc() //
			.level(Level.WARNING) //
			.text("DSP Version Error")), //
	STATE_37(new Doc() //
			.level(Level.WARNING) //
			.text("CPLD Version Error")), //
	STATE_38(new Doc() //
			.level(Level.WARNING) //
			.text("Hardware Version Error")), //
	STATE_39(new Doc() //
			.level(Level.WARNING) //
			.text("Communication Error")), //
	STATE_40(new Doc() //
			.level(Level.WARNING) //
			.text("AUX Power Error")), //
	STATE_41(new Doc() //
			.level(Level.FAULT) //
			.text("Fan Failure")), //
	STATE_42(new Doc() //
			.level(Level.WARNING) //
			.text("BUS Over Voltage")), //
	STATE_43(new Doc() //
			.level(Level.WARNING) //
			.text("BUS Low Voltage")), //
	STATE_44(new Doc() //
			.level(Level.WARNING) //
			.text("BUS Voltage Unbalanced")), //
	STATE_45(new Doc() //
			.level(Level.WARNING) //
			.text("AC Soft Start Failure")), //
	STATE_46(new Doc() //
			.level(Level.WARNING) //
			.text("Reserved")), //
	STATE_47(new Doc() //
			.level(Level.WARNING) //
			.text("Output Voltage Abnormal")), //
	STATE_48(new Doc() //
			.level(Level.WARNING) //
			.text("Output Current Unbalanced")), //
	STATE_49(new Doc() //
			.level(Level.WARNING) //
			.text("Over Temperature of Heat Sink")), //
	STATE_50(new Doc() //
			.level(Level.WARNING) //
			.text("Output Overload")), //
	STATE_51(new Doc() //
			.level(Level.WARNING) //
			.text("Reserved")), //
	STATE_52(new Doc() //
			.level(Level.WARNING) //
			.text("AC Breaker Short-Circuit")), //
	STATE_53(new Doc() //
			.level(Level.WARNING) //
			.text("Inverter Start Failure")), //
	STATE_54(new Doc() //
			.level(Level.WARNING) //
			.text("AC Breaker is open")), //
	STATE_55(new Doc() //
			.level(Level.WARNING) //
			.text("EE Reading Error 1")), //
	STATE_56(new Doc() //
			.level(Level.WARNING) //
			.text("EE Reading Error 2")), //
	STATE_57(new Doc() //
			.level(Level.FAULT) //
			.text("SPD Failure  ")), //
	STATE_58(new Doc() //
			.level(Level.WARNING) //
			.text("Inverter over load")), //
	STATE_59(new Doc() //
			.level(Level.INFO) //
			.text("DC Charging")), //
	STATE_60(new Doc() //
			.level(Level.INFO) //
			.text("DC Discharging")), //
	STATE_61(new Doc() //
			.level(Level.INFO) //
			.text("Battery fully charged")), //
	STATE_62(new Doc() //
			.level(Level.INFO) //
			.text("Battery empty")), //
	STATE_63(new Doc() //
			.level(Level.FAULT) //
			.text("Fault Status")), //
	STATE_64(new Doc() //
			.level(Level.WARNING) //
			.text("Alert Status")), //
	STATE_65(new Doc() //
			.level(Level.WARNING) //
			.text("DC input OVP")), //
	STATE_66(new Doc() //
			.level(Level.WARNING) //
			.text("DC input UVP")), //
	STATE_67(new Doc() //
			.level(Level.WARNING) //
			.text("DC Groundig Error")), //
	STATE_68(new Doc() //
			.level(Level.WARNING) //
			.text("BMS alerts")), //
	STATE_69(new Doc() //
			.level(Level.FAULT) //
			.text("DC Soft-Start failure")), //
	STATE_70(new Doc() //
			.level(Level.WARNING) //
			.text("DC relay short-circuit")), //
	STATE_71(new Doc() //
			.level(Level.WARNING) //
			.text("DC realy short open")), //
	STATE_72(new Doc() //
			.level(Level.WARNING) //
			.text("Battery power over load")), //
	STATE_73(new Doc() //
			.level(Level.FAULT) //
			.text("BUS start fails")), //
	STATE_74(new Doc() //
			.level(Level.WARNING) //
			.text("DC OCP")), //
	STATE_UNABLE_TO_SET_BATTERY_RANGES(new Doc() //
			.level(Level.FAULT) //
			.text("Unable to set battery ranges")); //

	private final Doc doc;

	private SinexcelChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}