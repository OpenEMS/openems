package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatDoc;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;

/**
 * This enum holds every possible channel id for a gridcon.
 */
public enum GridConChannelId implements ChannelId {

	CCU_STATE_IDLE(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_PRECHARGE(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_STOP_PRECHARGE(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_READY(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_PAUSE(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_RUN(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_ERROR(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_VOLTAGE_RAMPING_UP(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_OVERLOAD(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_SHORT_CIRCUIT_DETECTED(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_DERATING_POWER(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_DERATING_HARMONICS(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_STATE_SIA_ACTIVE(Doc.of(OpenemsType.BOOLEAN)), //
	CCU_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
	CCU_VOLTAGE_U12(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	CCU_VOLTAGE_U23(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	CCU_VOLTAGE_U31(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	CCU_CURRENT_IL1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	CCU_CURRENT_IL2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	CCU_CURRENT_IL3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	/**
	 * active power
	 */
	CCU_POWER_P(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	/**
	 * reactive power
	 */
	CCU_POWER_Q(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE)), //
	CCU_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),

	INVERTER_1_STATUS_STATE_MACHINE(Doc.of(StatusIPUStateMachine.values())),
	INVERTER_1_STATUS_MCU(Doc.of(StatusIPUStatusMCU.values())),
	INVERTER_1_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_1_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_1_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	INVERTER_1_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	INVERTER_1_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	INVERTER_1_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	INVERTER_1_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	INVERTER_2_STATUS_STATE_MACHINE(Doc.of(StatusIPUStateMachine.values())),
	INVERTER_2_STATUS_MCU(Doc.of(StatusIPUStatusMCU.values())),
	INVERTER_2_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_2_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_2_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	INVERTER_2_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	INVERTER_2_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	INVERTER_2_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	INVERTER_2_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	INVERTER_3_STATUS_STATE_MACHINE(Doc.of(StatusIPUStateMachine.values())),
	INVERTER_3_STATUS_MCU(Doc.of(StatusIPUStatusMCU.values())),
	INVERTER_3_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_3_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_3_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	INVERTER_3_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	INVERTER_3_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	INVERTER_3_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	INVERTER_3_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	DCDC_STATUS_STATE_MACHINE(Doc.of(StatusIPUStateMachine.values())),
	DCDC_STATUS_MCU(Doc.of(StatusIPUStatusMCU.values())), //
	DCDC_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	DCDC_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	DCDC_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
	DCDC_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
	DCDC_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	DCDC_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	DCDC_MEASUREMENTS_VOLTAGE_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_MEASUREMENTS_VOLTAGE_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_MEASUREMENTS_VOLTAGE_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_MEASUREMENTS_CURRENT_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_CURRENT_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_CURRENT_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_POWER_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT)),
	DCDC_MEASUREMENTS_POWER_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT)),
	DCDC_MEASUREMENTS_POWER_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT)),
	DCDC_MEASUREMENTS_UTILIZATION_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	DCDC_MEASUREMENTS_UTILIZATION_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	DCDC_MEASUREMENTS_UTILIZATION_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	DCDC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_MEASUREMENTS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	DCDC_MEASUREMENTS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),

	COMMAND_CONTROL_WORD_PLAY_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_PLAY(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_PLAY_DEBUG))),
	COMMAND_CONTROL_WORD_READY_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_READY(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_READY_DEBUG))),
	COMMAND_CONTROL_WORD_ACKNOWLEDGE_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ACKNOWLEDGE(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE_DEBUG))),
	COMMAND_CONTROL_WORD_STOP_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_STOP(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_STOP_DEBUG))),
	COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL_DEBUG))),
	COMMAND_CONTROL_WORD_SYNC_APPROVAL_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_SYNC_APPROVAL(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL_DEBUG))),
	COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING_DEBUG))),
	COMMAND_CONTROL_WORD_MODE_SELECTION_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_MODE_SELECTION(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION_DEBUG))),
	COMMAND_CONTROL_WORD_TRIGGER_SIA_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_TRIGGER_SIA(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA_DEBUG))),
	COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION_DEBUG))),
	COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET_DEBUG))),
	COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET_DEBUG))),
	COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET_DEBUG))),
	COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET_DEBUG))),
	COMMAND_CONTROL_WORD_DISABLE_IPU_4_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_DISABLE_IPU_4(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_4_DEBUG))),
	COMMAND_CONTROL_WORD_DISABLE_IPU_3_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_DISABLE_IPU_3(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_3_DEBUG))),
	COMMAND_CONTROL_WORD_DISABLE_IPU_2_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_DISABLE_IPU_2(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_2_DEBUG))),
	COMMAND_CONTROL_WORD_DISABLE_IPU_1_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_DISABLE_IPU_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_1_DEBUG))),
	COMMAND_ERROR_CODE_FEEDBACK_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), //
	COMMAND_ERROR_CODE_FEEDBACK(new IntegerDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK_DEBUG))),

	COMMAND_CONTROL_PARAMETER_U0_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the voltage provided in a blackstart where 1 is mains voltage. 1
	 * =&gt; 230V, 1.02 =&gt; 234.6V. Should be 1 when not using blackstart, because
	 * when system runs into blackstart mode
	 */
	COMMAND_CONTROL_PARAMETER_U0(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0_DEBUG))),

	COMMAND_CONTROL_PARAMETER_F0_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the frequency
	 */
	COMMAND_CONTROL_PARAMETER_F0(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0_DEBUG))),

	COMMAND_CONTROL_PARAMETER_Q_REF_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the reactive power
	 */
	COMMAND_CONTROL_PARAMETER_Q_REF(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF_DEBUG))),

	COMMAND_CONTROL_PARAMETER_P_REF_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the active power
	 */
	COMMAND_CONTROL_PARAMETER_P_REF(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF_DEBUG))),

	COMMAND_TIME_SYNC_DATE_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), //
	COMMAND_TIME_SYNC_DATE(new IntegerDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_TIME_SYNC_DATE_DEBUG))),

	COMMAND_TIME_SYNC_TIME_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), //
	COMMAND_TIME_SYNC_TIME(new IntegerDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(GridConChannelId.COMMAND_TIME_SYNC_TIME_DEBUG))),

	CONTROL_PARAMETER_U_Q_DROOP_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_DEBUG))),

	CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN_DEBUG))),

	CONTROL_PARAMETER_F_P_DRROP_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DROOP_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_F_P_DRROP_MAIN_DEBUG))),

	CONTROL_PARAMETER_F_P_DROOP_T1_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DROOP_T1_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN_DEBUG))),

	CONTROL_PARAMETER_Q_U_DROOP_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DROOP_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_DEBUG))),

	CONTROL_PARAMETER_Q_U_DEAD_BAND_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DEAD_BAND(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_DEBUG))),

	CONTROL_PARAMETER_Q_LIMIT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_Q_LIMIT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT_DEBUG))),

	CONTROL_PARAMETER_P_F_DROOP_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DROOP_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_DEBUG))),

	CONTROL_PARAMETER_P_F_DEAD_BAND_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DEAD_BAND(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_DEBUG))),

	CONTROL_PARAMETER_P_U_DROOP_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DROOP(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_DEBUG))),

	CONTROL_PARAMETER_P_U_DEAD_BAND_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DEAD_BAND(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_DEBUG))),

	CONTROL_PARAMETER_P_U_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE_DEBUG))),

	CONTROL_PARAMETER_P_U_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE_DEBUG))),

	CONTROL_PARAMETER_P_CONTROL_MODE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_MODE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE_DEBUG))),

	CONTROL_PARAMETER_P_CONTROL_LIM_TWO_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_LIM_TWO(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO_DEBUG))),

	CONTROL_PARAMETER_P_CONTROL_LIM_ONE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_LIM_ONE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE_DEBUG))),

	INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG))),

	INVERTER_1_CONTROL_DC_CURRENT_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_DC_CURRENT_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT_DEBUG))),

	INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_1_CONTROL_P_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE_DEBUG))),

	INVERTER_1_CONTROL_P_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE_DEBUG))),

	INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG))),

	INVERTER_2_CONTROL_DC_CURRENT_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_DC_CURRENT_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT_DEBUG))),

	INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_2_CONTROL_P_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE_DEBUG))),

	INVERTER_2_CONTROL_P_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE_DEBUG))),

	INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG))),

	INVERTER_3_CONTROL_DC_CURRENT_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_DC_CURRENT_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT_DEBUG))),

	INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG))),

	INVERTER_3_CONTROL_P_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE_DEBUG))),

	INVERTER_3_CONTROL_P_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onInit(new FloatWriteChannel.MirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE_DEBUG))),

	DCDC_CONTROL_DC_VOLTAGE_SETPOINT(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	DCDC_CONTROL_WEIGHT_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_WEIGHT_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_WEIGHT_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_I_REF_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_I_REF_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_I_REF_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_STRING_CONTROL_MODE(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),

	STATE_CYCLE_ERROR(Doc.of(Level.FAULT));

	private final Doc doc;

	private GridConChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}