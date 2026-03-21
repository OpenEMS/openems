package io.openems.edge.goodwe.batteryinverter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.EnableDisableOrUndefined;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.FixPfSetting;
import io.openems.edge.goodwe.common.enums.GridCode;
import io.openems.edge.goodwe.common.enums.ReactivePowerMode;
import io.openems.edge.goodwe.common.enums.SafetyCountry;
import io.openems.edge.goodwe.common.enums.SafetyParameterEnums;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private ControlMode controlMode;
		private String modbusId;
		private int modbusUnitId;
		private SafetyCountry safetyCountry;
		private EnableDisable mpptForShadowEnable;
		private EnableDisable backupEnable;
		private EnableDisable feedPowerEnable;
		private int feedPowerPara;
		private FeedInPowerSettings feedInPowerSettings;
		private EnableDisable rcrEnable = EnableDisable.DISABLE;
		private StartStopConfig startStop;
		private EnableDisable naProtectionEnable = EnableDisable.DISABLE;
		private GridCode gridCode = GridCode.VDE_4105;

		// APM Settings
		private int settingApmOutputActivePower = 0;
		private int settingApmPowerGradient = 0;
		private EnableDisableOrUndefined settingApmPfOverFrequencyCurveEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingApmPfOverFrequencyStartPoint = 0;
		private int settingApmPfOverFrequencySlope = 0;
		private int settingApmPfOverFrequencyDelayTime = 0;
		private EnableDisableOrUndefined settingApmPfOverFrequencyDeactivationThresholdFstop = EnableDisableOrUndefined.UNDEFINED;
		private int settingApmPfOverFrequencyHysteresisPoint = 0;
		private int settingApmPOverFrequencyDelayWaitingTime = 0;
		private int settingApmPfOverFrequencyHysteresisSlope = 0;
		private EnableDisableOrUndefined settingApmPfUnderFrequencyCurveEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingApmPfUnderFrequencyThreshold = 0;
		private int settingApmPfUnderFrequencySlope = 0;
		private int settingApmPfUnderFrequencyDelayTime = 0;
		private EnableDisableOrUndefined settingApmPfUnderFrequencyDeactivationThresholdFstop = EnableDisableOrUndefined.UNDEFINED;
		private int settingApmPfUnderFrequencyHysteresisPoint = 0;
		private int settingApmPUnderFrequencyDelayWaitingTime = 0;
		private int settingApmPfUnderFrequencyHysteresisSlope = 0;

		// PU Curve Settings
		private EnableDisableOrUndefined puCurveEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingApmPuV1Voltage = 0;
		private int settingApmPuV1ActivePower = 0;
		private int settingApmPuV2Voltage = 0;
		private int settingApmPuV2ActivePower = 0;
		private int settingApmPuV3Voltage = 0;
		private int settingApmPuV3ActivePower = 0;
		private int settingApmPuV4Voltage = 0;
		private int settingApmPuV4ActivePower = 0;
		private SafetyParameterEnums.Vrt.GeneralRecoveryMode settingApmPuResponseMode = SafetyParameterEnums.Vrt.GeneralRecoveryMode.UNDEFINED;
		private int settingApmPuPt1LowPassFilterTimeConstantPt1Mode = 0;
		private int settingApmPuPt1LowPassFilterTimeConstantGradientMode = 0;

		// RPM Settings
		private SafetyParameterEnums.Rpm.Mode settingRpmQuCurveMode = SafetyParameterEnums.Rpm.Mode.UNDEFINED;
		private int settingRpmQuVoltageDeadBand = 0;
		private int settingRpmQuOverexcitedSlope = 0;
		private int settingRpmQuUnderexcitedSlope = 0;
		private ReactivePowerMode settingRpmMode = ReactivePowerMode.UNSELECTED;
		private FixPfSetting settingRpmFixPf = FixPfSetting.LEADING_1_OR_NONE;
		private int settingRpmFixQ = 0;
		private int settingRpmQuV1Voltage = 0;
		private int settingRpmQuV1ReactivePower = 0;
		private int settingRpmQuV2Voltage = 0;
		private int settingRpmQuV2ReactivePower = 0;
		private int settingRpmQuV3Voltage = 0;
		private int settingRpmQuV3ReactivePower = 0;
		private int settingRpmQuV4Voltage = 0;
		private int settingRpmQuV4ReactivePower = 0;
		private int settingRpmQuTimeConstant = 0;
		private EnableDisableOrUndefined settingRpmQuExtendedFunctions = EnableDisableOrUndefined.UNDEFINED;
		private int settingRpmQuLockInPower = 0;
		private int settingRpmQuLockOutPower = 0;

		// RPM CosPhi Settings
		private SafetyParameterEnums.Rpm.Mode settingRpmCosPhiPCurveMode = SafetyParameterEnums.Rpm.Mode.UNDEFINED;
		private int settingRpmCosPhiPOverexcitedSlope = 0;
		private int settingRpmCosPhiPUnderexcitedSlope = 0;
		private int settingRpmCosPhipPowerA = 0;
		private int settingRpmCosPhipCosPhiA = 0;
		private int settingRpmCosPhipPowerB = 0;
		private int settingRpmCosPhipCosPhiB = 0;
		private int settingRpmCosPhipPowerC = 0;
		private int settingRpmCosPhipCosPhiC = 0;
		private int settingRpmCosPhipPowerD = 0;
		private int settingRpmCosPhipCosPhiD = 0;
		private int settingRpmCosPhipPowerE = 0;
		private int settingRpmCosPhipCosPhiE = 0;
		private int settingRpmCosPhipTimeConstant = 0;
		private EnableDisableOrUndefined settingRpmCosPhipExtendedFunctions = EnableDisableOrUndefined.UNDEFINED;
		private int settingRpmCosPhipLockInVolt = 0;
		private int settingRpmCosPhipLockOutVolt = 0;

		// QP Settings
		private SafetyParameterEnums.Rpm.Mode settingQpCurveMode = SafetyParameterEnums.Rpm.Mode.UNDEFINED;
		private int settingQpOverexcitedSlope = 0;
		private int settingQpUnderexcitedSlope = 0;
		private int settingRpmQpPowerP1 = 0;
		private int settingRpmQpReactivePowerP1 = 0;
		private int settingRpmQpPowerP2 = 0;
		private int settingRpmQpReactivePowerP2 = 0;
		private int settingRpmQpPowerP3 = 0;
		private int settingRpmQpReactivePowerP3 = 0;
		private int settingRpmQpPowerP4 = 0;
		private int settingRpmQpReactivePowerP4 = 0;
		private int settingRpmQpPowerP5 = 0;
		private int settingRpmQpReactivePowerP5 = 0;
		private int settingRpmQpPowerP6 = 0;
		private int settingRpmQpReactivePowerP6 = 0;
		private int settingRpmQpTimeConstant = 0;

		// VPP Settings
		private int settingVppOvStage1TriggerValue = 0;
		private long settingVppOvStage1TripTime = 0;
		private int settingVppUvStage1TripValue = 0;
		private long settingVppUvStage1TripTime = 0;
		private int settingVppOvStage2TriggerValue = 0;
		private long settingVppOvStage2TripTime = 0;
		private int settingVppUvStage2TripValue = 0;
		private long settingVppUvStage2TripTime = 0;
		private int settingVppOvStage3TriggerValue = 0;
		private long settingVppOvStage3TripTime = 0;
		private int settingVppUvStage3TripValue = 0;
		private long settingVppUvStage3TripTime = 0;
		private int settingVppOvStage4TriggerValue = 0;
		private long settingVppOvStage4TripTime = 0;
		private int settingVppUvStage4TripValue = 0;
		private long settingVppUvStage4TripTime = 0;
		private int settingVpp10MinOvTripThreshold = 0;
		private long settingVpp10MinOvTripTime = 0;

		// FPP Settings
		private int settingFppOfStage1TriggerValue = 0;
		private long settingFppOfStage1TripTime = 0;
		private int settingFppUfStage1TripValue = 0;
		private long settingFppUfStage1TripTime = 0;
		private int settingFppOfStage2TriggerValue = 0;
		private long settingFppOfStage2TripTime = 0;
		private int settingFppUfStage2TripValue = 0;
		private long settingFppUfStage2TripTime = 0;
		private int settingFppOfStage3TriggerValue = 0;
		private long settingFppOfStage3TripTime = 0;
		private int settingFppUfStage3TripValue = 0;
		private long settingFppUfStage3TripTime = 0;
		private int settingFppOfStage4TriggerValue = 0;
		private long settingFppOfStage4TripTime = 0;
		private int settingFppUfStage4TripValue = 0;
		private long settingFppUfStage4TripTime = 0;

		// CP Settings
		private int settingCpRampUpUpperVoltage = 0;
		private int settingCpRampUpLowerVoltage = 0;
		private int settingCpRampUpUpperFrequency = 0;
		private int settingCpRampUpLowerFrequency = 0;
		private int settingCpRampUpObservationTime = 0;
		private EnableDisableOrUndefined settingCpSoftRampUpGradientEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingCpSoftRampUpGradient = 0;
		private int settingCpReconnectionUpperVoltage = 0;
		private int settingCpReconnectionLowerVoltage = 0;
		private int settingCpReconnectionUpperFrequency = 0;
		private int settingCpReconnectionLowerFrequency = 0;
		private int settingCpReconnectionObservationTime = 0;
		private EnableDisableOrUndefined settingCpReconnectionGradientEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingCpReconnectionGradient = 0;

		// LVRT Settings
		private EnableDisableOrUndefined settingLvrtEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingLvrtUv1Voltage = 0;
		private int settingLvrtUv1Time = 0;
		private int settingLvrtUv2Voltage = 0;
		private int settingLvrtUv2Time = 0;
		private int settingLvrtUv3Voltage = 0;
		private int settingLvrtUv3Time = 0;
		private int settingLvrtUv4Voltage = 0;
		private int settingLvrtUv4Time = 0;
		private int settingLvrtUv5Voltage = 0;
		private int settingLvrtUv5Time = 0;
		private int settingLvrtUv6Voltage = 0;
		private int settingLvrtUv6Time = 0;
		private int settingLvrtUv7Voltage = 0;
		private int settingLvrtUv7Time = 0;
		private int settingLvrtEnterThreshold = 0;
		private int settingLvrtExitEndpoint = 0;
		private int settingLvrtK1Slope = 0;
		private EnableDisableOrUndefined settingLvrtZeroCurrentModeEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingLvrtZeroCurrentModeEntryThreshold = 0;

		// HVRT Settings
		private EnableDisableOrUndefined settingHvrtEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingHvrtOv1Voltage = 0;
		private int settingHvrtOv1Time = 0;
		private int settingHvrtOv2Voltage = 0;
		private int settingHvrtOv2Time = 0;
		private int settingHvrtOv3Voltage = 0;
		private int settingHvrtOv3Time = 0;
		private int settingHvrtOv4Voltage = 0;
		private int settingHvrtOv4Time = 0;
		private int settingHvrtOv5Voltage = 0;
		private int settingHvrtOv5Time = 0;
		private int settingHvrtOv6Voltage = 0;
		private int settingHvrtOv6Time = 0;
		private int settingHvrtOv7Voltage = 0;
		private int settingHvrtOv7Time = 0;
		private int settingHvrtEnterHighCrossingThreshold = 0;
		private int settingHvrtExitHighCrossingThreshold = 0;
		private int settingHvrtK2Slope = 0;
		private EnableDisableOrUndefined settingHvrtZeroCurrentModeEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingHvrtZeroCurrentModeEntryThreshold = 0;

		// VRT Settings
		private SafetyParameterEnums.Vrt.CurrentDistributionMode settingVrtCurrentDistributionMode = SafetyParameterEnums.Vrt.CurrentDistributionMode.UNDEFINED;
		private SafetyParameterEnums.Vrt.GeneralRecoveryMode settingVrtActivePowerRecoveryMode = SafetyParameterEnums.Vrt.GeneralRecoveryMode.UNDEFINED;
		private int settingVrtActivePowerRecoverySpeed = 0;
		private long settingVrtActivePowerRecoverySlope = 0;
		private SafetyParameterEnums.Vrt.GeneralRecoveryMode settingVrtReactivePowerRecoveryModeEnd = SafetyParameterEnums.Vrt.GeneralRecoveryMode.UNDEFINED;
		private int settingVrtReactivePowerRecoverySpeed = 0;
		private long settingVrtReactivePowerRecoverySlope = 0;

		// FRT Settings
		private EnableDisableOrUndefined settingFrtEnable = EnableDisableOrUndefined.UNDEFINED;
		private int settingFrtUf1Frequency = 0;
		private int settingFrtUf1Time = 0;
		private int settingFrtUf2Frequency = 0;
		private int settingFrtUf2Time = 0;
		private int settingFrtUf3Frequency = 0;
		private int settingFrtUf3Time = 0;
		private int settingFrtOf1Frequency = 0;
		private int settingFrtOf1Time = 0;
		private int settingFrtOf2Frequency = 0;
		private int settingFrtOf2Time = 0;
		private int settingFrtOf3Frequency = 0;
		private int settingFrtOf3Time = 0;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setControlMode(ControlMode controlMode) {
			this.controlMode = controlMode;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public Builder setSafetyCountry(SafetyCountry safetyCountry) {
			this.safetyCountry = safetyCountry;
			return this;
		}

		public Builder setMpptForShadowEnable(EnableDisable mpptForShadowEnable) {
			this.mpptForShadowEnable = mpptForShadowEnable;
			return this;
		}

		public Builder setBackupEnable(EnableDisable backupEnable) {
			this.backupEnable = backupEnable;
			return this;
		}

		public Builder setFeedPowerEnable(EnableDisable feedPowerEnable) {
			this.feedPowerEnable = feedPowerEnable;
			return this;
		}

		public Builder setFeedPowerPara(int feedPowerPara) {
			this.feedPowerPara = feedPowerPara;
			return this;
		}

		public Builder setFeedInPowerSettings(FeedInPowerSettings feedInPowerSettings) {
			this.feedInPowerSettings = feedInPowerSettings;
			return this;
		}

		public Builder setRcrEnable(EnableDisable rcrEnable) {
			this.rcrEnable = rcrEnable;
			return this;
		}

		public Builder setNaProtectionEnable(EnableDisable naProtectionEnable) {
			this.naProtectionEnable = naProtectionEnable;
			return this;
		}

		public Builder setStartStop(StartStopConfig startStop) {
			this.startStop = startStop;
			return this;
		}

		public Builder setGridCode(GridCode gridCode) {
			this.gridCode = gridCode;
			return this;
		}

		// APM Setters
		public Builder setSettingApmOutputActivePower(int value) {
			this.settingApmOutputActivePower = value;
			return this;
		}

		public Builder setSettingApmPowerGradient(int value) {
			this.settingApmPowerGradient = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencyCurveEnable(EnableDisableOrUndefined value) {
			this.settingApmPfOverFrequencyCurveEnable = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencyStartPoint(int value) {
			this.settingApmPfOverFrequencyStartPoint = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencySlope(int value) {
			this.settingApmPfOverFrequencySlope = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencyDelayTime(int value) {
			this.settingApmPfOverFrequencyDelayTime = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencyDeactivationThresholdFstop(EnableDisableOrUndefined value) {
			this.settingApmPfOverFrequencyDeactivationThresholdFstop = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencyHysteresisPoint(int value) {
			this.settingApmPfOverFrequencyHysteresisPoint = value;
			return this;
		}

		public Builder setSettingApmPOverFrequencyDelayWaitingTime(int value) {
			this.settingApmPOverFrequencyDelayWaitingTime = value;
			return this;
		}

		public Builder setSettingApmPfOverFrequencyHysteresisSlope(int value) {
			this.settingApmPfOverFrequencyHysteresisSlope = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencyCurveEnable(EnableDisableOrUndefined value) {
			this.settingApmPfUnderFrequencyCurveEnable = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencyThreshold(int value) {
			this.settingApmPfUnderFrequencyThreshold = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencySlope(int value) {
			this.settingApmPfUnderFrequencySlope = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencyDelayTime(int value) {
			this.settingApmPfUnderFrequencyDelayTime = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencyDeactivationThresholdFstop(EnableDisableOrUndefined value) {
			this.settingApmPfUnderFrequencyDeactivationThresholdFstop = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencyHysteresisPoint(int value) {
			this.settingApmPfUnderFrequencyHysteresisPoint = value;
			return this;
		}

		public Builder setSettingApmPUnderFrequencyDelayWaitingTime(int value) {
			this.settingApmPUnderFrequencyDelayWaitingTime = value;
			return this;
		}

		public Builder setSettingApmPfUnderFrequencyHysteresisSlope(int value) {
			this.settingApmPfUnderFrequencyHysteresisSlope = value;
			return this;
		}

		// PU Curve Setters
		public Builder setPuCurveEnable(EnableDisableOrUndefined value) {
			this.puCurveEnable = value;
			return this;
		}

		public Builder setSettingApmPuV1Voltage(int value) {
			this.settingApmPuV1Voltage = value;
			return this;
		}

		public Builder setSettingApmPuV1ActivePower(int value) {
			this.settingApmPuV1ActivePower = value;
			return this;
		}

		public Builder setSettingApmPuV2Voltage(int value) {
			this.settingApmPuV2Voltage = value;
			return this;
		}

		public Builder setSettingApmPuV2ActivePower(int value) {
			this.settingApmPuV2ActivePower = value;
			return this;
		}

		public Builder setSettingApmPuV3Voltage(int value) {
			this.settingApmPuV3Voltage = value;
			return this;
		}

		public Builder setSettingApmPuV3ActivePower(int value) {
			this.settingApmPuV3ActivePower = value;
			return this;
		}

		public Builder setSettingApmPuV4Voltage(int value) {
			this.settingApmPuV4Voltage = value;
			return this;
		}

		public Builder setSettingApmPuV4ActivePower(int value) {
			this.settingApmPuV4ActivePower = value;
			return this;
		}

		public Builder setSettingApmPuResponseMode(SafetyParameterEnums.Vrt.GeneralRecoveryMode value) {
			this.settingApmPuResponseMode = value;
			return this;
		}

		public Builder setSettingApmPuPt1LowPassFilterTimeConstantPt1Mode(int value) {
			this.settingApmPuPt1LowPassFilterTimeConstantPt1Mode = value;
			return this;
		}

		public Builder setSettingApmPuPt1LowPassFilterTimeConstantGradientMode(int value) {
			this.settingApmPuPt1LowPassFilterTimeConstantGradientMode = value;
			return this;
		}

		// RPM Setters
		public Builder setSettingRpmQuCurveMode(SafetyParameterEnums.Rpm.Mode value) {
			this.settingRpmQuCurveMode = value;
			return this;
		}

		public Builder setSettingRpmQuVoltageDeadBand(int value) {
			this.settingRpmQuVoltageDeadBand = value;
			return this;
		}

		public Builder setSettingRpmQuOverexcitedSlope(int value) {
			this.settingRpmQuOverexcitedSlope = value;
			return this;
		}

		public Builder setSettingRpmQuUnderexcitedSlope(int value) {
			this.settingRpmQuUnderexcitedSlope = value;
			return this;
		}

		public Builder setSettingRpmMode(ReactivePowerMode value) {
			this.settingRpmMode = value;
			return this;
		}

		public Builder setSettingRpmFixPf(FixPfSetting value) {
			this.settingRpmFixPf = value;
			return this;
		}

		public Builder setSettingRpmFixQ(int value) {
			this.settingRpmFixQ = value;
			return this;
		}

		public Builder setSettingRpmQuV1Voltage(int value) {
			this.settingRpmQuV1Voltage = value;
			return this;
		}

		public Builder setSettingRpmQuV1ReactivePower(int value) {
			this.settingRpmQuV1ReactivePower = value;
			return this;
		}

		public Builder setSettingRpmQuV2Voltage(int value) {
			this.settingRpmQuV2Voltage = value;
			return this;
		}

		public Builder setSettingRpmQuV2ReactivePower(int value) {
			this.settingRpmQuV2ReactivePower = value;
			return this;
		}

		public Builder setSettingRpmQuV3Voltage(int value) {
			this.settingRpmQuV3Voltage = value;
			return this;
		}

		public Builder setSettingRpmQuV3ReactivePower(int value) {
			this.settingRpmQuV3ReactivePower = value;
			return this;
		}

		public Builder setSettingRpmQuV4Voltage(int value) {
			this.settingRpmQuV4Voltage = value;
			return this;
		}

		public Builder setSettingRpmQuV4ReactivePower(int value) {
			this.settingRpmQuV4ReactivePower = value;
			return this;
		}

		public Builder setSettingRpmQuTimeConstant(int value) {
			this.settingRpmQuTimeConstant = value;
			return this;
		}

		public Builder setSettingRpmQuExtendedFunctions(EnableDisableOrUndefined value) {
			this.settingRpmQuExtendedFunctions = value;
			return this;
		}

		public Builder setSettingRpmQuLockInPower(int value) {
			this.settingRpmQuLockInPower = value;
			return this;
		}

		public Builder setSettingRpmQuLockOutPower(int value) {
			this.settingRpmQuLockOutPower = value;
			return this;
		}

		// RPM CosPhi Setters
		public Builder setSettingRpmCosPhiPCurveMode(SafetyParameterEnums.Rpm.Mode value) {
			this.settingRpmCosPhiPCurveMode = value;
			return this;
		}

		public Builder setSettingRpmCosPhiPOverexcitedSlope(int value) {
			this.settingRpmCosPhiPOverexcitedSlope = value;
			return this;
		}

		public Builder setSettingRpmCosPhiPUnderexcitedSlope(int value) {
			this.settingRpmCosPhiPUnderexcitedSlope = value;
			return this;
		}

		public Builder setSettingRpmCosPhipPowerA(int value) {
			this.settingRpmCosPhipPowerA = value;
			return this;
		}

		public Builder setSettingRpmCosPhipCosPhiA(int value) {
			this.settingRpmCosPhipCosPhiA = value;
			return this;
		}

		public Builder setSettingRpmCosPhipPowerB(int value) {
			this.settingRpmCosPhipPowerB = value;
			return this;
		}

		public Builder setSettingRpmCosPhipCosPhiB(int value) {
			this.settingRpmCosPhipCosPhiB = value;
			return this;
		}

		public Builder setSettingRpmCosPhipPowerC(int value) {
			this.settingRpmCosPhipPowerC = value;
			return this;
		}

		public Builder setSettingRpmCosPhipCosPhiC(int value) {
			this.settingRpmCosPhipCosPhiC = value;
			return this;
		}

		public Builder setSettingRpmCosPhipPowerD(int value) {
			this.settingRpmCosPhipPowerD = value;
			return this;
		}

		public Builder setSettingRpmCosPhipCosPhiD(int value) {
			this.settingRpmCosPhipCosPhiD = value;
			return this;
		}

		public Builder setSettingRpmCosPhipPowerE(int value) {
			this.settingRpmCosPhipPowerE = value;
			return this;
		}

		public Builder setSettingRpmCosPhipCosPhiE(int value) {
			this.settingRpmCosPhipCosPhiE = value;
			return this;
		}

		public Builder setSettingRpmCosPhipTimeConstant(int value) {
			this.settingRpmCosPhipTimeConstant = value;
			return this;
		}

		public Builder setSettingRpmCosPhipExtendedFunctions(EnableDisableOrUndefined value) {
			this.settingRpmCosPhipExtendedFunctions = value;
			return this;
		}

		public Builder setSettingRpmCosPhipLockInVolt(int value) {
			this.settingRpmCosPhipLockInVolt = value;
			return this;
		}

		public Builder setSettingRpmCosPhipLockOutVolt(int value) {
			this.settingRpmCosPhipLockOutVolt = value;
			return this;
		}

		// QP Setters
		public Builder setSettingQpCurveMode(SafetyParameterEnums.Rpm.Mode value) {
			this.settingQpCurveMode = value;
			return this;
		}

		public Builder setSettingQpOverexcitedSlope(int value) {
			this.settingQpOverexcitedSlope = value;
			return this;
		}

		public Builder setSettingQpUnderexcitedSlope(int value) {
			this.settingQpUnderexcitedSlope = value;
			return this;
		}

		public Builder setSettingRpmQpPowerP1(int value) {
			this.settingRpmQpPowerP1 = value;
			return this;
		}

		public Builder setSettingRpmQpReactivePowerP1(int value) {
			this.settingRpmQpReactivePowerP1 = value;
			return this;
		}

		public Builder setSettingRpmQpPowerP2(int value) {
			this.settingRpmQpPowerP2 = value;
			return this;
		}

		public Builder setSettingRpmQpReactivePowerP2(int value) {
			this.settingRpmQpReactivePowerP2 = value;
			return this;
		}

		public Builder setSettingRpmQpPowerP3(int value) {
			this.settingRpmQpPowerP3 = value;
			return this;
		}

		public Builder setSettingRpmQpReactivePowerP3(int value) {
			this.settingRpmQpReactivePowerP3 = value;
			return this;
		}

		public Builder setSettingRpmQpPowerP4(int value) {
			this.settingRpmQpPowerP4 = value;
			return this;
		}

		public Builder setSettingRpmQpReactivePowerP4(int value) {
			this.settingRpmQpReactivePowerP4 = value;
			return this;
		}

		public Builder setSettingRpmQpPowerP5(int value) {
			this.settingRpmQpPowerP5 = value;
			return this;
		}

		public Builder setSettingRpmQpReactivePowerP5(int value) {
			this.settingRpmQpReactivePowerP5 = value;
			return this;
		}

		public Builder setSettingRpmQpPowerP6(int value) {
			this.settingRpmQpPowerP6 = value;
			return this;
		}

		public Builder setSettingRpmQpReactivePowerP6(int value) {
			this.settingRpmQpReactivePowerP6 = value;
			return this;
		}

		public Builder setSettingRpmQpTimeConstant(int value) {
			this.settingRpmQpTimeConstant = value;
			return this;
		}

		// VPP Setters
		public Builder setSettingVppOvStage1TriggerValue(int value) {
			this.settingVppOvStage1TriggerValue = value;
			return this;
		}

		public Builder setSettingVppOvStage1TripTime(long value) {
			this.settingVppOvStage1TripTime = value;
			return this;
		}

		public Builder setSettingVppUvStage1TripValue(int value) {
			this.settingVppUvStage1TripValue = value;
			return this;
		}

		public Builder setSettingVppUvStage1TripTime(long value) {
			this.settingVppUvStage1TripTime = value;
			return this;
		}

		public Builder setSettingVppOvStage2TriggerValue(int value) {
			this.settingVppOvStage2TriggerValue = value;
			return this;
		}

		public Builder setSettingVppOvStage2TripTime(long value) {
			this.settingVppOvStage2TripTime = value;
			return this;
		}

		public Builder setSettingVppUvStage2TripValue(int value) {
			this.settingVppUvStage2TripValue = value;
			return this;
		}

		public Builder setSettingVppUvStage2TripTime(long value) {
			this.settingVppUvStage2TripTime = value;
			return this;
		}

		public Builder setSettingVppOvStage3TriggerValue(int value) {
			this.settingVppOvStage3TriggerValue = value;
			return this;
		}

		public Builder setSettingVppOvStage3TripTime(long value) {
			this.settingVppOvStage3TripTime = value;
			return this;
		}

		public Builder setSettingVppUvStage3TripValue(int value) {
			this.settingVppUvStage3TripValue = value;
			return this;
		}

		public Builder setSettingVppUvStage3TripTime(long value) {
			this.settingVppUvStage3TripTime = value;
			return this;
		}

		public Builder setSettingVppOvStage4TriggerValue(int value) {
			this.settingVppOvStage4TriggerValue = value;
			return this;
		}

		public Builder setSettingVppOvStage4TripTime(long value) {
			this.settingVppOvStage4TripTime = value;
			return this;
		}

		public Builder setSettingVppUvStage4TripValue(int value) {
			this.settingVppUvStage4TripValue = value;
			return this;
		}

		public Builder setSettingVppUvStage4TripTime(long value) {
			this.settingVppUvStage4TripTime = value;
			return this;
		}

		public Builder setSettingVpp10MinOvTripThreshold(int value) {
			this.settingVpp10MinOvTripThreshold = value;
			return this;
		}

		public Builder setSettingVpp10MinOvTripTime(long value) {
			this.settingVpp10MinOvTripTime = value;
			return this;
		}

		// FPP Setters
		public Builder setSettingFppOfStage1TriggerValue(int value) {
			this.settingFppOfStage1TriggerValue = value;
			return this;
		}

		public Builder setSettingFppOfStage1TripTime(long value) {
			this.settingFppOfStage1TripTime = value;
			return this;
		}

		public Builder setSettingFppUfStage1TripValue(int value) {
			this.settingFppUfStage1TripValue = value;
			return this;
		}

		public Builder setSettingFppUfStage1TripTime(long value) {
			this.settingFppUfStage1TripTime = value;
			return this;
		}

		public Builder setSettingFppOfStage2TriggerValue(int value) {
			this.settingFppOfStage2TriggerValue = value;
			return this;
		}

		public Builder setSettingFppOfStage2TripTime(long value) {
			this.settingFppOfStage2TripTime = value;
			return this;
		}

		public Builder setSettingFppUfStage2TripValue(int value) {
			this.settingFppUfStage2TripValue = value;
			return this;
		}

		public Builder setSettingFppUfStage2TripTime(long value) {
			this.settingFppUfStage2TripTime = value;
			return this;
		}

		public Builder setSettingFppOfStage3TriggerValue(int value) {
			this.settingFppOfStage3TriggerValue = value;
			return this;
		}

		public Builder setSettingFppOfStage3TripTime(long value) {
			this.settingFppOfStage3TripTime = value;
			return this;
		}

		public Builder setSettingFppUfStage3TripValue(int value) {
			this.settingFppUfStage3TripValue = value;
			return this;
		}

		public Builder setSettingFppUfStage3TripTime(long value) {
			this.settingFppUfStage3TripTime = value;
			return this;
		}

		public Builder setSettingFppOfStage4TriggerValue(int value) {
			this.settingFppOfStage4TriggerValue = value;
			return this;
		}

		public Builder setSettingFppOfStage4TripTime(long value) {
			this.settingFppOfStage4TripTime = value;
			return this;
		}

		public Builder setSettingFppUfStage4TripValue(int value) {
			this.settingFppUfStage4TripValue = value;
			return this;
		}

		public Builder setSettingFppUfStage4TripTime(long value) {
			this.settingFppUfStage4TripTime = value;
			return this;
		}

		// CP Setters
		public Builder setSettingCpRampUpUpperVoltage(int value) {
			this.settingCpRampUpUpperVoltage = value;
			return this;
		}

		public Builder setSettingCpRampUpLowerVoltage(int value) {
			this.settingCpRampUpLowerVoltage = value;
			return this;
		}

		public Builder setSettingCpRampUpUpperFrequency(int value) {
			this.settingCpRampUpUpperFrequency = value;
			return this;
		}

		public Builder setSettingCpRampUpLowerFrequency(int value) {
			this.settingCpRampUpLowerFrequency = value;
			return this;
		}

		public Builder setSettingCpRampUpObservationTime(int value) {
			this.settingCpRampUpObservationTime = value;
			return this;
		}

		public Builder setSettingCpSoftRampUpGradientEnable(EnableDisableOrUndefined value) {
			this.settingCpSoftRampUpGradientEnable = value;
			return this;
		}

		public Builder setSettingCpSoftRampUpGradient(int value) {
			this.settingCpSoftRampUpGradient = value;
			return this;
		}

		public Builder setSettingCpReconnectionUpperVoltage(int value) {
			this.settingCpReconnectionUpperVoltage = value;
			return this;
		}

		public Builder setSettingCpReconnectionLowerVoltage(int value) {
			this.settingCpReconnectionLowerVoltage = value;
			return this;
		}

		public Builder setSettingCpReconnectionUpperFrequency(int value) {
			this.settingCpReconnectionUpperFrequency = value;
			return this;
		}

		public Builder setSettingCpReconnectionLowerFrequency(int value) {
			this.settingCpReconnectionLowerFrequency = value;
			return this;
		}

		public Builder setSettingCpReconnectionObservationTime(int value) {
			this.settingCpReconnectionObservationTime = value;
			return this;
		}

		public Builder setSettingCpReconnectionGradientEnable(EnableDisableOrUndefined value) {
			this.settingCpReconnectionGradientEnable = value;
			return this;
		}

		public Builder setSettingCpReconnectionGradient(int value) {
			this.settingCpReconnectionGradient = value;
			return this;
		}

		// LVRT Setters
		public Builder setSettingLvrtEnable(EnableDisableOrUndefined value) {
			this.settingLvrtEnable = value;
			return this;
		}

		public Builder setSettingLvrtUv1Voltage(int value) {
			this.settingLvrtUv1Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv1Time(int value) {
			this.settingLvrtUv1Time = value;
			return this;
		}

		public Builder setSettingLvrtUv2Voltage(int value) {
			this.settingLvrtUv2Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv2Time(int value) {
			this.settingLvrtUv2Time = value;
			return this;
		}

		public Builder setSettingLvrtUv3Voltage(int value) {
			this.settingLvrtUv3Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv3Time(int value) {
			this.settingLvrtUv3Time = value;
			return this;
		}

		public Builder setSettingLvrtUv4Voltage(int value) {
			this.settingLvrtUv4Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv4Time(int value) {
			this.settingLvrtUv4Time = value;
			return this;
		}

		public Builder setSettingLvrtUv5Voltage(int value) {
			this.settingLvrtUv5Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv5Time(int value) {
			this.settingLvrtUv5Time = value;
			return this;
		}

		public Builder setSettingLvrtUv6Voltage(int value) {
			this.settingLvrtUv6Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv6Time(int value) {
			this.settingLvrtUv6Time = value;
			return this;
		}

		public Builder setSettingLvrtUv7Voltage(int value) {
			this.settingLvrtUv7Voltage = value;
			return this;
		}

		public Builder setSettingLvrtUv7Time(int value) {
			this.settingLvrtUv7Time = value;
			return this;
		}

		public Builder setSettingLvrtEnterThreshold(int value) {
			this.settingLvrtEnterThreshold = value;
			return this;
		}

		public Builder setSettingLvrtExitEndpoint(int value) {
			this.settingLvrtExitEndpoint = value;
			return this;
		}

		public Builder setSettingLvrtK1Slope(int value) {
			this.settingLvrtK1Slope = value;
			return this;
		}

		public Builder setSettingLvrtZeroCurrentModeEnable(EnableDisableOrUndefined value) {
			this.settingLvrtZeroCurrentModeEnable = value;
			return this;
		}

		public Builder setSettingLvrtZeroCurrentModeEntryThreshold(int value) {
			this.settingLvrtZeroCurrentModeEntryThreshold = value;
			return this;
		}

		// HVRT Setters
		public Builder setSettingHvrtEnable(EnableDisableOrUndefined value) {
			this.settingHvrtEnable = value;
			return this;
		}

		public Builder setSettingHvrtOv1Voltage(int value) {
			this.settingHvrtOv1Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv1Time(int value) {
			this.settingHvrtOv1Time = value;
			return this;
		}

		public Builder setSettingHvrtOv2Voltage(int value) {
			this.settingHvrtOv2Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv2Time(int value) {
			this.settingHvrtOv2Time = value;
			return this;
		}

		public Builder setSettingHvrtOv3Voltage(int value) {
			this.settingHvrtOv3Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv3Time(int value) {
			this.settingHvrtOv3Time = value;
			return this;
		}

		public Builder setSettingHvrtOv4Voltage(int value) {
			this.settingHvrtOv4Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv4Time(int value) {
			this.settingHvrtOv4Time = value;
			return this;
		}

		public Builder setSettingHvrtOv5Voltage(int value) {
			this.settingHvrtOv5Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv5Time(int value) {
			this.settingHvrtOv5Time = value;
			return this;
		}

		public Builder setSettingHvrtOv6Voltage(int value) {
			this.settingHvrtOv6Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv6Time(int value) {
			this.settingHvrtOv6Time = value;
			return this;
		}

		public Builder setSettingHvrtOv7Voltage(int value) {
			this.settingHvrtOv7Voltage = value;
			return this;
		}

		public Builder setSettingHvrtOv7Time(int value) {
			this.settingHvrtOv7Time = value;
			return this;
		}

		public Builder setSettingHvrtEnterHighCrossingThreshold(int value) {
			this.settingHvrtEnterHighCrossingThreshold = value;
			return this;
		}

		public Builder setSettingHvrtExitHighCrossingThreshold(int value) {
			this.settingHvrtExitHighCrossingThreshold = value;
			return this;
		}

		public Builder setSettingHvrtK2Slope(int value) {
			this.settingHvrtK2Slope = value;
			return this;
		}

		public Builder setSettingHvrtZeroCurrentModeEnable(EnableDisableOrUndefined value) {
			this.settingHvrtZeroCurrentModeEnable = value;
			return this;
		}

		public Builder setSettingHvrtZeroCurrentModeEntryThreshold(int value) {
			this.settingHvrtZeroCurrentModeEntryThreshold = value;
			return this;
		}

		// VRT Setters
		public Builder setSettingVrtCurrentDistributionMode(SafetyParameterEnums.Vrt.CurrentDistributionMode value) {
			this.settingVrtCurrentDistributionMode = value;
			return this;
		}

		public Builder setSettingVrtActivePowerRecoveryMode(SafetyParameterEnums.Vrt.GeneralRecoveryMode value) {
			this.settingVrtActivePowerRecoveryMode = value;
			return this;
		}

		public Builder setSettingVrtActivePowerRecoverySpeed(int value) {
			this.settingVrtActivePowerRecoverySpeed = value;
			return this;
		}

		public Builder setSettingVrtActivePowerRecoverySlope(long value) {
			this.settingVrtActivePowerRecoverySlope = value;
			return this;
		}

		public Builder setSettingVrtReactivePowerRecoveryModeEnd(SafetyParameterEnums.Vrt.GeneralRecoveryMode value) {
			this.settingVrtReactivePowerRecoveryModeEnd = value;
			return this;
		}

		public Builder setSettingVrtReactivePowerRecoverySpeed(int value) {
			this.settingVrtReactivePowerRecoverySpeed = value;
			return this;
		}

		public Builder setSettingVrtReactivePowerRecoverySlope(long value) {
			this.settingVrtReactivePowerRecoverySlope = value;
			return this;
		}

		// FRT Setters
		public Builder setSettingFrtEnable(EnableDisableOrUndefined value) {
			this.settingFrtEnable = value;
			return this;
		}

		public Builder setSettingFrtUf1Frequency(int value) {
			this.settingFrtUf1Frequency = value;
			return this;
		}

		public Builder setSettingFrtUf1Time(int value) {
			this.settingFrtUf1Time = value;
			return this;
		}

		public Builder setSettingFrtUf2Frequency(int value) {
			this.settingFrtUf2Frequency = value;
			return this;
		}

		public Builder setSettingFrtUf2Time(int value) {
			this.settingFrtUf2Time = value;
			return this;
		}

		public Builder setSettingFrtUf3Frequency(int value) {
			this.settingFrtUf3Frequency = value;
			return this;
		}

		public Builder setSettingFrtUf3Time(int value) {
			this.settingFrtUf3Time = value;
			return this;
		}

		public Builder setSettingFrtOf1Frequency(int value) {
			this.settingFrtOf1Frequency = value;
			return this;
		}

		public Builder setSettingFrtOf1Time(int value) {
			this.settingFrtOf1Time = value;
			return this;
		}

		public Builder setSettingFrtOf2Frequency(int value) {
			this.settingFrtOf2Frequency = value;
			return this;
		}

		public Builder setSettingFrtOf2Time(int value) {
			this.settingFrtOf2Time = value;
			return this;
		}

		public Builder setSettingFrtOf3Frequency(int value) {
			this.settingFrtOf3Frequency = value;
			return this;
		}

		public Builder setSettingFrtOf3Time(int value) {
			this.settingFrtOf3Time = value;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public SafetyCountry safetyCountry() {
		return this.builder.safetyCountry;
	}

	@Override
	public EnableDisable mpptForShadowEnable() {
		return this.builder.mpptForShadowEnable;
	}

	@Override
	public EnableDisable backupEnable() {
		return this.builder.backupEnable;
	}

	@Deprecated
	@Override
	public EnableDisable feedPowerEnable() {
		return this.builder.feedPowerEnable;
	}

	@Deprecated
	@Override
	public int feedPowerPara() {
		return this.builder.feedPowerPara;
	}

	@Override
	public FeedInPowerSettings setfeedInPowerSettings() {
		return this.builder.feedInPowerSettings;
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.controlMode;
	}

	@Override
	public EnableDisable rcrEnable() {
		return this.builder.rcrEnable;
	}

	@Override
	public StartStopConfig startStop() {
		return this.builder.startStop;
	}

	@Override
	public EnableDisable naProtectionEnable() {
		return this.builder.naProtectionEnable;
	}

	@Override
	public GridCode gridCode() {
		return this.builder.gridCode;
	}

	// APM Settings
	@Override
	public int settingApmOutputActivePower() {
		return this.builder.settingApmOutputActivePower;
	}

	@Override
	public int settingApmPowerGradient() {
		return this.builder.settingApmPowerGradient;
	}

	@Override
	public EnableDisableOrUndefined settingApmPfOverFrequencyCurveEnable() {
		return this.builder.settingApmPfOverFrequencyCurveEnable;
	}

	@Override
	public int settingApmPfOverFrequencyStartPoint() {
		return this.builder.settingApmPfOverFrequencyStartPoint;
	}

	@Override
	public int settingApmPfOverFrequencySlope() {
		return this.builder.settingApmPfOverFrequencySlope;
	}

	@Override
	public int settingApmPfOverFrequencyDelayTime() {
		return this.builder.settingApmPfOverFrequencyDelayTime;
	}

	@Override
	public EnableDisableOrUndefined settingApmPfOverFrequencyDeactivationThresholdFstop() {
		return this.builder.settingApmPfOverFrequencyDeactivationThresholdFstop;
	}

	@Override
	public int settingApmPfOverFrequencyHysteresisPoint() {
		return this.builder.settingApmPfOverFrequencyHysteresisPoint;
	}

	@Override
	public int settingApmPOverFrequencyDelayWaitingTime() {
		return this.builder.settingApmPOverFrequencyDelayWaitingTime;
	}

	@Override
	public int settingApmPfOverFrequencyHysteresisSlope() {
		return this.builder.settingApmPfOverFrequencyHysteresisSlope;
	}

	@Override
	public EnableDisableOrUndefined settingApmPfUnderFrequencyCurveEnable() {
		return this.builder.settingApmPfUnderFrequencyCurveEnable;
	}

	@Override
	public int settingApmPfUnderFrequencyThreshold() {
		return this.builder.settingApmPfUnderFrequencyThreshold;
	}

	@Override
	public int settingApmPfUnderFrequencySlope() {
		return this.builder.settingApmPfUnderFrequencySlope;
	}

	@Override
	public int settingApmPfUnderFrequencyDelayTime() {
		return this.builder.settingApmPfUnderFrequencyDelayTime;
	}

	@Override
	public EnableDisableOrUndefined settingApmPfUnderFrequencyDeactivationThresholdFstop() {
		return this.builder.settingApmPfUnderFrequencyDeactivationThresholdFstop;
	}

	@Override
	public int settingApmPfUnderFrequencyHysteresisPoint() {
		return this.builder.settingApmPfUnderFrequencyHysteresisPoint;
	}

	@Override
	public int settingApmPUnderFrequencyDelayWaitingTime() {
		return this.builder.settingApmPUnderFrequencyDelayWaitingTime;
	}

	@Override
	public int settingApmPfUnderFrequencyHysteresisSlope() {
		return this.builder.settingApmPfUnderFrequencyHysteresisSlope;
	}

	// PU Curve Settings
	@Override
	public EnableDisableOrUndefined puCurveEnable() {
		return this.builder.puCurveEnable;
	}

	@Override
	public int settingApmPuV1Voltage() {
		return this.builder.settingApmPuV1Voltage;
	}

	@Override
	public int settingApmPuV1ActivePower() {
		return this.builder.settingApmPuV1ActivePower;
	}

	@Override
	public int settingApmPuV2Voltage() {
		return this.builder.settingApmPuV2Voltage;
	}

	@Override
	public int settingApmPuV2ActivePower() {
		return this.builder.settingApmPuV2ActivePower;
	}

	@Override
	public int settingApmPuV3Voltage() {
		return this.builder.settingApmPuV3Voltage;
	}

	@Override
	public int settingApmPuV3ActivePower() {
		return this.builder.settingApmPuV3ActivePower;
	}

	@Override
	public int settingApmPuV4Voltage() {
		return this.builder.settingApmPuV4Voltage;
	}

	@Override
	public int settingApmPuV4ActivePower() {
		return this.builder.settingApmPuV4ActivePower;
	}

	@Override
	public SafetyParameterEnums.Vrt.GeneralRecoveryMode settingApmPuResponseMode() {
		return this.builder.settingApmPuResponseMode;
	}

	@Override
	public int settingApmPuPt1LowPassFilterTimeConstantPt1Mode() {
		return this.builder.settingApmPuPt1LowPassFilterTimeConstantPt1Mode;
	}

	@Override
	public int settingApmPuPt1LowPassFilterTimeConstantGradientMode() {
		return this.builder.settingApmPuPt1LowPassFilterTimeConstantGradientMode;
	}

	// RPM Settings
	@Override
	public SafetyParameterEnums.Rpm.Mode settingRpmQuCurveMode() {
		return this.builder.settingRpmQuCurveMode;
	}

	@Override
	public int settingRpmQuVoltageDeadBand() {
		return this.builder.settingRpmQuVoltageDeadBand;
	}

	@Override
	public int settingRpmQuOverexcitedSlope() {
		return this.builder.settingRpmQuOverexcitedSlope;
	}

	@Override
	public int settingRpmQuUnderexcitedSlope() {
		return this.builder.settingRpmQuUnderexcitedSlope;
	}

	@Override
	public ReactivePowerMode settingRpmMode() {
		return this.builder.settingRpmMode;
	}

	@Override
	public FixPfSetting settingRpmFixPf() {
		return this.builder.settingRpmFixPf;
	}

	@Override
	public int settingRpmFixQ() {
		return this.builder.settingRpmFixQ;
	}

	@Override
	public int settingRpmQuV1Voltage() {
		return this.builder.settingRpmQuV1Voltage;
	}

	@Override
	public int settingRpmQuV1ReactivePower() {
		return this.builder.settingRpmQuV1ReactivePower;
	}

	@Override
	public int settingRpmQuV2Voltage() {
		return this.builder.settingRpmQuV2Voltage;
	}

	@Override
	public int settingRpmQuV2ReactivePower() {
		return this.builder.settingRpmQuV2ReactivePower;
	}

	@Override
	public int settingRpmQuV3Voltage() {
		return this.builder.settingRpmQuV3Voltage;
	}

	@Override
	public int settingRpmQuV3ReactivePower() {
		return this.builder.settingRpmQuV3ReactivePower;
	}

	@Override
	public int settingRpmQuV4Voltage() {
		return this.builder.settingRpmQuV4Voltage;
	}

	@Override
	public int settingRpmQuV4ReactivePower() {
		return this.builder.settingRpmQuV4ReactivePower;
	}

	@Override
	public int settingRpmQuTimeConstant() {
		return this.builder.settingRpmQuTimeConstant;
	}

	@Override
	public EnableDisableOrUndefined settingRpmQuExtendedFunctions() {
		return this.builder.settingRpmQuExtendedFunctions;
	}

	@Override
	public int settingRpmQuLockInPower() {
		return this.builder.settingRpmQuLockInPower;
	}

	@Override
	public int settingRpmQuLockOutPower() {
		return this.builder.settingRpmQuLockOutPower;
	}

	// RPM CosPhi Settings
	@Override
	public SafetyParameterEnums.Rpm.Mode settingRpmCosPhiPCurveMode() {
		return this.builder.settingRpmCosPhiPCurveMode;
	}

	@Override
	public int settingRpmCosPhiPOverexcitedSlope() {
		return this.builder.settingRpmCosPhiPOverexcitedSlope;
	}

	@Override
	public int settingRpmCosPhiPUnderexcitedSlope() {
		return this.builder.settingRpmCosPhiPUnderexcitedSlope;
	}

	@Override
	public int settingRpmCosPhipPowerA() {
		return this.builder.settingRpmCosPhipPowerA;
	}

	@Override
	public int settingRpmCosPhipCosPhiA() {
		return this.builder.settingRpmCosPhipCosPhiA;
	}

	@Override
	public int settingRpmCosPhipPowerB() {
		return this.builder.settingRpmCosPhipPowerB;
	}

	@Override
	public int settingRpmCosPhipCosPhiB() {
		return this.builder.settingRpmCosPhipCosPhiB;
	}

	@Override
	public int settingRpmCosPhipPowerC() {
		return this.builder.settingRpmCosPhipPowerC;
	}

	@Override
	public int settingRpmCosPhipCosPhiC() {
		return this.builder.settingRpmCosPhipCosPhiC;
	}

	@Override
	public int settingRpmCosPhipPowerD() {
		return this.builder.settingRpmCosPhipPowerD;
	}

	@Override
	public int settingRpmCosPhipCosPhiD() {
		return this.builder.settingRpmCosPhipCosPhiD;
	}

	@Override
	public int settingRpmCosPhipPowerE() {
		return this.builder.settingRpmCosPhipPowerE;
	}

	@Override
	public int settingRpmCosPhipCosPhiE() {
		return this.builder.settingRpmCosPhipCosPhiE;
	}

	@Override
	public int settingRpmCosPhipTimeConstant() {
		return this.builder.settingRpmCosPhipTimeConstant;
	}

	@Override
	public EnableDisableOrUndefined settingRpmCosPhipExtendedFunctions() {
		return this.builder.settingRpmCosPhipExtendedFunctions;
	}

	@Override
	public int settingRpmCosPhipLockInVolt() {
		return this.builder.settingRpmCosPhipLockInVolt;
	}

	@Override
	public int settingRpmCosPhipLockOutVolt() {
		return this.builder.settingRpmCosPhipLockOutVolt;
	}

	// QP Settings
	@Override
	public SafetyParameterEnums.Rpm.Mode settingQpCurveMode() {
		return this.builder.settingQpCurveMode;
	}

	@Override
	public int settingQpOverexcitedSlope() {
		return this.builder.settingQpOverexcitedSlope;
	}

	@Override
	public int settingQpUnderexcitedSlope() {
		return this.builder.settingQpUnderexcitedSlope;
	}

	@Override
	public int settingRpmQpPowerP1() {
		return this.builder.settingRpmQpPowerP1;
	}

	@Override
	public int settingRpmQpReactivePowerP1() {
		return this.builder.settingRpmQpReactivePowerP1;
	}

	@Override
	public int settingRpmQpPowerP2() {
		return this.builder.settingRpmQpPowerP2;
	}

	@Override
	public int settingRpmQpReactivePowerP2() {
		return this.builder.settingRpmQpReactivePowerP2;
	}

	@Override
	public int settingRpmQpPowerP3() {
		return this.builder.settingRpmQpPowerP3;
	}

	@Override
	public int settingRpmQpReactivePowerP3() {
		return this.builder.settingRpmQpReactivePowerP3;
	}

	@Override
	public int settingRpmQpPowerP4() {
		return this.builder.settingRpmQpPowerP4;
	}

	@Override
	public int settingRpmQpReactivePowerP4() {
		return this.builder.settingRpmQpReactivePowerP4;
	}

	@Override
	public int settingRpmQpPowerP5() {
		return this.builder.settingRpmQpPowerP5;
	}

	@Override
	public int settingRpmQpReactivePowerP5() {
		return this.builder.settingRpmQpReactivePowerP5;
	}

	@Override
	public int settingRpmQpPowerP6() {
		return this.builder.settingRpmQpPowerP6;
	}

	@Override
	public int settingRpmQpReactivePowerP6() {
		return this.builder.settingRpmQpReactivePowerP6;
	}

	@Override
	public int settingRpmQpTimeConstant() {
		return this.builder.settingRpmQpTimeConstant;
	}

	// VPP Settings
	@Override
	public int settingVppOvStage1TriggerValue() {
		return this.builder.settingVppOvStage1TriggerValue;
	}

	@Override
	public long settingVppOvStage1TripTime() {
		return this.builder.settingVppOvStage1TripTime;
	}

	@Override
	public int settingVppUvStage1TripValue() {
		return this.builder.settingVppUvStage1TripValue;
	}

	@Override
	public long settingVppUvStage1TripTime() {
		return this.builder.settingVppUvStage1TripTime;
	}

	@Override
	public int settingVppOvStage2TriggerValue() {
		return this.builder.settingVppOvStage2TriggerValue;
	}

	@Override
	public long settingVppOvStage2TripTime() {
		return this.builder.settingVppOvStage2TripTime;
	}

	@Override
	public int settingVppUvStage2TripValue() {
		return this.builder.settingVppUvStage2TripValue;
	}

	@Override
	public long settingVppUvStage2TripTime() {
		return this.builder.settingVppUvStage2TripTime;
	}

	@Override
	public int settingVppOvStage3TriggerValue() {
		return this.builder.settingVppOvStage3TriggerValue;
	}

	@Override
	public long settingVppOvStage3TripTime() {
		return this.builder.settingVppOvStage3TripTime;
	}

	@Override
	public int settingVppUvStage3TripValue() {
		return this.builder.settingVppUvStage3TripValue;
	}

	@Override
	public long settingVppUvStage3TripTime() {
		return this.builder.settingVppUvStage3TripTime;
	}

	@Override
	public int settingVppOvStage4TriggerValue() {
		return this.builder.settingVppOvStage4TriggerValue;
	}

	@Override
	public long settingVppOvStage4TripTime() {
		return this.builder.settingVppOvStage4TripTime;
	}

	@Override
	public int settingVppUvStage4TripValue() {
		return this.builder.settingVppUvStage4TripValue;
	}

	@Override
	public long settingVppUvStage4TripTime() {
		return this.builder.settingVppUvStage4TripTime;
	}

	@Override
	public int settingVpp10MinOvTripThreshold() {
		return this.builder.settingVpp10MinOvTripThreshold;
	}

	@Override
	public long settingVpp10MinOvTripTime() {
		return this.builder.settingVpp10MinOvTripTime;
	}

	// FPP Settings
	@Override
	public int settingFppOfStage1TriggerValue() {
		return this.builder.settingFppOfStage1TriggerValue;
	}

	@Override
	public long settingFppOfStage1TripTime() {
		return this.builder.settingFppOfStage1TripTime;
	}

	@Override
	public int settingFppUfStage1TripValue() {
		return this.builder.settingFppUfStage1TripValue;
	}

	@Override
	public long settingFppUfStage1TripTime() {
		return this.builder.settingFppUfStage1TripTime;
	}

	@Override
	public int settingFppOfStage2TriggerValue() {
		return this.builder.settingFppOfStage2TriggerValue;
	}

	@Override
	public long settingFppOfStage2TripTime() {
		return this.builder.settingFppOfStage2TripTime;
	}

	@Override
	public int settingFppUfStage2TripValue() {
		return this.builder.settingFppUfStage2TripValue;
	}

	@Override
	public long settingFppUfStage2TripTime() {
		return this.builder.settingFppUfStage2TripTime;
	}

	@Override
	public int settingFppOfStage3TriggerValue() {
		return this.builder.settingFppOfStage3TriggerValue;
	}

	@Override
	public long settingFppOfStage3TripTime() {
		return this.builder.settingFppOfStage3TripTime;
	}

	@Override
	public int settingFppUfStage3TripValue() {
		return this.builder.settingFppUfStage3TripValue;
	}

	@Override
	public long settingFppUfStage3TripTime() {
		return this.builder.settingFppUfStage3TripTime;
	}

	@Override
	public int settingFppOfStage4TriggerValue() {
		return this.builder.settingFppOfStage4TriggerValue;
	}

	@Override
	public long settingFppOfStage4TripTime() {
		return this.builder.settingFppOfStage4TripTime;
	}

	@Override
	public int settingFppUfStage4TripValue() {
		return this.builder.settingFppUfStage4TripValue;
	}

	@Override
	public long settingFppUfStage4TripTime() {
		return this.builder.settingFppUfStage4TripTime;
	}

	// CP Settings
	@Override
	public int settingCpRampUpUpperVoltage() {
		return this.builder.settingCpRampUpUpperVoltage;
	}

	@Override
	public int settingCpRampUpLowerVoltage() {
		return this.builder.settingCpRampUpLowerVoltage;
	}

	@Override
	public int settingCpRampUpUpperFrequency() {
		return this.builder.settingCpRampUpUpperFrequency;
	}

	@Override
	public int settingCpRampUpLowerFrequency() {
		return this.builder.settingCpRampUpLowerFrequency;
	}

	@Override
	public int settingCpRampUpObservationTime() {
		return this.builder.settingCpRampUpObservationTime;
	}

	@Override
	public EnableDisableOrUndefined settingCpSoftRampUpGradientEnable() {
		return this.builder.settingCpSoftRampUpGradientEnable;
	}

	@Override
	public int settingCpSoftRampUpGradient() {
		return this.builder.settingCpSoftRampUpGradient;
	}

	@Override
	public int settingCpReconnectionUpperVoltage() {
		return this.builder.settingCpReconnectionUpperVoltage;
	}

	@Override
	public int settingCpReconnectionLowerVoltage() {
		return this.builder.settingCpReconnectionLowerVoltage;
	}

	@Override
	public int settingCpReconnectionUpperFrequency() {
		return this.builder.settingCpReconnectionUpperFrequency;
	}

	@Override
	public int settingCpReconnectionLowerFrequency() {
		return this.builder.settingCpReconnectionLowerFrequency;
	}

	@Override
	public int settingCpReconnectionObservationTime() {
		return this.builder.settingCpReconnectionObservationTime;
	}

	@Override
	public EnableDisableOrUndefined settingCpReconnectionGradientEnable() {
		return this.builder.settingCpReconnectionGradientEnable;
	}

	@Override
	public int settingCpReconnectionGradient() {
		return this.builder.settingCpReconnectionGradient;
	}

	// LVRT Settings
	@Override
	public EnableDisableOrUndefined settingLvrtEnable() {
		return this.builder.settingLvrtEnable;
	}

	@Override
	public int settingLvrtUv1Voltage() {
		return this.builder.settingLvrtUv1Voltage;
	}

	@Override
	public int settingLvrtUv1Time() {
		return this.builder.settingLvrtUv1Time;
	}

	@Override
	public int settingLvrtUv2Voltage() {
		return this.builder.settingLvrtUv2Voltage;
	}

	@Override
	public int settingLvrtUv2Time() {
		return this.builder.settingLvrtUv2Time;
	}

	@Override
	public int settingLvrtUv3Voltage() {
		return this.builder.settingLvrtUv3Voltage;
	}

	@Override
	public int settingLvrtUv3Time() {
		return this.builder.settingLvrtUv3Time;
	}

	@Override
	public int settingLvrtUv4Voltage() {
		return this.builder.settingLvrtUv4Voltage;
	}

	@Override
	public int settingLvrtUv4Time() {
		return this.builder.settingLvrtUv4Time;
	}

	@Override
	public int settingLvrtUv5Voltage() {
		return this.builder.settingLvrtUv5Voltage;
	}

	@Override
	public int settingLvrtUv5Time() {
		return this.builder.settingLvrtUv5Time;
	}

	@Override
	public int settingLvrtUv6Voltage() {
		return this.builder.settingLvrtUv6Voltage;
	}

	@Override
	public int settingLvrtUv6Time() {
		return this.builder.settingLvrtUv6Time;
	}

	@Override
	public int settingLvrtUv7Voltage() {
		return this.builder.settingLvrtUv7Voltage;
	}

	@Override
	public int settingLvrtUv7Time() {
		return this.builder.settingLvrtUv7Time;
	}

	@Override
	public int settingLvrtEnterThreshold() {
		return this.builder.settingLvrtEnterThreshold;
	}

	@Override
	public int settingLvrtExitEndpoint() {
		return this.builder.settingLvrtExitEndpoint;
	}

	@Override
	public int settingLvrtK1Slope() {
		return this.builder.settingLvrtK1Slope;
	}

	@Override
	public EnableDisableOrUndefined settingLvrtZeroCurrentModeEnable() {
		return this.builder.settingLvrtZeroCurrentModeEnable;
	}

	@Override
	public int settingLvrtZeroCurrentModeEntryThreshold() {
		return this.builder.settingLvrtZeroCurrentModeEntryThreshold;
	}

	// HVRT Settings
	@Override
	public EnableDisableOrUndefined settingHvrtEnable() {
		return this.builder.settingHvrtEnable;
	}

	@Override
	public int settingHvrtOv1Voltage() {
		return this.builder.settingHvrtOv1Voltage;
	}

	@Override
	public int settingHvrtOv1Time() {
		return this.builder.settingHvrtOv1Time;
	}

	@Override
	public int settingHvrtOv2Voltage() {
		return this.builder.settingHvrtOv2Voltage;
	}

	@Override
	public int settingHvrtOv2Time() {
		return this.builder.settingHvrtOv2Time;
	}

	@Override
	public int settingHvrtOv3Voltage() {
		return this.builder.settingHvrtOv3Voltage;
	}

	@Override
	public int settingHvrtOv3Time() {
		return this.builder.settingHvrtOv3Time;
	}

	@Override
	public int settingHvrtOv4Voltage() {
		return this.builder.settingHvrtOv4Voltage;
	}

	@Override
	public int settingHvrtOv4Time() {
		return this.builder.settingHvrtOv4Time;
	}

	@Override
	public int settingHvrtOv5Voltage() {
		return this.builder.settingHvrtOv5Voltage;
	}

	@Override
	public int settingHvrtOv5Time() {
		return this.builder.settingHvrtOv5Time;
	}

	@Override
	public int settingHvrtOv6Voltage() {
		return this.builder.settingHvrtOv6Voltage;
	}

	@Override
	public int settingHvrtOv6Time() {
		return this.builder.settingHvrtOv6Time;
	}

	@Override
	public int settingHvrtOv7Voltage() {
		return this.builder.settingHvrtOv7Voltage;
	}

	@Override
	public int settingHvrtOv7Time() {
		return this.builder.settingHvrtOv7Time;
	}

	@Override
	public int settingHvrtEnterHighCrossingThreshold() {
		return this.builder.settingHvrtEnterHighCrossingThreshold;
	}

	@Override
	public int settingHvrtExitHighCrossingThreshold() {
		return this.builder.settingHvrtExitHighCrossingThreshold;
	}

	@Override
	public int settingHvrtK2Slope() {
		return this.builder.settingHvrtK2Slope;
	}

	@Override
	public EnableDisableOrUndefined settingHvrtZeroCurrentModeEnable() {
		return this.builder.settingHvrtZeroCurrentModeEnable;
	}

	@Override
	public int settingHvrtZeroCurrentModeEntryThreshold() {
		return this.builder.settingHvrtZeroCurrentModeEntryThreshold;
	}

	// VRT Settings
	@Override
	public SafetyParameterEnums.Vrt.CurrentDistributionMode settingVrtCurrentDistributionMode() {
		return this.builder.settingVrtCurrentDistributionMode;
	}

	@Override
	public SafetyParameterEnums.Vrt.GeneralRecoveryMode settingVrtActivePowerRecoveryMode() {
		return this.builder.settingVrtActivePowerRecoveryMode;
	}

	@Override
	public int settingVrtActivePowerRecoverySpeed() {
		return this.builder.settingVrtActivePowerRecoverySpeed;
	}

	@Override
	public long settingVrtActivePowerRecoverySlope() {
		return this.builder.settingVrtActivePowerRecoverySlope;
	}

	@Override
	public SafetyParameterEnums.Vrt.GeneralRecoveryMode settingVrtReactivePowerRecoveryModeEnd() {
		return this.builder.settingVrtReactivePowerRecoveryModeEnd;
	}

	@Override
	public int settingVrtReactivePowerRecoverySpeed() {
		return this.builder.settingVrtReactivePowerRecoverySpeed;
	}

	@Override
	public long settingVrtReactivePowerRecoverySlope() {
		return this.builder.settingVrtReactivePowerRecoverySlope;
	}

	// FRT Settings
	@Override
	public EnableDisableOrUndefined settingFrtEnable() {
		return this.builder.settingFrtEnable;
	}

	@Override
	public int settingFrtUf1Frequency() {
		return this.builder.settingFrtUf1Frequency;
	}

	@Override
	public int settingFrtUf1Time() {
		return this.builder.settingFrtUf1Time;
	}

	@Override
	public int settingFrtUf2Frequency() {
		return this.builder.settingFrtUf2Frequency;
	}

	@Override
	public int settingFrtUf2Time() {
		return this.builder.settingFrtUf2Time;
	}

	@Override
	public int settingFrtUf3Frequency() {
		return this.builder.settingFrtUf3Frequency;
	}

	@Override
	public int settingFrtUf3Time() {
		return this.builder.settingFrtUf3Time;
	}

	@Override
	public int settingFrtOf1Frequency() {
		return this.builder.settingFrtOf1Frequency;
	}

	@Override
	public int settingFrtOf1Time() {
		return this.builder.settingFrtOf1Time;
	}

	@Override
	public int settingFrtOf2Frequency() {
		return this.builder.settingFrtOf2Frequency;
	}

	@Override
	public int settingFrtOf2Time() {
		return this.builder.settingFrtOf2Time;
	}

	@Override
	public int settingFrtOf3Frequency() {
		return this.builder.settingFrtOf3Frequency;
	}

	@Override
	public int settingFrtOf3Time() {
		return this.builder.settingFrtOf3Time;
	}
}