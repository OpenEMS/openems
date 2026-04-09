package io.openems.edge.goodwe.batteryinverter;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.FunctionUtils;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.GoodWePowerSetting;
import io.openems.edge.goodwe.common.enums.EnableCurve;

public class PowerSettingHandler {

	private final GoodWeBatteryInverterImpl parent;

	private final Logger log = LoggerFactory.getLogger(PowerSettingHandler.class);

	public PowerSettingHandler(GoodWeBatteryInverterImpl parent) {
		this.parent = parent;
	}

	/**
	 * Handles the Power Setting and sets the value of the channels of the
	 * GoodWeBatteryInverter.
	 * 
	 * @param config the config
	 */
	public void handlePowerSetting(Config config) {

		try {

			// ###############################
			// Active Power Settings
			// ###############################

			// --- General Settings ---
			if (isConfigured(config.settingApmOutputActivePower())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_GENERAL_OUTPUT_ACTIVE_POWER),
						config.settingApmOutputActivePower());
			}
			if (isConfigured(config.settingApmPowerGradient())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_GENERAL_POWER_GRADIENT),
						config.settingApmPowerGradient());
			}

			// --- PF Curve ---
			if (isConfigured(config.settingApmPfOverFrequencyCurveEnable())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PF_OVERFREQUENZY_CURVE),
						config.settingApmPfOverFrequencyCurveEnable()); //
			}
			if (isConfigured(config.settingApmPfOverFrequencyStartPoint())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_START),
						config.settingApmPfOverFrequencyStartPoint());
			}
			if (isConfigured(config.settingApmPfOverFrequencySlope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_SLOPE),
						config.settingApmPfOverFrequencySlope());
			}
			if (isConfigured(config.settingApmPfOverFrequencyDelayTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_TIME),
						config.settingApmPfOverFrequencyDelayTime());
			}
			if (isConfigured(config.settingApmPfOverFrequencyDeactivationThresholdFstop())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE),
						config.settingApmPfOverFrequencyDeactivationThresholdFstop().booleanValue);
			}
			if (isConfigured(config.settingApmPfOverFrequencyHysteresisPoint())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT),
						config.settingApmPfOverFrequencyHysteresisPoint());
			}
			if (isConfigured(config.settingApmPOverFrequencyDelayWaitingTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME),
						config.settingApmPOverFrequencyDelayWaitingTime());
			}
			if (isConfigured(config.settingApmPfOverFrequencyHysteresisSlope())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE),
						config.settingApmPfOverFrequencyHysteresisSlope());
			}
			if (isConfigured(config.settingApmPfUnderFrequencyCurveEnable())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PF_UNDERFREQUENZY_CURVE),
						config.settingApmPfUnderFrequencyCurveEnable());
			}
			if (isConfigured(config.settingApmPfUnderFrequencyThreshold())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_THRESHOLD),
						config.settingApmPfUnderFrequencyThreshold());
			}
			if (isConfigured(config.settingApmPfUnderFrequencySlope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_SLOPE),
						config.settingApmPfUnderFrequencySlope());
			}
			if (isConfigured(config.settingApmPfUnderFrequencyDelayTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_TIME),
						config.settingApmPfUnderFrequencyDelayTime());
			}
			if (isConfigured(config.settingApmPfUnderFrequencyDeactivationThresholdFstop())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE),
						config.settingApmPfUnderFrequencyDeactivationThresholdFstop().booleanValue);
			}
			if (isConfigured(config.settingApmPfUnderFrequencyHysteresisPoint())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT),
						config.settingApmPfUnderFrequencyHysteresisPoint());
			}
			if (isConfigured(config.settingApmPUnderFrequencyDelayWaitingTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME),
						config.settingApmPUnderFrequencyDelayWaitingTime());
			}
			if (isConfigured(config.settingApmPfUnderFrequencyHysteresisSlope())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE),
						config.settingApmPfUnderFrequencyHysteresisSlope());
			}

			// --- PU Curve ---
			if (isConfigured(config.puCurveEnable())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PU_CURVE),
						config.puCurveEnable());
			}
			if (isConfigured(config.settingApmPuV1Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VOLTAGE),
						config.settingApmPuV1Voltage());
			}
			if (isConfigured(config.settingApmPuV1ActivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VALUE),
						config.settingApmPuV1ActivePower());
			}
			if (isConfigured(config.settingApmPuV2Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VOLTAGE),
						config.settingApmPuV2Voltage());
			}
			if (isConfigured(config.settingApmPuV2ActivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VALUE),
						config.settingApmPuV2ActivePower());
			}
			if (isConfigured(config.settingApmPuV3Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VOLTAGE),
						config.settingApmPuV3Voltage());
			}
			if (isConfigured(config.settingApmPuV3ActivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VALUE),
						config.settingApmPuV3ActivePower());
			}
			if (isConfigured(config.settingApmPuV4Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VOLTAGE),
						config.settingApmPuV4Voltage());
			}
			if (isConfigured(config.settingApmPuV4ActivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VALUE),
						config.settingApmPuV4ActivePower());
			}
			if (isConfigured(config.settingApmPuPt1LowPassFilterTimeConstantPt1Mode())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE),
						config.settingApmPuPt1LowPassFilterTimeConstantPt1Mode());
			}
			if (isConfigured(config.settingApmPuPt1LowPassFilterTimeConstantGradientMode())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE),
						config.settingApmPuPt1LowPassFilterTimeConstantGradientMode());
			}
			if (isConfigured(config.settingApmPuResponseMode())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_APM_PU_OUTPUT_RESPONSE_MODE),
						config.settingApmPuResponseMode());
			}

			// ###############################
			// Reactive Power Settings
			// ###############################

			// --- Mode Selection ---
			EnableCurve enaRpmFixPf = EnableCurve.DISABLE;
			EnableCurve enaRpmFixQ = EnableCurve.DISABLE;
			EnableCurve enaRpmQuCurve = EnableCurve.DISABLE;
			EnableCurve enaRpmCosPhi = EnableCurve.DISABLE;
			EnableCurve enaRpmQpCurve = EnableCurve.DISABLE;

			switch (config.settingRpmMode()) {
			case FIX_PF -> enaRpmFixPf = EnableCurve.ENABLE;
			case FIX_Q -> enaRpmFixQ = EnableCurve.ENABLE;
			case QU_CURVE -> enaRpmQuCurve = EnableCurve.ENABLE;
			case COS_PHI_P_CURVE -> enaRpmCosPhi = EnableCurve.ENABLE;
			case QP_CURVE -> enaRpmQpCurve = EnableCurve.ENABLE;
			case UNSELECTED -> FunctionUtils.doNothing();
			}

			setWriteValueIfNotRead(this.parent.channel(GoodWe.ChannelId.ENABLE_FIXED_POWER_FACTOR_V2), enaRpmFixPf);
			setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_FIXED_Q), enaRpmFixQ);
			setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_ENABLE_QU_CURVE),
					enaRpmQuCurve);
			setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_CURVE_COS_PHI_P),
					enaRpmCosPhi);
			setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_QP_CURVE),
					enaRpmQpCurve);

			// --- Reactive Power Mode: Fix P(F) ---
			if (isConfigured(config.settingRpmFixPf())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWe.ChannelId.FIXED_POWER_FACTOR_V2),
						config.settingRpmFixPf());
			}

			// --- Reactive Power Mode: Fix Q ---
			if (isConfigured(config.settingRpmFixQ())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_FIXED_Q_VALUE),
						config.settingRpmFixQ());
			}

			// --- Q(U) Curve ---
			if (isConfigured(config.settingRpmQuCurveMode())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_CURVE_MODE),
						config.settingRpmQuCurveMode());
			}

			if (isConfigured(config.settingRpmQuVoltageDeadBand())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_VOLTAGE_DEAD_BAND),
						config.settingRpmQuVoltageDeadBand());
			}

			if (isConfigured(config.settingRpmQuOverexcitedSlope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_OVEREXCITED_SLOPE),
						config.settingRpmQuOverexcitedSlope());
			}

			if (isConfigured(config.settingRpmQuUnderexcitedSlope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_UNDEREXCITED_SLOPE),
						config.settingRpmQuUnderexcitedSlope());
			}
			if (isConfigured(config.settingRpmQuV1Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VOLTAGE),
						config.settingRpmQuV1Voltage());
			}
			if (isConfigured(config.settingRpmQuV1ReactivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VALUE),
						config.settingRpmQuV1ReactivePower());
			}
			if (isConfigured(config.settingRpmQuV2Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VOLTAGE),
						config.settingRpmQuV2Voltage());
			}
			if (isConfigured(config.settingRpmQuV2ReactivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VALUE),
						config.settingRpmQuV2ReactivePower());
			}
			if (isConfigured(config.settingRpmQuV3Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VOLTAGE),
						config.settingRpmQuV3Voltage());
			}
			if (isConfigured(config.settingRpmQuV3ReactivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VALUE),
						config.settingRpmQuV3ReactivePower());
			}
			if (isConfigured(config.settingRpmQuV4Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VOLTAGE),
						config.settingRpmQuV4Voltage());
			}
			if (isConfigured(config.settingRpmQuV4ReactivePower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VALUE),
						config.settingRpmQuV4ReactivePower());
			}
			if (isConfigured(config.settingRpmQuTimeConstant())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_TIME_CONSTANT),
						config.settingRpmQuTimeConstant());
			}
			if (isConfigured(config.settingRpmQuExtendedFunctions())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_EXTENDED_FUNCTIONS),
						config.settingRpmQuExtendedFunctions());
			}
			if (isConfigured(config.settingRpmQuLockInPower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_IN_POWER),
						config.settingRpmQuLockInPower());
			}
			if (isConfigured(config.settingRpmQuLockOutPower())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_OUT_POWER),
						config.settingRpmQuLockOutPower());
			}

			// --- CosPhi(P) Curve ---
			if (isConfigured(config.settingRpmCosPhiPCurveMode())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COS_PHI_P_CURVE_MODE),
						config.settingRpmCosPhiPCurveMode());
			}
			if (isConfigured(config.settingRpmCosPhiPOverexcitedSlope())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COS_PHI_P_OVEREXCITED_SLOPE),
						config.settingRpmCosPhiPOverexcitedSlope());
			}
			if (isConfigured(config.settingRpmCosPhiPUnderexcitedSlope())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COS_PHI_P_UNDEREXCITED_SLOPE),
						config.settingRpmCosPhiPUnderexcitedSlope());
			}
			if (isConfigured(config.settingRpmCosPhipPowerA())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_POWER),
						config.settingRpmCosPhipPowerA());
			}
			if (isConfigured(config.settingRpmCosPhipCosPhiA())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_COS_PHI),
						config.settingRpmCosPhipCosPhiA());
			}
			if (isConfigured(config.settingRpmCosPhipPowerB())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_POWER),
						config.settingRpmCosPhipPowerB());
			}
			if (isConfigured(config.settingRpmCosPhipCosPhiB())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_COS_PHI),
						config.settingRpmCosPhipCosPhiB());
			}
			if (isConfigured(config.settingRpmCosPhipPowerC())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_POWER),
						config.settingRpmCosPhipPowerC());
			}
			if (isConfigured(config.settingRpmCosPhipCosPhiC())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_COS_PHI),
						config.settingRpmCosPhipCosPhiC());
			}
			if (isConfigured(config.settingRpmCosPhipPowerD())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_POWER),
						config.settingRpmCosPhipPowerD());
			}
			if (isConfigured(config.settingRpmCosPhipCosPhiD())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_COS_PHI),
						config.settingRpmCosPhipCosPhiD());
			}
			if (isConfigured(config.settingRpmCosPhipPowerE())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_POWER),
						config.settingRpmCosPhipPowerE());
			}
			if (isConfigured(config.settingRpmCosPhipCosPhiE())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_COS_PHI),
						config.settingRpmCosPhipCosPhiE());
			}
			if (isConfigured(config.settingRpmCosPhipTimeConstant())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_TIME_CONSTANT),
						config.settingRpmCosPhipTimeConstant());
			}
			if (isConfigured(config.settingRpmCosPhipLockInVolt())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_IN_VOLTAGE),
						config.settingRpmCosPhipLockInVolt());
			}
			if (isConfigured(config.settingRpmCosPhipLockOutVolt())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE),
						config.settingRpmCosPhipLockOutVolt());
			}
			if (isConfigured(config.settingRpmCosPhipExtendedFunctions())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_EXTENDED_FUNCTIONS),
						config.settingRpmCosPhipExtendedFunctions());
			}

			// --- Q(P) Curve ---
			if (isConfigured(config.settingQpCurveMode())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_CURVE_MODE),
						config.settingQpCurveMode());
			}
			if (isConfigured(config.settingQpOverexcitedSlope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_OVEREXCITED_SLOPE),
						config.settingQpOverexcitedSlope());
			}
			if (isConfigured(config.settingQpUnderexcitedSlope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_UNDEREXCITED_SLOPE),
						config.settingQpUnderexcitedSlope());
			}
			if (isConfigured(config.settingRpmQpPowerP1())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_POWER),
						config.settingRpmQpPowerP1());
			}
			if (isConfigured(config.settingRpmQpReactivePowerP1())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_REACTIVE_POWER),
						config.settingRpmQpReactivePowerP1());
			}
			if (isConfigured(config.settingRpmQpPowerP2())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_POWER),
						config.settingRpmQpPowerP2());
			}
			if (isConfigured(config.settingRpmQpReactivePowerP2())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_REACTIVE_POWER),
						config.settingRpmQpReactivePowerP2());
			}
			if (isConfigured(config.settingRpmQpPowerP3())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_POWER),
						config.settingRpmQpPowerP3());
			}
			if (isConfigured(config.settingRpmQpReactivePowerP3())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_REACTIVE_POWER),
						config.settingRpmQpReactivePowerP3());
			}
			if (isConfigured(config.settingRpmQpPowerP4())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_POWER),
						config.settingRpmQpPowerP4());
			}
			if (isConfigured(config.settingRpmQpReactivePowerP4())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_REACTIVE_POWER),
						config.settingRpmQpReactivePowerP4());
			}
			if (isConfigured(config.settingRpmQpPowerP5())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_POWER),
						config.settingRpmQpPowerP5());
			}
			if (isConfigured(config.settingRpmQpReactivePowerP5())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_REACTIVE_POWER),
						config.settingRpmQpReactivePowerP5());
			}
			if (isConfigured(config.settingRpmQpPowerP6())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_POWER),
						config.settingRpmQpPowerP6());
			}
			if (isConfigured(config.settingRpmQpReactivePowerP6())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_REACTIVE_POWER),
						config.settingRpmQpReactivePowerP6());
			}
			if (isConfigured(config.settingRpmQpTimeConstant())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_RPM_QP_TIME_CONSTANT),
						config.settingRpmQpTimeConstant());
			}

			// --- Voltage Protection Parameters ---
			// Over-voltage/Under-voltage stages (mapping as per manufacturer list)
			if (isConfigured(config.settingVppOvStage1TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_VALUE),
						config.settingVppOvStage1TriggerValue());
			}
			if (isConfigured(config.settingVppOvStage1TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME),
						config.settingVppOvStage1TripTime());
			}
			if (isConfigured(config.settingVppUvStage1TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_VALUE),
						config.settingVppUvStage1TripValue());
			}
			if (isConfigured(config.settingVppUvStage1TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME),
						config.settingVppUvStage1TripTime());
			}
			if (isConfigured(config.settingVppOvStage2TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_VALUE),
						config.settingVppOvStage2TriggerValue());
			}
			if (isConfigured(config.settingVppOvStage2TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME),
						config.settingVppOvStage2TripTime());
			}
			if (isConfigured(config.settingVppUvStage2TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_VALUE),
						config.settingVppUvStage2TripValue());
			}
			if (isConfigured(config.settingVppUvStage2TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME),
						config.settingVppUvStage2TripTime());
			}
			if (isConfigured(config.settingVppOvStage3TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_VALUE),
						config.settingVppOvStage3TriggerValue());
			}
			if (isConfigured(config.settingVppOvStage3TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME),
						config.settingVppOvStage3TripTime());
			}
			if (isConfigured(config.settingVppUvStage3TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_VALUE),
						config.settingVppUvStage3TripValue());
			}
			if (isConfigured(config.settingVppUvStage3TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME),
						config.settingVppUvStage3TripTime());
			}
			if (isConfigured(config.settingVppOvStage4TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_VALUE),
						config.settingVppOvStage4TriggerValue());
			}
			if (isConfigured(config.settingVppOvStage4TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME),
						config.settingVppOvStage4TripTime());
			}
			if (isConfigured(config.settingVppUvStage4TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_VALUE),
						config.settingVppUvStage4TripValue());
			}
			if (isConfigured(config.settingVppUvStage4TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME),
						config.settingVppUvStage4TripTime());
			}
			if (isConfigured(config.settingVpp10MinOvTripThreshold())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE),
						config.settingVpp10MinOvTripThreshold());
			}
			if (isConfigured(config.settingVpp10MinOvTripTime())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_STAGE_TRIP_TIME),
						config.settingVpp10MinOvTripTime());
			}

			// --- Frequency Protection Parameters ---
			if (isConfigured(config.settingFppOfStage1TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_VALUE),
						config.settingFppOfStage1TriggerValue());
			}
			if (isConfigured(config.settingFppOfStage1TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME),
						config.settingFppOfStage1TripTime());
			}
			if (isConfigured(config.settingFppUfStage1TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_VALUE),
						config.settingFppUfStage1TripValue());
			}
			if (isConfigured(config.settingFppUfStage1TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME),
						config.settingFppUfStage1TripTime());
			}
			if (isConfigured(config.settingFppOfStage2TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_VALUE),
						config.settingFppOfStage2TriggerValue());
			}
			if (isConfigured(config.settingFppOfStage2TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME),
						config.settingFppOfStage2TripTime());
			}
			if (isConfigured(config.settingFppUfStage2TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_VALUE),
						config.settingFppUfStage2TripValue());
			}
			if (isConfigured(config.settingFppUfStage2TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME),
						config.settingFppUfStage2TripTime());
			}
			if (isConfigured(config.settingFppOfStage3TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_VALUE),
						config.settingFppOfStage3TriggerValue());
			}
			if (isConfigured(config.settingFppOfStage3TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME),
						config.settingFppOfStage3TripTime());
			}
			if (isConfigured(config.settingFppUfStage3TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_VALUE),
						config.settingFppUfStage3TripValue());
			}
			if (isConfigured(config.settingFppUfStage3TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME),
						config.settingFppUfStage3TripTime());
			}
			if (isConfigured(config.settingFppOfStage4TriggerValue())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_VALUE),
						config.settingFppOfStage4TriggerValue());
			}
			if (isConfigured(config.settingFppOfStage4TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME),
						config.settingFppOfStage4TripTime());
			}
			if (isConfigured(config.settingFppUfStage4TripValue())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_VALUE),
						config.settingFppUfStage4TripValue());
			}
			if (isConfigured(config.settingFppUfStage4TripTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME),
						config.settingFppUfStage4TripTime());
			}

			// --- Connection Parameters: Ramp Up ---
			if (isConfigured(config.settingCpRampUpUpperVoltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_VOLTAGE),
						config.settingCpRampUpUpperVoltage());
			}
			if (isConfigured(config.settingCpRampUpLowerVoltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_VOLTAGE),
						config.settingCpRampUpLowerVoltage());
			}
			if (isConfigured(config.settingCpRampUpUpperFrequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_FREQUENCY),
						config.settingCpRampUpUpperFrequency());
			}
			if (isConfigured(config.settingCpRampUpLowerFrequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_FREQUENCY),
						config.settingCpRampUpLowerFrequency());
			}
			if (isConfigured(config.settingCpRampUpObservationTime())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_OBSERVATION_TIME),
						config.settingCpRampUpObservationTime());
			}
			if (isConfigured(config.settingCpSoftRampUpGradientEnable())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE),
						config.settingCpSoftRampUpGradientEnable().booleanValue);
			}
			if (isConfigured(config.settingCpSoftRampUpGradient())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT),
						config.settingCpSoftRampUpGradient());
			}

			// --- Connection Parameters: Reconnection ---
			if (isConfigured(config.settingCpReconnectionUpperVoltage())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_VOLTAGE),
						config.settingCpReconnectionUpperVoltage());
			}
			if (isConfigured(config.settingCpReconnectionLowerVoltage())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_VOLTAGE),
						config.settingCpReconnectionLowerVoltage());
			}
			if (isConfigured(config.settingCpReconnectionUpperFrequency())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_FREQUENCY),
						config.settingCpReconnectionUpperFrequency());
			}
			if (isConfigured(config.settingCpReconnectionLowerFrequency())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_FREQUENCY),
						config.settingCpReconnectionLowerFrequency());
			}
			if (isConfigured(config.settingCpReconnectionObservationTime())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_OBSERVATION_TIME),
						config.settingCpReconnectionObservationTime());
			}
			if (isConfigured(config.settingCpReconnectionGradientEnable())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT_ENABLE),
						config.settingCpReconnectionGradientEnable());
			}
			if (isConfigured(config.settingCpReconnectionGradient())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT),
						config.settingCpReconnectionGradient());
			}

			// --- Voltage Ride Through Parameters ---
			// LVRT
			if (isConfigured(config.settingLvrtEnable())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_ENABLE),
						config.settingLvrtEnable());
			}
			if (isConfigured(config.settingLvrtUv1Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_VOLTAGE),
						config.settingLvrtUv1Voltage());
			}
			if (isConfigured(config.settingLvrtUv1Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_TIME),
						config.settingLvrtUv1Time());
			}
			if (isConfigured(config.settingLvrtUv2Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_VOLTAGE),
						config.settingLvrtUv2Voltage());
			}
			if (isConfigured(config.settingLvrtUv2Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_TIME),
						config.settingLvrtUv2Time());
			}
			if (isConfigured(config.settingLvrtUv3Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_VOLTAGE),
						config.settingLvrtUv3Voltage());
			}
			if (isConfigured(config.settingLvrtUv3Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_TIME),
						config.settingLvrtUv3Time());
			}
			if (isConfigured(config.settingLvrtUv4Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_VOLTAGE),
						config.settingLvrtUv4Voltage());
			}
			if (isConfigured(config.settingLvrtUv4Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_TIME),
						config.settingLvrtUv4Time());
			}
			if (isConfigured(config.settingLvrtUv5Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_VOLTAGE),
						config.settingLvrtUv5Voltage());
			}
			if (isConfigured(config.settingLvrtUv5Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_TIME),
						config.settingLvrtUv5Time());
			}
			if (isConfigured(config.settingLvrtUv6Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_VOLTAGE),
						config.settingLvrtUv6Voltage());
			}
			if (isConfigured(config.settingLvrtUv6Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_TIME),
						config.settingLvrtUv6Time());
			}
			if (isConfigured(config.settingLvrtUv7Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_VOLTAGE),
						config.settingLvrtUv7Voltage());
			}
			if (isConfigured(config.settingLvrtUv7Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_TIME),
						config.settingLvrtUv7Time());
			}
			if (isConfigured(config.settingLvrtEnterThreshold())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_ENTER_THRESHOLD),
						config.settingLvrtEnterThreshold());
			}
			if (isConfigured(config.settingLvrtExitEndpoint())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_EXIT_ENDPOINT),
						config.settingLvrtExitEndpoint());
			}
			if (isConfigured(config.settingLvrtK1Slope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_K1_SLOPE),
						config.settingLvrtK1Slope());
			}
			if (isConfigured(config.settingLvrtZeroCurrentModeEnable())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENABLE),
						config.settingLvrtZeroCurrentModeEnable());
			}
			if (isConfigured(config.settingLvrtZeroCurrentModeEntryThreshold())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD),
						config.settingLvrtZeroCurrentModeEntryThreshold());
			}

			// HVRT
			if (isConfigured(config.settingHvrtEnable())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_ENABLE),
						config.settingHvrtEnable());
			}
			if (isConfigured(config.settingHvrtOv1Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_VOLTAGE),
						config.settingHvrtOv1Voltage());
			}
			if (isConfigured(config.settingHvrtOv1Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_TIME),
						config.settingHvrtOv1Time());
			}
			if (isConfigured(config.settingHvrtOv2Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_VOLTAGE),
						config.settingHvrtOv2Voltage());
			}
			if (isConfigured(config.settingHvrtOv2Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_TIME),
						config.settingHvrtOv2Time());
			}
			if (isConfigured(config.settingHvrtOv3Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_VOLTAGE),
						config.settingHvrtOv3Voltage());
			}
			if (isConfigured(config.settingHvrtOv3Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_TIME),
						config.settingHvrtOv3Time());
			}
			if (isConfigured(config.settingHvrtOv4Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_VOLTAGE),
						config.settingHvrtOv4Voltage());
			}
			if (isConfigured(config.settingHvrtOv4Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_TIME),
						config.settingHvrtOv4Time());
			}
			if (isConfigured(config.settingHvrtOv5Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_VOLTAGE),
						config.settingHvrtOv5Voltage());
			}
			if (isConfigured(config.settingHvrtOv5Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_TIME),
						config.settingHvrtOv5Time());
			}
			if (isConfigured(config.settingHvrtOv6Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_VOLTAGE),
						config.settingHvrtOv6Voltage());
			}
			if (isConfigured(config.settingHvrtOv6Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_TIME),
						config.settingHvrtOv6Time());
			}
			if (isConfigured(config.settingHvrtOv7Voltage())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_VOLTAGE),
						config.settingHvrtOv7Voltage());
			}
			if (isConfigured(config.settingHvrtOv7Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_TIME),
						config.settingHvrtOv7Time());
			}
			if (isConfigured(config.settingHvrtEnterHighCrossingThreshold())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_ENTER_HIGH_CROSSING),
						config.settingHvrtEnterHighCrossingThreshold());
			}
			if (isConfigured(config.settingHvrtExitHighCrossingThreshold())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_EXIT_HIGH_CROSSING),
						config.settingHvrtExitHighCrossingThreshold());
			}
			if (isConfigured(config.settingHvrtK2Slope())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_K2_SLOPE),
						config.settingHvrtK2Slope());
			}
			if (isConfigured(config.settingHvrtZeroCurrentModeEnable())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENABLE),
						config.settingHvrtZeroCurrentModeEnable());
			}
			if (isConfigured(config.settingHvrtZeroCurrentModeEntryThreshold())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD),
						config.settingHvrtZeroCurrentModeEntryThreshold());
			}

			if (isConfigured(config.settingVrtCurrentDistributionMode())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_CURRENT_DISTRIBUTION_MODE),
						config.settingVrtCurrentDistributionMode());
			}
			if (isConfigured(config.settingVrtActivePowerRecoveryMode())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_MODE),
						config.settingVrtActivePowerRecoveryMode());
			}
			if (isConfigured(config.settingVrtActivePowerRecoverySpeed())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SPEED),
						config.settingVrtActivePowerRecoverySpeed());
			}
			if (isConfigured(config.settingVrtActivePowerRecoverySlope())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SLOPE),
						config.settingVrtActivePowerRecoverySlope());
			}
			if (isConfigured(config.settingVrtReactivePowerRecoveryModeEnd())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_MODE_END),
						config.settingVrtReactivePowerRecoveryModeEnd());
			}
			if (isConfigured(config.settingVrtReactivePowerRecoverySpeed())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SPEED),
						config.settingVrtReactivePowerRecoverySpeed());
			}
			if (isConfigured(config.settingVrtReactivePowerRecoverySlope())) {
				setWriteValueIfNotRead(
						this.parent.channel(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SLOPE),
						config.settingVrtReactivePowerRecoverySlope());
			}

			// --- Frequency Ride Through Parameters ---
			if (isConfigured(config.settingFrtEnable())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_ENABLE),
						config.settingFrtEnable());
			}
			if (isConfigured(config.settingFrtUf1Frequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_UF1_FREQUENCY),
						config.settingFrtUf1Frequency());
			}
			if (isConfigured(config.settingFrtUf1Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_UF1_TIME),
						config.settingFrtUf1Time());
			}
			if (isConfigured(config.settingFrtUf2Frequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_UF2_FREQUENCY),
						config.settingFrtUf2Frequency());
			}
			if (isConfigured(config.settingFrtUf2Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_UF2_TIME),
						config.settingFrtUf2Time());
			}
			if (isConfigured(config.settingFrtUf3Frequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_UF3_FREQUENCY),
						config.settingFrtUf3Frequency());
			}
			if (isConfigured(config.settingFrtUf3Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_UF3_TIME),
						config.settingFrtUf3Time());
			}
			if (isConfigured(config.settingFrtOf1Frequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_OF1_FREQUENCY),
						config.settingFrtOf1Frequency());
			}
			if (isConfigured(config.settingFrtOf1Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_OF1_TIME),
						config.settingFrtOf1Time());
			}
			if (isConfigured(config.settingFrtOf2Frequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_OF2_FREQUENCY),
						config.settingFrtOf2Frequency());
			}
			if (isConfigured(config.settingFrtOf2Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_OF2_TIME),
						config.settingFrtOf2Time());
			}
			if (isConfigured(config.settingFrtOf3Frequency())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_OF3_FREQUENCY),
						config.settingFrtOf3Frequency());
			}
			if (isConfigured(config.settingFrtOf3Time())) {
				setWriteValueIfNotRead(this.parent.channel(GoodWePowerSetting.ChannelId.V2_FRT_OF3_TIME),
						config.settingFrtOf3Time());
			}
		} catch (Exception e) {
			this.log.warn("Unable to set the channel value" + e.getMessage());
		}

	}

	private static boolean isConfigured(long value) {
		return value != Integer.MIN_VALUE;
	}

	private static boolean isConfigured(Enum<?> value) {
		return value != null && !value.name().equals("UNDEFINED");
	}

}
