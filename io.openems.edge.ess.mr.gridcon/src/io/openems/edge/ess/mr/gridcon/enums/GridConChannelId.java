package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatDoc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.LongDoc;

/**
 * This enum holds every possible channel id for a gridcon.
 */
public enum GridConChannelId implements ChannelId {
	CCU_STATE(Doc.of(CcuState.values())), // = 1
	CCU_ERROR_COUNT(Doc.of(OpenemsType.INTEGER)), //
	CCU_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
	CCU_VOLTAGE_U12(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	CCU_VOLTAGE_U23(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	CCU_VOLTAGE_U31(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	CCU_CURRENT_IL1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	CCU_CURRENT_IL2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	CCU_CURRENT_IL3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	/**
	 * Active power.
	 */
	CCU_POWER_P(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	/**
	 * Reactive power.
	 */
	CCU_POWER_Q(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE)), //
	CCU_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),

	INVERTER_1_STATUS_STATE_MACHINE(Doc.of(StatusIpuStateMachine.values())),
	INVERTER_1_STATUS_MCU(Doc.of(StatusIpuStatusMcu.values())),
	INVERTER_1_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_1_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_1_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	INVERTER_1_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	INVERTER_1_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
	INVERTER_1_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
	INVERTER_1_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_1_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	INVERTER_2_STATUS_STATE_MACHINE(Doc.of(StatusIpuStateMachine.values())),
	INVERTER_2_STATUS_MCU(Doc.of(StatusIpuStatusMcu.values())),
	INVERTER_2_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_2_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_2_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	INVERTER_2_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	INVERTER_2_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
	INVERTER_2_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
	INVERTER_2_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_2_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	INVERTER_3_STATUS_STATE_MACHINE(Doc.of(StatusIpuStateMachine.values())),
	INVERTER_3_STATUS_MCU(Doc.of(StatusIpuStatusMcu.values())),
	INVERTER_3_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_3_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	INVERTER_3_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	INVERTER_3_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	INVERTER_3_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
	INVERTER_3_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
	INVERTER_3_STATUS_TEMPERATURE_IGBT_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_MCU_BOARD(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_GRID_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_TEMPERATURE_INVERTER_CHOKE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_1(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
	INVERTER_3_STATUS_RESERVE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),

	DCDC_STATUS_STATE_MACHINE(Doc.of(StatusIpuStateMachine.values())),
	DCDC_STATUS_MCU(Doc.of(StatusIpuStatusMcu.values())), //
	DCDC_STATUS_FILTER_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_STATUS_DC_LINK_NEGATIVE_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
	DCDC_STATUS_DC_LINK_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
	DCDC_STATUS_DC_LINK_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
	DCDC_STATUS_DC_LINK_UTILIZATION(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
	DCDC_STATUS_FAN_SPEED_MAX(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)), //
	DCDC_STATUS_FAN_SPEED_MIN(Doc.of(OpenemsType.LONG).unit(Unit.PERCENT)),
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

	DCDC_MEASUREMENTS_CURRENT_STRING_A_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),
	DCDC_MEASUREMENTS_CURRENT_STRING_B_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),
	DCDC_MEASUREMENTS_CURRENT_STRING_C_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),

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

