package io.openems.edge.ess.mr.gridcon.enums;

import java.util.function.Consumer;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;

/**
 * This enum holds every possible channel id for a gridcon.
 */
public enum GridConChannelId implements io.openems.edge.common.channel.doc.ChannelId {

	CCU_STATE_IDLE(new Doc()), CCU_STATE_PRECHARGE(new Doc()), CCU_STATE_STOP_PRECHARGE(new Doc()),
	CCU_STATE_READY(new Doc()), CCU_STATE_PAUSE(new Doc()), CCU_STATE_RUN(new Doc()), CCU_STATE_ERROR(new Doc()),
	CCU_STATE_VOLTAGE_RAMPING_UP(new Doc()), CCU_STATE_OVERLOAD(new Doc()), CCU_STATE_SHORT_CIRCUIT_DETECTED(new Doc()),
	CCU_STATE_DERATING_POWER(new Doc()), CCU_STATE_DERATING_HARMONICS(new Doc()), CCU_STATE_SIA_ACTIVE(new Doc()),
	CCU_ERROR_CODE(new Doc()), CCU_VOLTAGE_U12(new Doc().unit(Unit.VOLT)), CCU_VOLTAGE_U23(new Doc().unit(Unit.VOLT)),
	CCU_VOLTAGE_U31(new Doc().unit(Unit.VOLT)), CCU_CURRENT_IL1(new Doc().unit(Unit.AMPERE)),
	CCU_CURRENT_IL2(new Doc().unit(Unit.AMPERE)), CCU_CURRENT_IL3(new Doc().unit(Unit.AMPERE)),
	/**
	 * active power
	 */
	CCU_POWER_P(new Doc().unit(Unit.WATT)),
	/**
	 * reactive power
	 */
	CCU_POWER_Q(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), CCU_FREQUENCY(new Doc().unit(Unit.HERTZ)),

