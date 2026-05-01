package io.openems.edge.goodwe.common;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.EnableDisableOrUndefined;
import io.openems.edge.goodwe.common.enums.SafetyParameterEnums;

/**
 * Defines all Safety Parameter Settings of a GoodWe Inverter in the new Format
 * 2025.
 */
public interface GoodWePowerSetting {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// ################################################
		// Active Power Mode Settings
		// ################################################

		// --- Begin: General ---
		DEBUG_V2_APM_GENERAL_OUTPUT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_GENERAL_OUTPUT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_GENERAL_OUTPUT_ACTIVE_POWER)), //

		DEBUG_V2_APM_GENERAL_POWER_GRADIENT(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_GENERAL_POWER_GRADIENT(Doc.of(OpenemsType.INTEGER)// [0, 1200] s
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_GENERAL_POWER_GRADIENT)), //

		// --- Begin: Power/Frequency Curve (PF) (Overfrequenzy)---
		DEBUG_V2_APM_ENABLE_PF_OVERFREQUENZY_CURVE(Doc.of(EnableCurve.values())), //
		V2_APM_ENABLE_PF_OVERFREQUENZY_CURVE(Doc.of(EnableCurve.values())//
				.text("Power and Frequency Curve Enabled")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_ENABLE_PF_OVERFREQUENZY_CURVE)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_START(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_OVERFREQUENCY_START(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("PF Curve Overfrequency Start Point")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_START)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_OVERFREQUENCY_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_PER_HERTZ)//
				.text("PF Curve Overfrequency Reduction Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_SLOPE)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_DELAY_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_OVERFREQUENCY_DELAY_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("PF Curve Overfrequency Delay Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_DELAY_TIME)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE(Doc.of(OpenemsType.BOOLEAN)), //
		V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE(Doc.of(OpenemsType.BOOLEAN)//
				.text("PF Curve Overfrequency Fstop Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Over-frequency Hysteresis Point")//
				.accessMode(AccessMode.READ_WRITE)
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Over-frequency Delay Waiting Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME)), //

		DEBUG_V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_PN_PER_MINUTE)//
				.text("Over-frequency Hysteresis Power Recovery Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE)), //

		// --- Begin: Power/Frequency Curve (PF) (Underfrequenzy)---
		DEBUG_V2_APM_ENABLE_PF_UNDERFREQUENZY_CURVE(Doc.of(EnableCurve.values())), //
		V2_APM_ENABLE_PF_UNDERFREQUENZY_CURVE(Doc.of(EnableCurve.values())//
				.text("Power and Frequency Curve Enabled")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_ENABLE_PF_UNDERFREQUENZY_CURVE)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_THRESHOLD(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_UNDERFREQUENCY_THRESHOLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("PF Curve Underfrequency Threshold")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_THRESHOLD)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_UNDERFREQUENCY_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_PER_HERTZ)//
				.text("PF Curve Underfrequency Increase Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_SLOPE)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_DELAY_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_UNDERFREQUENCY_DELAY_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("PF Curve Underfrequency Delay Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_DELAY_TIME)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE(Doc.of(OpenemsType.BOOLEAN)), //
		V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE(Doc.of(OpenemsType.BOOLEAN)//
				.text("PF Curve Underfrequency Fstop Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Under-frequency Hysteresis Point")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Under-frequency Delay Waiting Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(
						ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME)), //

		DEBUG_V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_PN_PER_MINUTE)//
				.text("Under-frequency Hysteresis Power Recovery Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE)), //

		// --- Begin: PU Curve ---
		DEBUG_V2_APM_ENABLE_PU_CURVE(Doc.of(EnableCurve.values())), //
		V2_APM_ENABLE_PU_CURVE(Doc.of(EnableCurve.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_ENABLE_PU_CURVE)), //

		DEBUG_V2_APM_PU_V1_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V1_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V1_VOLTAGE)), //

		DEBUG_V2_APM_PU_V1_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V1_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V1_VALUE)), //

		DEBUG_V2_APM_PU_V2_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V2_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V2_VOLTAGE)), //

		DEBUG_V2_APM_PU_V2_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V2_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V2_VALUE)), //

		DEBUG_V2_APM_PU_V3_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V3_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V3_VOLTAGE)), //

		DEBUG_V2_APM_PU_V3_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V3_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V3_VALUE)), //

		DEBUG_V2_APM_PU_V4_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V4_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V4_VOLTAGE)), //

		DEBUG_V2_APM_PU_V4_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_V4_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_V4_VALUE)), //

		DEBUG_V2_APM_PU_OUTPUT_RESPONSE_MODE(Doc.of(SafetyParameterEnums.Vrt.GeneralRecoveryMode.values())), //
		V2_APM_PU_OUTPUT_RESPONSE_MODE(Doc.of(SafetyParameterEnums.Vrt.GeneralRecoveryMode.values())//
				.text("Response Mode")//
				.accessMode(AccessMode.READ_WRITE)
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_OUTPUT_RESPONSE_MODE)), //

		DEBUG_V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("P(U) PT1 Low-pass Filter Time Constant for Output Response Mode PT-1 Behaviour")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE)), //

		DEBUG_V2_APM_PU_PT1_TIME_CONSTANT_GRADIENT_MODE(Doc.of(OpenemsType.INTEGER)), //
		V2_APM_PU_PT1_TIME_CONSTANT_GRADIENT_MODE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_PN_PER_SECOND)//
				.text("P(U) PT1 Low-pass Filter Time Constant for Output Response Mode PT-1 Behaviour")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_APM_PU_PT1_TIME_CONSTANT_GRADIENT_MODE)), //

		// ################################################
		// Reactive Power Mode Settings
		// ################################################

		// --- Begin: Fixed Q ---
		DEBUG_V2_RPM_ENABLE_FIXED_Q(Doc.of(EnableCurve.values())), //
		V2_RPM_ENABLE_FIXED_Q(Doc.of(EnableCurve.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Enable Fixed Q")//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_ENABLE_FIXED_Q)), //

		DEBUG_V2_RPM_FIXED_Q_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_FIXED_Q_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_FIXED_Q_VALUE)), //

		// --- Begin: Q(U) Curve ---
		DEBUG_V2_RPM_QU_ENABLE_QU_CURVE(Doc.of(EnableCurve.values())), //
		V2_RPM_QU_ENABLE_QU_CURVE(Doc.of(EnableCurve.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_ENABLE_QU_CURVE)), //

		DEBUG_V2_RPM_QU_CURVE_MODE(Doc.of(SafetyParameterEnums.Rpm.Mode.values())), //
		V2_RPM_QU_CURVE_MODE(Doc.of(SafetyParameterEnums.Rpm.Mode.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_CURVE_MODE)), //

		DEBUG_V2_RPM_QU_VOLTAGE_DEAD_BAND(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_VOLTAGE_DEAD_BAND(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_VOLTAGE_DEAD_BAND)), //

		DEBUG_V2_RPM_QU_OVEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_OVEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.QMAX_PER_DECIPERCENT_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_OVEREXCITED_SLOPE)), //

		DEBUG_V2_RPM_QU_UNDEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_UNDEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.QMAX_PER_DECIPERCENT_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_UNDEREXCITED_SLOPE)), //

		DEBUG_V2_RPM_QU_V1_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V1_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V1_VOLTAGE)), //

		DEBUG_V2_RPM_QU_V1_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V1_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V1_VALUE)), //

		DEBUG_V2_RPM_QU_V2_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V2_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V2_VOLTAGE)), //

		DEBUG_V2_RPM_QU_V2_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V2_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V2_VALUE)), //

		DEBUG_V2_RPM_QU_V3_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V3_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V3_VOLTAGE)), //

		DEBUG_V2_RPM_QU_V3_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V3_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V3_VALUE)), //

		DEBUG_V2_RPM_QU_V4_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V4_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V4_VOLTAGE)), //

		DEBUG_V2_RPM_QU_V4_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_V4_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_V4_VALUE)), //

		DEBUG_V2_RPM_QU_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)// [0, 6000] 0.1s
				.unit(Unit.MILLISECONDS)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_TIME_CONSTANT)), //

		DEBUG_V2_RPM_QU_EXTENDED_FUNCTIONS(Doc.of(EnableDisableOrUndefined.values())), //
		V2_RPM_QU_EXTENDED_FUNCTIONS(Doc.of(EnableDisableOrUndefined.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_EXTENDED_FUNCTIONS)), //

		DEBUG_V2_RPM_QU_LOCK_IN_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_LOCK_IN_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_LOCK_IN_POWER)), //

		DEBUG_V2_RPM_QU_LOCK_OUT_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QU_LOCK_OUT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QU_LOCK_OUT_POWER)), //

		// --- Begin: Cos Phi(P) Curve ---
		DEBUG_V2_RPM_ENABLE_CURVE_COS_PHI_P(Doc.of(EnableCurve.values())), //
		V2_RPM_ENABLE_CURVE_COS_PHI_P(Doc.of(EnableCurve.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_ENABLE_CURVE_COS_PHI_P)), //

		DEBUG_V2_RPM_COS_PHI_P_CURVE_MODE(Doc.of(SafetyParameterEnums.Rpm.Mode.values())), //
		V2_RPM_COS_PHI_P_CURVE_MODE(Doc.of(SafetyParameterEnums.Rpm.Mode.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COS_PHI_P_CURVE_MODE)), //

		DEBUG_V2_RPM_COS_PHI_P_OVEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_COS_PHI_P_OVEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.QMAX_PER_DECIPERCENT_PN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COS_PHI_P_OVEREXCITED_SLOPE)), //

		DEBUG_V2_RPM_COS_PHI_P_UNDEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_COS_PHI_P_UNDEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.QMAX_PER_DECIPERCENT_PN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COS_PHI_P_UNDEREXCITED_SLOPE)), //

		DEBUG_V2_RPM_A_POINT_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_A_POINT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_A_POINT_POWER)), //

		DEBUG_V2_RPM_A_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_A_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_A_POINT_COS_PHI)), //

		DEBUG_V2_RPM_B_POINT_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_B_POINT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_B_POINT_POWER)), //

		DEBUG_V2_RPM_B_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_B_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_B_POINT_COS_PHI)), //

		DEBUG_V2_RPM_C_POINT_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_C_POINT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_C_POINT_POWER)), //

		DEBUG_V2_RPM_C_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_C_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_C_POINT_COS_PHI)), //

		DEBUG_V2_RPM_D_POINT_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_D_POINT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_D_POINT_POWER)), //

		DEBUG_V2_RPM_D_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_D_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_D_POINT_COS_PHI)), //

		DEBUG_V2_RPM_E_POINT_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_E_POINT_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_PN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_E_POINT_POWER)), //

		DEBUG_V2_RPM_E_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_E_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_E_POINT_COS_PHI)), //

		DEBUG_V2_RPM_COSPHIP_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_COSPHIP_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COSPHIP_TIME_CONSTANT)), //

		DEBUG_V2_RPM_COSPHIP_LOCK_IN_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_COSPHIP_LOCK_IN_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COSPHIP_LOCK_IN_VOLTAGE)), //

		DEBUG_V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE)), //

		DEBUG_V2_RPM_COSPHIP_EXTENDED_FUNCTIONS(Doc.of(EnableDisableOrUndefined.values())), //
		V2_RPM_COSPHIP_EXTENDED_FUNCTIONS(Doc.of(EnableDisableOrUndefined.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_COSPHIP_EXTENDED_FUNCTIONS)), //

		// --- Begin: Q(P) Curve ---
		DEBUG_V2_RPM_ENABLE_QP_CURVE(Doc.of(EnableCurve.values())), //
		V2_RPM_ENABLE_QP_CURVE(Doc.of(EnableCurve.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_ENABLE_QP_CURVE)), //

		DEBUG_V2_RPM_QP_CURVE_MODE(Doc.of(SafetyParameterEnums.Rpm.Mode.values())), //
		V2_RPM_QP_CURVE_MODE(Doc.of(SafetyParameterEnums.Rpm.Mode.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_CURVE_MODE)), //

		DEBUG_V2_RPM_QP_OVEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_OVEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.QMAX_PER_DECIPERCENT_PN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_OVEREXCITED_SLOPE)), //

		DEBUG_V2_RPM_QP_UNDEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_UNDEREXCITED_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.QMAX_PER_DECIPERCENT_PN)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_UNDEREXCITED_SLOPE)), //

		DEBUG_V2_RPM_QP_P1_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P1_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.text("Q(P) Curve P1 Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P1_POWER)), //

		DEBUG_V2_RPM_QP_P1_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P1_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.text("Q(P) Curve P1 Reactive Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P1_REACTIVE_POWER)), //

		DEBUG_V2_RPM_QP_P2_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P2_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.text("Q(P) Curve P2 Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P2_POWER)), //

		DEBUG_V2_RPM_QP_P2_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P2_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.text("Q(P) Curve P2 Reactive Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P2_REACTIVE_POWER)), //

		DEBUG_V2_RPM_QP_P3_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P3_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.text("Q(P) Curve P3 Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P3_POWER)), //

		DEBUG_V2_RPM_QP_P3_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P3_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.text("Q(P) Curve P3 Reactive Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P3_REACTIVE_POWER)), //

		DEBUG_V2_RPM_QP_P4_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P4_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.text("Q(P) Curve P4 Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P4_POWER)), //

		DEBUG_V2_RPM_QP_P4_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P4_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.text("Q(P) Curve P4 Reactive Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P4_REACTIVE_POWER)), //

		DEBUG_V2_RPM_QP_P5_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P5_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.text("Q(P) Curve P5 Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P5_POWER)), //

		DEBUG_V2_RPM_QP_P5_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P5_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.text("Q(P) Curve P5 Reactive Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P5_REACTIVE_POWER)), //

		DEBUG_V2_RPM_QP_P6_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P6_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.text("Q(P) Curve P6 Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P6_POWER)), //

		DEBUG_V2_RPM_QP_P6_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_P6_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.text("Q(P) Curve P6 Reactive Power")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_P6_REACTIVE_POWER)), //

		DEBUG_V2_RPM_QP_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)), //
		V2_RPM_QP_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Q(P) Curve Time Constant")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_RPM_QP_TIME_CONSTANT)), //

		// ###############################
		// Protection Parameters
		// ###############################

		// --- Voltage Protection Parameters (VPP) ---
		DEBUG_V2_VPP_OVER_VOLT_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_OVER_VOLT_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Overvoltage trigger first order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_1_VALUE)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overvoltage trigger first order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_UNDER_VOLT_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Undervoltage trigger first order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_1_VALUE)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Undervoltage trigger first order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_OVER_VOLT_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Overvoltage trigger second order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_2_VALUE)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overvoltage trigger second order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_UNDER_VOLT_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Undervoltage trigger second order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_2_VALUE)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Undervoltage trigger second order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_OVER_VOLT_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Overvoltage trigger third order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_3_VALUE)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overvoltage trigger third order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_UNDER_VOLT_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Undervoltage trigger third order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_3_VALUE)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Undervoltage trigger third order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_OVER_VOLT_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Overvoltage trigger forth order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_4_VALUE)), //

		DEBUG_V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overvoltage trigger forth order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_UNDER_VOLT_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Undervoltage trigger forth order value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_4_VALUE)), //

		DEBUG_V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Undervoltage trigger forth order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME)), //

		DEBUG_V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("10min overvoltage trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE)), //

		DEBUG_V2_VPP_TEN_MIN_STAGE_TRIP_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_VPP_TEN_MIN_STAGE_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("10min trigger trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_VPP_TEN_MIN_STAGE_TRIP_TIME)), //

		// --- Frequenzy Protection Parameters (FPP) ---

		DEBUG_V2_FPP_OVER_FREQ_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_OVER_FREQ_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Over-frequency stage one trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_1_VALUE)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overfrequence trigger first-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_UNDER_FREQ_STAGE_1_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Under-frequency stage one trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_1_VALUE)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Underfrequence trigger first-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_OVER_FREQ_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Over-frequency stage two trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_2_VALUE)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overfrequence trigger second-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_UNDER_FREQ_STAGE_2_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Under-frequency stage two trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_2_VALUE)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Underfrequence trigger second-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_OVER_FREQ_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Over-frequency stage three trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_3_VALUE)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overfrequence trigger third-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_UNDER_FREQ_STAGE_3_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Under-frequency stage three trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_3_VALUE)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Underfrequence trigger third-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_OVER_FREQ_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Over-frequency stage four trigger value")//
				.accessMode(AccessMode.READ_WRITE)
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_4_VALUE)), //

		DEBUG_V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Overfrequence trigger forth-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)), //
		V2_FPP_UNDER_FREQ_STAGE_4_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Under-frequency stage four trigger value")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_4_VALUE)), //

		DEBUG_V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)), //
		V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Underfrequence trigger forth-order trip time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME)), //

		// ################################################
		// Connection Parameters
		// ################################################

		// --- Begin: Ramp Up ---
		DEBUG_V2_CP_RAMP_UP_UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RAMP_UP_UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Ramp Up Upper Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RAMP_UP_UPPER_VOLTAGE)), //

		DEBUG_V2_CP_RAMP_UP_LOWER_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.MEDIUM)), //
		V2_CP_RAMP_UP_LOWER_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Ramp Up Lower Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RAMP_UP_LOWER_VOLTAGE)), //

		DEBUG_V2_CP_RAMP_UP_UPPER_FREQUENCY(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RAMP_UP_UPPER_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Ramp Up Upper Frequency")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RAMP_UP_UPPER_FREQUENCY)), //

		DEBUG_V2_CP_RAMP_UP_LOWER_FREQUENCY(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RAMP_UP_LOWER_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Ramp Up Lower Frequency")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RAMP_UP_LOWER_FREQUENCY)), //

		DEBUG_V2_CP_RAMP_UP_OBSERVATION_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RAMP_UP_OBSERVATION_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Ramp Up Observation Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RAMP_UP_OBSERVATION_TIME)), //

		DEBUG_V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE(Doc.of(OpenemsType.BOOLEAN)), //
		V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Soft Ramp Up Gradient Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE)), //

		DEBUG_V2_CP_SOFT_RAMP_UP_GRADIENT(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_SOFT_RAMP_UP_GRADIENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Soft Ramp Up Gradient")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_SOFT_RAMP_UP_GRADIENT)), //

		// --- Begin: Reconnection ---
		DEBUG_V2_CP_RECONNECTION_UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RECONNECTION_UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Reconnection Upper Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_UPPER_VOLTAGE)), //

		DEBUG_V2_CP_RECONNECTION_LOWER_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RECONNECTION_LOWER_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Reconnection Lower Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_LOWER_VOLTAGE)), //

		DEBUG_V2_CP_RECONNECTION_UPPER_FREQUENCY(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RECONNECTION_UPPER_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Reconnection Upper Frequency")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_UPPER_FREQUENCY)), //

		DEBUG_V2_CP_RECONNECTION_LOWER_FREQUENCY(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RECONNECTION_LOWER_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Reconnection Lower Frequency")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_LOWER_FREQUENCY)), //

		DEBUG_V2_CP_RECONNECTION_OBSERVATION_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RECONNECTION_OBSERVATION_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Reconnection Observation Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_OBSERVATION_TIME)), //

		DEBUG_V2_CP_RECONNECTION_GRADIENT_ENABLE(Doc.of(EnableDisableOrUndefined.values())), //
		V2_CP_RECONNECTION_GRADIENT_ENABLE(Doc.of(EnableDisableOrUndefined.values())//
				.text("Reconnection Gradient Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_GRADIENT_ENABLE)), //

		DEBUG_V2_CP_RECONNECTION_GRADIENT(Doc.of(OpenemsType.INTEGER)), //
		V2_CP_RECONNECTION_GRADIENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.text("Reconnection Gradient")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_CP_RECONNECTION_GRADIENT)), //

		// --- Begin: Voltage Ride Through (LVRT/HVRT) and Zero Current ---
		DEBUG_V2_LVRT_ENABLE(Doc.of(EnableDisableOrUndefined.values())), //
		V2_LVRT_ENABLE(Doc.of(EnableDisableOrUndefined.values())//
				.text("LVRT Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_ENABLE)), //

		DEBUG_V2_LVRT_UV1_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV1_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV1 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV1_VOLTAGE)), //

		DEBUG_V2_LVRT_UV1_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)// GoodWe storing in 10ms steps
				.text("LVRT UV1 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV1_TIME)), //

		DEBUG_V2_LVRT_UV2_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV2_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV2 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV2_VOLTAGE)), //

		DEBUG_V2_LVRT_UV2_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("LVRT UV2 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV2_TIME)), //

		DEBUG_V2_LVRT_UV3_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV3_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV3 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV3_VOLTAGE)), //

		DEBUG_V2_LVRT_UV3_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("LVRT UV3 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV3_TIME)), //

		DEBUG_V2_LVRT_UV4_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV4_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV4 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV4_VOLTAGE)), //

		DEBUG_V2_LVRT_UV4_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV4_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("LVRT UV4 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV4_TIME)), //

		DEBUG_V2_LVRT_UV5_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV5_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV5 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV5_VOLTAGE)), //

		DEBUG_V2_LVRT_UV5_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV5_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("LVRT UV5 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV5_TIME)), //

		DEBUG_V2_LVRT_UV6_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV6_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV6 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV6_VOLTAGE)), //

		DEBUG_V2_LVRT_UV6_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV6_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("LVRT UV6 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV6_TIME)), //

		DEBUG_V2_LVRT_UV7_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV7_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT UV7 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV7_VOLTAGE)), //

		DEBUG_V2_LVRT_UV7_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_UV7_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("LVRT UV7 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_UV7_TIME)), //

		DEBUG_V2_LVRT_ENTER_THRESHOLD(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_ENTER_THRESHOLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT Enter Threshold")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_ENTER_THRESHOLD)), //

		DEBUG_V2_LVRT_EXIT_ENDPOINT(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_EXIT_ENDPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("LVRT Exit Endpoint")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_EXIT_ENDPOINT)), //

		DEBUG_V2_LVRT_K1_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_K1_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.text("LVRT K1 Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_K1_SLOPE)), //

		DEBUG_V2_LVRT_ZERO_CURRENT_MODE_ENABLE(Doc.of(EnableDisableOrUndefined.values())), //
		V2_LVRT_ZERO_CURRENT_MODE_ENABLE(Doc.of(EnableDisableOrUndefined.values())//
				.text("Zero Current Mode Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_ZERO_CURRENT_MODE_ENABLE)), //

		DEBUG_V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD(Doc.of(OpenemsType.INTEGER)), //
		V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Zero Current Mode Entry Threshold")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD)), //

		DEBUG_V2_HVRT_ENABLE(Doc.of(EnableDisableOrUndefined.values())), //
		V2_HVRT_ENABLE(Doc.of(EnableDisableOrUndefined.values())//
				.text("HVRT Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_ENABLE)), //

		DEBUG_V2_HVRT_OV1_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV1_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV1 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV1_VOLTAGE)), //

		DEBUG_V2_HVRT_OV1_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV1 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV1_TIME)), //

		DEBUG_V2_HVRT_OV2_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV2_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV2 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV2_VOLTAGE)), //

		DEBUG_V2_HVRT_OV2_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV2 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV2_TIME)), //

		DEBUG_V2_HVRT_OV3_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV3_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV3 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV3_VOLTAGE)), //

		DEBUG_V2_HVRT_OV3_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV3 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV3_TIME)), //

		DEBUG_V2_HVRT_OV4_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV4_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV4 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV4_VOLTAGE)), //

		DEBUG_V2_HVRT_OV4_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV4_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV4 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV4_TIME)), //

		DEBUG_V2_HVRT_OV5_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV5_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV5 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV5_VOLTAGE)), //

		DEBUG_V2_HVRT_OV5_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV5_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV5 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV5_TIME)), //

		DEBUG_V2_HVRT_OV6_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV6_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV6 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV6_VOLTAGE)), //

		DEBUG_V2_HVRT_OV6_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV6_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV6 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV6_TIME)), //

		DEBUG_V2_HVRT_OV7_VOLTAGE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV7_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT OV7 Voltage")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV7_VOLTAGE)), //

		DEBUG_V2_HVRT_OV7_TIME(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_OV7_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("HVRT OV7 Time")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_OV7_TIME)), //

		DEBUG_V2_HVRT_ENTER_HIGH_CROSSING(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_ENTER_HIGH_CROSSING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT Enter High Crossing Threshold")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_ENTER_HIGH_CROSSING)), //

		DEBUG_V2_HVRT_EXIT_HIGH_CROSSING(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_EXIT_HIGH_CROSSING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("HVRT Exit High Crossing Threshold")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_EXIT_HIGH_CROSSING)), //

		DEBUG_V2_HVRT_K2_SLOPE(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_K2_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.text("HVRT K2 Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_K2_SLOPE)), //

		DEBUG_V2_HVRT_ZERO_CURRENT_MODE_ENABLE(Doc.of(EnableDisableOrUndefined.values())), //
		V2_HVRT_ZERO_CURRENT_MODE_ENABLE(Doc.of(EnableDisableOrUndefined.values())//
				.text("Zero Current Mode Enable")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_ZERO_CURRENT_MODE_ENABLE)), //

		DEBUG_V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD(Doc.of(OpenemsType.INTEGER)), //
		V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_VN)//
				.text("Zero Current Mode Entry Threshold")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD)), //

		DEBUG_VRT_CURRENT_DISTRIBUTION_MODE(Doc.of(SafetyParameterEnums.Vrt.CurrentDistributionMode.values())), //
		V2_VRT_CURRENT_DISTRIBUTION_MODE(Doc.of(SafetyParameterEnums.Vrt.CurrentDistributionMode.values())//
				.text("Current Distribution Mode")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_CURRENT_DISTRIBUTION_MODE)), //

		DEBUG_VRT_ACTIVE_POWER_RECOVERY_MODE(Doc.of(SafetyParameterEnums.Vrt.GeneralRecoveryMode.values())), //
		V2_VRT_ACTIVE_POWER_RECOVERY_MODE(Doc.of(SafetyParameterEnums.Vrt.GeneralRecoveryMode.values())//
				.text("Active Power Recovery Mode After Crossing")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_ACTIVE_POWER_RECOVERY_MODE)), //

		DEBUG_VRT_ACTIVE_POWER_RECOVERY_SPEED(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.MEDIUM)),
		V2_VRT_ACTIVE_POWER_RECOVERY_SPEED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_LN_PER_SECOND)//
				.text("Ride Through End Active Power Recover Speed")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_ACTIVE_POWER_RECOVERY_SPEED)),

		DEBUG_VRT_ACTIVE_POWER_RECOVERY_SLOPE(Doc.of(OpenemsType.LONG)), //
		V2_VRT_ACTIVE_POWER_RECOVERY_SLOPE(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Active Power Recovery Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_ACTIVE_POWER_RECOVERY_SLOPE)),

		DEBUG_VRT_REACTIVE_POWER_RECOVERY_MODE_END(Doc.of(SafetyParameterEnums.Vrt.GeneralRecoveryMode.values())), //
		V2_VRT_REACTIVE_POWER_RECOVERY_MODE_END(Doc.of(SafetyParameterEnums.Vrt.GeneralRecoveryMode.values())//
				.text("Traversing End of Reactive Power Recovery Mode")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_REACTIVE_POWER_RECOVERY_MODE_END)), //

		DEBUG_VRT_REACTIVE_POWER_RECOVERY_SPEED(Doc.of(OpenemsType.INTEGER)), //
		V2_VRT_REACTIVE_POWER_RECOVERY_SPEED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PROMILLE_LN_PER_SECOND)//
				.text("Ride Through End Active Power Recover Speed")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_REACTIVE_POWER_RECOVERY_SPEED)),

		DEBUG_VRT_REACTIVE_POWER_RECOVERY_SLOPE(Doc.of(OpenemsType.LONG)), //
		V2_VRT_REACTIVE_POWER_RECOVERY_SLOPE(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)//
				.text("Active Power Recovery Slope")//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_VRT_REACTIVE_POWER_RECOVERY_SLOPE)),

		// --- Begin: Frequency Ride Through ---
		V2_FRT_ENABLE(Doc.of(EnableDisableOrUndefined.values())//
				.text("Frequency Ride Through Enable")//
				.accessMode(AccessMode.READ_WRITE)), //

		V2_FRT_UF1_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Frequency Ride Through UF1 Frequency")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_UF1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Frequency Ride Through UF1 Time")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_UF2_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Frequency Ride Through UF2 Frequency")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_UF2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Frequency Ride Through UF2 Time")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_UF3_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Frequency Ride Through UF3 Frequency")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_UF3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Frequency Ride Through UF3 Time")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_OF1_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Frequency Ride Through OF1 Frequency")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_OF1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Frequency Ride Through OF1 Time")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_OF2_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Frequency Ride Through OF2 Frequency")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_OF2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Frequency Ride Through OF2 Time")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_OF3_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIHERTZ)//
				.text("Frequency Ride Through OF3 Frequency")//
				.accessMode(AccessMode.READ_WRITE)), //
		V2_FRT_OF3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.text("Frequency Ride Through OF3 Time")//
				.accessMode(AccessMode.READ_WRITE)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