	GRID_MEASUREMENT_I_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	GRID_MEASUREMENT_I_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	GRID_MEASUREMENT_I_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
	GRID_MEASUREMENT_I_LN(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //

	GRID_MEASUREMENT_P_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
	GRID_MEASUREMENT_P_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
	GRID_MEASUREMENT_P_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
	GRID_MEASUREMENT_P_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //

	GRID_MEASUREMENT_Q_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE)), //
	GRID_MEASUREMENT_Q_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE)), //
	GRID_MEASUREMENT_Q_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE)), //
	GRID_MEASUREMENT_Q_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE)), //

	COMMAND_CONTROL_WORD_PLAY_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_PLAY(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_PLAY_DEBUG)),
	COMMAND_CONTROL_WORD_READY_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_READY(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_READY_DEBUG)),
	COMMAND_CONTROL_WORD_ACKNOWLEDGE_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ACKNOWLEDGE(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE_DEBUG)),
	COMMAND_CONTROL_WORD_STOP_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_STOP(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_STOP_DEBUG)),
	COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL_DEBUG)),
	COMMAND_CONTROL_WORD_SYNC_APPROVAL_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_SYNC_APPROVAL(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL_DEBUG)),
	COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING_DEBUG)),
	COMMAND_CONTROL_WORD_MODE_SELECTION_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_MODE_SELECTION(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION_DEBUG)),
	COMMAND_CONTROL_WORD_TRIGGER_SIA_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_TRIGGER_SIA(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA_DEBUG)),
	COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_1_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_1_DEBUG)),
	COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_2_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_2(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_2_DEBUG)),
	COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_1_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_1_DEBUG)),
	COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_2_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_2(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_2_DEBUG)),
	COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_1_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_1_DEBUG)),
	COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_2_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_2(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_2_DEBUG)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_4_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ENABLE_IPU_4(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_4_DEBUG)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_3_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ENABLE_IPU_3(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_3_DEBUG)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_2_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ENABLE_IPU_2(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_2_DEBUG)),
	COMMAND_CONTROL_WORD_DISABLE_IPU_1_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
	COMMAND_CONTROL_WORD_ENABLE_IPU_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_WORD_DISABLE_IPU_1_DEBUG)),
	COMMAND_ERROR_CODE_FEEDBACK_DEBUG(Doc.of(OpenemsType.LONG).unit(Unit.NONE)), //
	COMMAND_ERROR_CODE_FEEDBACK(new LongDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK_DEBUG)),

	COMMAND_CONTROL_PARAMETER_U0_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the voltage provided in a blackstart where 1 is mains voltage. 1
	 * =&gt; 230V, 1.02 =&gt; 234.6V. Should be 1 when not using blackstart, because
	 * when system runs into blackstart mode
	 */
	COMMAND_CONTROL_PARAMETER_U0(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0_DEBUG)),

	COMMAND_CONTROL_PARAMETER_F0_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the frequency.
	 */
	COMMAND_CONTROL_PARAMETER_F0(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0_DEBUG)),

	COMMAND_CONTROL_PARAMETER_Q_REF_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the reactive power.
	 */
	COMMAND_CONTROL_PARAMETER_Q_REF(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF_DEBUG)),

	COMMAND_CONTROL_PARAMETER_P_REF_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	/**
	 * Describes the active power.
	 */
	COMMAND_CONTROL_PARAMETER_P_REF(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF_DEBUG)),

	COMMAND_TIME_SYNC_DATE_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), //
	COMMAND_TIME_SYNC_DATE(new IntegerDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_TIME_SYNC_DATE_DEBUG)),

	COMMAND_TIME_SYNC_TIME_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), //
	COMMAND_TIME_SYNC_TIME(new IntegerDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.COMMAND_TIME_SYNC_TIME_DEBUG)),

	CONTROL_PARAMETER_U_Q_DROOP_MAIN_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_MAIN_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_LOWER_DEBUG)),
	CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER_DEBUG)),

	CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN_DEBUG)),

	CONTROL_PARAMETER_F_P_DRROP_MAIN_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DROOP_MAIN_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DRROP_MAIN_LOWER_DEBUG)),

	CONTROL_PARAMETER_F_P_DRROP_MAIN_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DROOP_MAIN_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DRROP_MAIN_UPPER_DEBUG)),

	CONTROL_PARAMETER_F_P_DROOP_T1_MAIN_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_F_P_DROOP_T1_MAIN(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN_DEBUG)),

	CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER_DEBUG)),

	CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER_DEBUG)),

	CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER_DEBUG)),

	CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)),
	CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER_DEBUG)),

	CONTROL_PARAMETER_Q_LIMIT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_Q_LIMIT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT_DEBUG)),

	CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER_DEBUG)),
	CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER_DEBUG)),

	CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER_DEBUG)),
	CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER_DEBUG)),

	CONTROL_PARAMETER_P_U_DROOP_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DROOP_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_LOWER_DEBUG)),
	CONTROL_PARAMETER_P_U_DROOP_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DROOP_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_UPPER_DEBUG)),

	CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER_DEBUG)),
	CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER_DEBUG)),

	CONTROL_PARAMETER_P_U_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE_DEBUG)),

	CONTROL_PARAMETER_P_U_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_U_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE_DEBUG)),

	CONTROL_PARAMETER_P_CONTROL_MODE_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_MODE(new IntegerDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE_DEBUG)),

	CONTROL_PARAMETER_P_CONTROL_LIM_TWO_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_LIM_TWO(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO_DEBUG)),

	CONTROL_PARAMETER_P_CONTROL_LIM_ONE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_P_CONTROL_LIM_ONE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE_DEBUG)),

	CONTROL_PARAMETER_COS_PHI_SETPOINT_1_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_COS_PHI_SETPOINT_1(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_COS_PHI_SETPOINT_1_DEBUG)),
	CONTROL_PARAMETER_COS_PHI_SETPOINT_2_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	CONTROL_PARAMETER_COS_PHI_SETPOINT_2(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.CONTROL_PARAMETER_COS_PHI_SETPOINT_2_DEBUG)),

	INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG)),

	INVERTER_1_CONTROL_DC_CURRENT_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_DC_CURRENT_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT_DEBUG)),

	INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_1_CONTROL_P_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE_DEBUG)),

	INVERTER_1_CONTROL_P_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_1_CONTROL_P_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE_DEBUG)),

	INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG)),

	INVERTER_2_CONTROL_DC_CURRENT_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_DC_CURRENT_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT_DEBUG)),

	INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_2_CONTROL_P_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE_DEBUG)),

	INVERTER_2_CONTROL_P_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_2_CONTROL_P_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE_DEBUG)),

	INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT_DEBUG)),

	INVERTER_3_CONTROL_DC_CURRENT_SETPOINT_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_DC_CURRENT_SETPOINT(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT_DEBUG)),

	INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE).onChannelSetNextWriteMirrorToDebugChannel(
					GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE_DEBUG)),

	INVERTER_3_CONTROL_P_MAX_DISCHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_MAX_DISCHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE_DEBUG)),

	INVERTER_3_CONTROL_P_MAX_CHARGE_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	INVERTER_3_CONTROL_P_MAX_CHARGE(new FloatDoc() //
			.accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE_DEBUG)),

	DCDC_CONTROL_DC_VOLTAGE_SETPOINT(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	DCDC_CONTROL_WEIGHT_STRING_A_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	DCDC_CONTROL_WEIGHT_STRING_A(new FloatDoc().unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A_DEBUG)),
	DCDC_CONTROL_WEIGHT_STRING_B_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	DCDC_CONTROL_WEIGHT_STRING_B(new FloatDoc().unit(Unit.NONE).accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B_DEBUG)),
	DCDC_CONTROL_WEIGHT_STRING_C_DEBUG(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE)), //
	DCDC_CONTROL_WEIGHT_STRING_C(new FloatDoc().unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)
			.onChannelSetNextWriteMirrorToDebugChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C_DEBUG)),

	DCDC_CONTROL_I_REF_STRING_A_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
	DCDC_CONTROL_I_REF_STRING_B_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
	DCDC_CONTROL_I_REF_STRING_C_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //

	DCDC_CONTROL_I_REF_STRING_A(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_I_REF_STRING_B(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
	DCDC_CONTROL_I_REF_STRING_C(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //

	DCDC_CONTROL_STRING_CONTROL_MODE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),;

	private final Doc doc;

	private GridConChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}