	INVERTER_1_STATUS_STATE_MACHINE(new Doc().options(StatusIPUStateMachine.values())),
	INVERTER_1_STATUS_MCU(new Doc().options(StatusIPUStatusMCU.values())),
	INVERTER_1_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
	INVERTER_1_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	INVERTER_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	INVERTER_1_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
	INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.WATT)),
	INVERTER_1_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
	INVERTER_1_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
	INVERTER_1_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
	INVERTER_1_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

	INVERTER_2_STATUS_STATE_MACHINE(new Doc().options(StatusIPUStateMachine.values())),
	INVERTER_2_STATUS_MCU(new Doc().options(StatusIPUStatusMCU.values())),
	INVERTER_2_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
	INVERTER_2_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	INVERTER_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	INVERTER_2_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
	INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.WATT)),
	INVERTER_2_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
	INVERTER_2_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
	INVERTER_2_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
	INVERTER_2_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

	INVERTER_3_STATUS_STATE_MACHINE(new Doc().options(StatusIPUStateMachine.values())),
	INVERTER_3_STATUS_MCU(new Doc().options(StatusIPUStatusMCU.values())),
	INVERTER_3_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
	INVERTER_3_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	INVERTER_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	INVERTER_3_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
	INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.WATT)),
	INVERTER_3_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
	INVERTER_3_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
	INVERTER_3_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
	INVERTER_3_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

	DCDC_STATUS_STATE_MACHINE(new Doc().options(StatusIPUStateMachine.values())),
	DCDC_STATUS_MCU(new Doc().options(StatusIPUStatusMCU.values())),
	DCDC_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
	DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	DCDC_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
	DCDC_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
	DCDC_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.WATT)),
	DCDC_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
	DCDC_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)), DCDC_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
	DCDC_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

	DCDC_MEASUREMENTS_VOLTAGE_STRING_A(new Doc().unit(Unit.VOLT)),
	DCDC_MEASUREMENTS_VOLTAGE_STRING_B(new Doc().unit(Unit.VOLT)),
	DCDC_MEASUREMENTS_VOLTAGE_STRING_C(new Doc().unit(Unit.VOLT)),
	DCDC_MEASUREMENTS_CURRENT_STRING_A(new Doc().unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_CURRENT_STRING_B(new Doc().unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_CURRENT_STRING_C(new Doc().unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_POWER_STRING_A(new Doc().unit(Unit.KILOWATT)),
	DCDC_MEASUREMENTS_POWER_STRING_B(new Doc().unit(Unit.KILOWATT)),
	DCDC_MEASUREMENTS_POWER_STRING_C(new Doc().unit(Unit.KILOWATT)),
	DCDC_MEASUREMENTS_UTILIZATION_STRING_A(new Doc().unit(Unit.PERCENT)),
	DCDC_MEASUREMENTS_UTILIZATION_STRING_B(new Doc().unit(Unit.PERCENT)),
	DCDC_MEASUREMENTS_UTILIZATION_STRING_C(new Doc().unit(Unit.PERCENT)),
	DCDC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT(new Doc().unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION(new Doc().unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_RESERVE_1(new Doc().unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_RESERVE_2(new Doc().unit(Unit.PERCENT)),

	COMMAND_CONTROL_WORD_PLAY_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_PLAY(new Doc().unit(Unit.ON_OFF) //
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_PLAY_DEBUG))),

	COMMAND_CONTROL_WORD_READY_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_READY(new Doc().unit(Unit.ON_OFF) //
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_READY_DEBUG))),

	COMMAND_CONTROL_WORD_ACKNOWLEDGE_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ACKNOWLEDGE(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE_DEBUG))),

	COMMAND_CONTROL_WORD_STOP_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_STOP(new Doc().unit(Unit.ON_OFF) //
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_STOP_DEBUG))),

	COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL(new Doc().unit(Unit.ON_OFF) //
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL_DEBUG))),

	COMMAND_CONTROL_WORD_SYNC_APPROVAL_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_SYNC_APPROVAL(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL_DEBUG))),

	COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING(new Doc().unit(Unit.ON_OFF) //
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING_DEBUG))),

	COMMAND_CONTROL_WORD_MODE_SELECTION_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_MODE_SELECTION(new Doc().unit(Unit.ON_OFF) // 0=voltage control, 1=current control
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION_DEBUG))),

	COMMAND_CONTROL_WORD_TRIGGER_SIA_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_TRIGGER_SIA(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA_DEBUG))),

	COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION_DEBUG))),

	COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET_DEBUG))),

	COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET_DEBUG))),

	COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET_DEBUG))),

	COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET_DEBUG(new Doc().unit(Unit.ON_OFF)), //
	COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)
			.onInit(new BooleanDebug(GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET_DEBUG))),

	COMMAND_CONTROL_WORD_DISABLE_IPU_4(new Doc().unit(Unit.ON_OFF)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_3(new Doc().unit(Unit.ON_OFF)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_2(new Doc().unit(Unit.ON_OFF)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_1(new Doc().unit(Unit.ON_OFF)), //

	COMMAND_ERROR_CODE_FEEDBACK_DEBUG(new Doc().unit(Unit.NONE)), //
	COMMAND_ERROR_CODE_FEEDBACK(new Doc() //
			.onInit(new LongDebug(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK_DEBUG))),

	COMMAND_CONTROL_PARAMETER_U0_DEBUG(new Doc().unit(Unit.NONE)), //
	/**
	 * Describes the voltage provided in a blackstart where 1 is mains voltage. 1
	 * =&gt; 230V, 1.02 =&gt; 234.6V. Should be 1 when not using blackstart, because
	 * when system runs into blackstart mode
	 */
	COMMAND_CONTROL_PARAMETER_U0(new Doc() //
			.onInit(new FloatDebug(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0_DEBUG))),

	COMMAND_CONTROL_PARAMETER_F0_DEBUG(new Doc().unit(Unit.NONE)), //
	/**
	 * Describes the frequency
	 */
	COMMAND_CONTROL_PARAMETER_F0(new Doc() //
			.onInit(new FloatDebug(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0_DEBUG))),

	COMMAND_CONTROL_PARAMETER_Q_REF_DEBUG(new Doc().unit(Unit.NONE)), //
	/**
	 * Describes the reactive power
	 */
	COMMAND_CONTROL_PARAMETER_Q_REF(new Doc() //
			.onInit(new FloatDebug(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF_DEBUG))),

	COMMAND_CONTROL_PARAMETER_P_REF_DEBUG(new Doc().unit(Unit.NONE)), //
	/**
	 * Describes the active power
	 */
	COMMAND_CONTROL_PARAMETER_P_REF(new Doc() //
			.onInit(new FloatDebug(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF_DEBUG))),

	COMMAND_TIME_SYNC_DATE_DEBUG(new Doc().unit(Unit.NONE)), //
	COMMAND_TIME_SYNC_DATE(new Doc() //
			.onInit(new LongDebug(GridConChannelId.COMMAND_TIME_SYNC_DATE_DEBUG))),

	COMMAND_TIME_SYNC_TIME_DEBUG(new Doc().unit(Unit.NONE)), //
	COMMAND_TIME_SYNC_TIME(new Doc() //
			.onInit(new LongDebug(GridConChannelId.COMMAND_TIME_SYNC_TIME_DEBUG))),

	CONTROL_PARAMETER_U_Q_DROOP_MAIN_DEBUG(new Doc().unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_MAIN(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_DEBUG))),

	CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN_DEBUG(new Doc().unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN(new Doc().unit(Unit.SECONDS) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN_DEBUG))),

	CONTROL_PARAMETER_F_P_DRROP_MAIN_DEBUG(new Doc().unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DRROP_MAIN(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_F_P_DRROP_MAIN_DEBUG))),

	CONTROL_PARAMETER_F_P_DROOP_T1_MAIN_DEBUG(new Doc().unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DROOP_T1_MAIN(new Doc().unit(Unit.SECONDS)
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN_DEBUG))),

	CONTROL_PARAMETER_Q_U_DROOP_MAIN_DEBUG(new Doc().unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DROOP_MAIN(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_DEBUG))),

	CONTROL_PARAMETER_Q_U_DEAD_BAND_DEBUG(new Doc().unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DEAD_BAND(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_DEBUG))),

	CONTROL_PARAMETER_Q_LIMIT_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_Q_LIMIT(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT_DEBUG))),

	CONTROL_PARAMETER_P_F_DROOP_MAIN_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DROOP_MAIN(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_DEBUG))),

	CONTROL_PARAMETER_P_F_DEAD_BAND_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DEAD_BAND(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_DEBUG))),

	CONTROL_PARAMETER_P_U_DROOP_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DROOP(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_DEBUG))),

	CONTROL_PARAMETER_P_U_DEAD_BAND_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DEAD_BAND(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_DEBUG))),

	CONTROL_PARAMETER_P_U_MAX_CHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_MAX_CHARGE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE_DEBUG))),

	CONTROL_PARAMETER_P_U_MAX_DISCHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_MAX_DISCHARGE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE_DEBUG))),

	CONTROL_PARAMETER_P_CONTROL_MODE_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_MODE(new Doc().options(PControlMode.values()) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE_DEBUG))),

	CONTROL_PARAMETER_P_CONTROL_LIM_TWO_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_LIM_TWO(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO_DEBUG))),

	CONTROL_PARAMETER_P_CONTROL_LIM_ONE_DEBUG(new Doc().unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_LIM_ONE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE_DEBUG))),

	INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG))),

	INVERTER_1_CONTROL_DC_CURRENT_SETPOINT_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_DC_CURRENT_SETPOINT(new Doc().unit(Unit.AMPERE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT_DEBUG))),

	INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_P_MAX_DISCHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_MAX_DISCHARGE(new Doc().unit(Unit.WATT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE_DEBUG))),

	INVERTER_1_CONTROL_P_MAX_CHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_MAX_CHARGE(new Doc().unit(Unit.WATT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE_DEBUG))),

	INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG))),

	INVERTER_2_CONTROL_DC_CURRENT_SETPOINT_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_DC_CURRENT_SETPOINT(new Doc().unit(Unit.AMPERE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT_DEBUG))),

	INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_P_MAX_DISCHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_MAX_DISCHARGE(new Doc().unit(Unit.WATT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE_DEBUG))),

	INVERTER_2_CONTROL_P_MAX_CHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_MAX_CHARGE(new Doc().unit(Unit.WATT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE_DEBUG))),

	INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG))),

	INVERTER_3_CONTROL_DC_CURRENT_SETPOINT_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_DC_CURRENT_SETPOINT(new Doc().unit(Unit.AMPERE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT_DEBUG))),

	INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_P_MAX_DISCHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_MAX_DISCHARGE(new Doc().unit(Unit.WATT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE_DEBUG))),

	INVERTER_3_CONTROL_P_MAX_CHARGE_DEBUG(new Doc().unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_MAX_CHARGE(new Doc().unit(Unit.WATT) //
			.onInit(new FloatDebug(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE_DEBUG))),

	DCDC_CONTROL_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT)),
	DCDC_CONTROL_WEIGHT_STRING_A(new Doc().unit(Unit.NONE)), //
	DCDC_CONTROL_WEIGHT_STRING_B(new Doc().unit(Unit.NONE)), //
	DCDC_CONTROL_WEIGHT_STRING_C(new Doc().unit(Unit.NONE)), //
	DCDC_CONTROL_I_REF_STRING_A(new Doc().unit(Unit.NONE)), //
	DCDC_CONTROL_I_REF_STRING_B(new Doc().unit(Unit.NONE)), //
	DCDC_CONTROL_I_REF_STRING_C(new Doc().unit(Unit.NONE)), //
	DCDC_CONTROL_STRING_CONTROL_MODE(new Doc().unit(Unit.NONE)),

	STATE_CYCLE_ERROR(new Doc().level(Level.FAULT));

	private final Doc doc;

	private GridConChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}

	private static class BooleanDebug implements Consumer<Channel<?>> {

		private final GridConChannelId targetChannelId;

		public BooleanDebug(GridConChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<?> channel) {
			((BooleanWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	};

	private static class FloatDebug implements Consumer<Channel<?>> {

		private final GridConChannelId targetChannelId;

		public FloatDebug(GridConChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<?> channel) {
			((FloatWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	};

	private static class LongDebug implements Consumer<Channel<?>> {

		private final GridConChannelId targetChannelId;

		public LongDebug(GridConChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<?> channel) {
			((LongWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	};
}