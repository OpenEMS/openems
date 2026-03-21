package io.openems.edge.goodwe.batteryinverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.EnableDisableOrUndefined;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.FixPfSetting;
import io.openems.edge.goodwe.common.enums.ReactivePowerMode;
import io.openems.edge.goodwe.common.enums.SafetyCountry;
import io.openems.edge.goodwe.common.enums.SafetyParameterEnums;

public class ExtendedPropertiesTest {

	@Test
	public void testApmSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test1") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingApmOutputActivePower(5000) //
				.setSettingApmPowerGradient(100) //
				.setSettingApmPfOverFrequencyCurveEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingApmPfOverFrequencyStartPoint(5010) //
				.setSettingApmPfOverFrequencySlope(20) //
				.setSettingApmPfOverFrequencyDelayTime(30) //
				.setSettingApmPfOverFrequencyDeactivationThresholdFstop(EnableDisableOrUndefined.DISABLE) //
				.setSettingApmPfOverFrequencyHysteresisPoint(40) //
				.setSettingApmPOverFrequencyDelayWaitingTime(50) //
				.setSettingApmPfOverFrequencyHysteresisSlope(60) //
				.setSettingApmPfUnderFrequencyCurveEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingApmPfUnderFrequencyThreshold(4990) //
				.setSettingApmPfUnderFrequencySlope(25) //
				.setSettingApmPfUnderFrequencyDelayTime(35) //
				.setSettingApmPfUnderFrequencyDeactivationThresholdFstop(EnableDisableOrUndefined.DISABLE) //
				.setSettingApmPfUnderFrequencyHysteresisPoint(45) //
				.setSettingApmPUnderFrequencyDelayWaitingTime(55) //
				.setSettingApmPfUnderFrequencyHysteresisSlope(65) //
				.build();

		assertEquals(5000, config.settingApmOutputActivePower());
		assertEquals(100, config.settingApmPowerGradient());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingApmPfOverFrequencyCurveEnable());
		assertEquals(5010, config.settingApmPfOverFrequencyStartPoint());
		assertEquals(20, config.settingApmPfOverFrequencySlope());
		assertEquals(30, config.settingApmPfOverFrequencyDelayTime());
		assertEquals(EnableDisableOrUndefined.DISABLE, config.settingApmPfOverFrequencyDeactivationThresholdFstop());
		assertEquals(40, config.settingApmPfOverFrequencyHysteresisPoint());
		assertEquals(50, config.settingApmPOverFrequencyDelayWaitingTime());
		assertEquals(60, config.settingApmPfOverFrequencyHysteresisSlope());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingApmPfUnderFrequencyCurveEnable());
		assertEquals(4990, config.settingApmPfUnderFrequencyThreshold());
		assertEquals(25, config.settingApmPfUnderFrequencySlope());
		assertEquals(35, config.settingApmPfUnderFrequencyDelayTime());
		assertEquals(EnableDisableOrUndefined.DISABLE, config.settingApmPfUnderFrequencyDeactivationThresholdFstop());
		assertEquals(45, config.settingApmPfUnderFrequencyHysteresisPoint());
		assertEquals(55, config.settingApmPUnderFrequencyDelayWaitingTime());
		assertEquals(65, config.settingApmPfUnderFrequencyHysteresisSlope());
	}

	@Test
	public void testPuCurveSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test2") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setPuCurveEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingApmPuV1Voltage(200) //
				.setSettingApmPuV1ActivePower(1000) //
				.setSettingApmPuV2Voltage(220) //
				.setSettingApmPuV2ActivePower(2000) //
				.setSettingApmPuV3Voltage(230) //
				.setSettingApmPuV3ActivePower(3000) //
				.setSettingApmPuV4Voltage(240) //
				.setSettingApmPuV4ActivePower(4000) //
				.setSettingApmPuResponseMode(SafetyParameterEnums.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR) //
				.setSettingApmPuPt1LowPassFilterTimeConstantPt1Mode(10) //
				.setSettingApmPuPt1LowPassFilterTimeConstantGradientMode(20) //
				.build();

		assertEquals(EnableDisableOrUndefined.ENABLE, config.puCurveEnable());
		assertEquals(200, config.settingApmPuV1Voltage());
		assertEquals(1000, config.settingApmPuV1ActivePower());
		assertEquals(220, config.settingApmPuV2Voltage());
		assertEquals(2000, config.settingApmPuV2ActivePower());
		assertEquals(230, config.settingApmPuV3Voltage());
		assertEquals(3000, config.settingApmPuV3ActivePower());
		assertEquals(240, config.settingApmPuV4Voltage());
		assertEquals(4000, config.settingApmPuV4ActivePower());
		assertEquals(SafetyParameterEnums.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR, config.settingApmPuResponseMode());
		assertEquals(10, config.settingApmPuPt1LowPassFilterTimeConstantPt1Mode());
		assertEquals(20, config.settingApmPuPt1LowPassFilterTimeConstantGradientMode());
	}

	@Test
	public void testRpmSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test3") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingRpmQuCurveMode(SafetyParameterEnums.Rpm.Mode.BASIC) //
				.setSettingRpmQuVoltageDeadBand(5) //
				.setSettingRpmQuOverexcitedSlope(10) //
				.setSettingRpmQuUnderexcitedSlope(15) //
				.setSettingRpmMode(ReactivePowerMode.UNSELECTED) //
				.setSettingRpmFixPf(FixPfSetting.LEADING_0_95) //
				.setSettingRpmFixQ(500) //
				.setSettingRpmQuV1Voltage(210) //
				.setSettingRpmQuV1ReactivePower(100) //
				.setSettingRpmQuV2Voltage(220) //
				.setSettingRpmQuV2ReactivePower(200) //
				.setSettingRpmQuV3Voltage(230) //
				.setSettingRpmQuV3ReactivePower(300) //
				.setSettingRpmQuV4Voltage(240) //
				.setSettingRpmQuV4ReactivePower(400) //
				.setSettingRpmQuTimeConstant(25) //
				.setSettingRpmQuExtendedFunctions(EnableDisableOrUndefined.ENABLE) //
				.setSettingRpmQuLockInPower(1000) //
				.setSettingRpmQuLockOutPower(500) //
				.build();

		assertEquals(SafetyParameterEnums.Rpm.Mode.BASIC, config.settingRpmQuCurveMode());
		assertEquals(5, config.settingRpmQuVoltageDeadBand());
		assertEquals(10, config.settingRpmQuOverexcitedSlope());
		assertEquals(15, config.settingRpmQuUnderexcitedSlope());
		assertEquals(ReactivePowerMode.UNSELECTED, config.settingRpmMode());
		assertEquals(FixPfSetting.LEADING_0_95, config.settingRpmFixPf());
		assertEquals(500, config.settingRpmFixQ());
		assertEquals(210, config.settingRpmQuV1Voltage());
		assertEquals(100, config.settingRpmQuV1ReactivePower());
		assertEquals(220, config.settingRpmQuV2Voltage());
		assertEquals(200, config.settingRpmQuV2ReactivePower());
		assertEquals(230, config.settingRpmQuV3Voltage());
		assertEquals(300, config.settingRpmQuV3ReactivePower());
		assertEquals(240, config.settingRpmQuV4Voltage());
		assertEquals(400, config.settingRpmQuV4ReactivePower());
		assertEquals(25, config.settingRpmQuTimeConstant());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingRpmQuExtendedFunctions());
		assertEquals(1000, config.settingRpmQuLockInPower());
		assertEquals(500, config.settingRpmQuLockOutPower());
	}

	@Test
	public void testRpmCosPhiSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test4") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingRpmCosPhiPCurveMode(SafetyParameterEnums.Rpm.Mode.BASIC) //
				.setSettingRpmCosPhiPOverexcitedSlope(12) //
				.setSettingRpmCosPhiPUnderexcitedSlope(18) //
				.setSettingRpmCosPhipPowerA(100) //
				.setSettingRpmCosPhipCosPhiA(95) //
				.setSettingRpmCosPhipPowerB(200) //
				.setSettingRpmCosPhipCosPhiB(96) //
				.setSettingRpmCosPhipPowerC(300) //
				.setSettingRpmCosPhipCosPhiC(97) //
				.setSettingRpmCosPhipPowerD(400) //
				.setSettingRpmCosPhipCosPhiD(98) //
				.setSettingRpmCosPhipPowerE(500) //
				.setSettingRpmCosPhipCosPhiE(99) //
				.setSettingRpmCosPhipTimeConstant(30) //
				.setSettingRpmCosPhipExtendedFunctions(EnableDisableOrUndefined.DISABLE) //
				.setSettingRpmCosPhipLockInVolt(230) //
				.setSettingRpmCosPhipLockOutVolt(210) //
				.build();

		assertEquals(SafetyParameterEnums.Rpm.Mode.BASIC, config.settingRpmCosPhiPCurveMode());
		assertEquals(12, config.settingRpmCosPhiPOverexcitedSlope());
		assertEquals(18, config.settingRpmCosPhiPUnderexcitedSlope());
		assertEquals(100, config.settingRpmCosPhipPowerA());
		assertEquals(95, config.settingRpmCosPhipCosPhiA());
		assertEquals(200, config.settingRpmCosPhipPowerB());
		assertEquals(96, config.settingRpmCosPhipCosPhiB());
		assertEquals(300, config.settingRpmCosPhipPowerC());
		assertEquals(97, config.settingRpmCosPhipCosPhiC());
		assertEquals(400, config.settingRpmCosPhipPowerD());
		assertEquals(98, config.settingRpmCosPhipCosPhiD());
		assertEquals(500, config.settingRpmCosPhipPowerE());
		assertEquals(99, config.settingRpmCosPhipCosPhiE());
		assertEquals(30, config.settingRpmCosPhipTimeConstant());
		assertEquals(EnableDisableOrUndefined.DISABLE, config.settingRpmCosPhipExtendedFunctions());
		assertEquals(230, config.settingRpmCosPhipLockInVolt());
		assertEquals(210, config.settingRpmCosPhipLockOutVolt());
	}

	@Test
	public void testQpSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test5") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingQpCurveMode(SafetyParameterEnums.Rpm.Mode.BASIC) //
				.setSettingQpOverexcitedSlope(8) //
				.setSettingQpUnderexcitedSlope(12) //
				.setSettingRpmQpPowerP1(1000) //
				.setSettingRpmQpReactivePowerP1(100) //
				.setSettingRpmQpPowerP2(2000) //
				.setSettingRpmQpReactivePowerP2(200) //
				.setSettingRpmQpPowerP3(3000) //
				.setSettingRpmQpReactivePowerP3(300) //
				.setSettingRpmQpPowerP4(4000) //
				.setSettingRpmQpReactivePowerP4(400) //
				.setSettingRpmQpPowerP5(5000) //
				.setSettingRpmQpReactivePowerP5(500) //
				.setSettingRpmQpPowerP6(6000) //
				.setSettingRpmQpReactivePowerP6(600) //
				.setSettingRpmQpTimeConstant(40) //
				.build();

		assertEquals(SafetyParameterEnums.Rpm.Mode.BASIC, config.settingQpCurveMode());
		assertEquals(8, config.settingQpOverexcitedSlope());
		assertEquals(12, config.settingQpUnderexcitedSlope());
		assertEquals(1000, config.settingRpmQpPowerP1());
		assertEquals(100, config.settingRpmQpReactivePowerP1());
		assertEquals(2000, config.settingRpmQpPowerP2());
		assertEquals(200, config.settingRpmQpReactivePowerP2());
		assertEquals(3000, config.settingRpmQpPowerP3());
		assertEquals(300, config.settingRpmQpReactivePowerP3());
		assertEquals(4000, config.settingRpmQpPowerP4());
		assertEquals(400, config.settingRpmQpReactivePowerP4());
		assertEquals(5000, config.settingRpmQpPowerP5());
		assertEquals(500, config.settingRpmQpReactivePowerP5());
		assertEquals(6000, config.settingRpmQpPowerP6());
		assertEquals(600, config.settingRpmQpReactivePowerP6());
		assertEquals(40, config.settingRpmQpTimeConstant());
	}

	@Test
	public void testVppSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test6") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingVppOvStage1TriggerValue(253) //
				.setSettingVppOvStage1TripTime(1000L) //
				.setSettingVppUvStage1TripValue(207) //
				.setSettingVppUvStage1TripTime(1500L) //
				.setSettingVppOvStage2TriggerValue(265) //
				.setSettingVppOvStage2TripTime(200L) //
				.setSettingVppUvStage2TripValue(184) //
				.setSettingVppUvStage2TripTime(200L) //
				.setSettingVppOvStage3TriggerValue(270) //
				.setSettingVppOvStage3TripTime(100L) //
				.setSettingVppUvStage3TripValue(110) //
				.setSettingVppUvStage3TripTime(1000L) //
				.setSettingVppOvStage4TriggerValue(280) //
				.setSettingVppOvStage4TripTime(50L) //
				.setSettingVppUvStage4TripValue(90) //
				.setSettingVppUvStage4TripTime(160L) //
				.setSettingVpp10MinOvTripThreshold(245) //
				.setSettingVpp10MinOvTripTime(60000L) //
				.build();

		assertEquals(253, config.settingVppOvStage1TriggerValue());
		assertEquals(1000L, config.settingVppOvStage1TripTime());
		assertEquals(207, config.settingVppUvStage1TripValue());
		assertEquals(1500L, config.settingVppUvStage1TripTime());
		assertEquals(265, config.settingVppOvStage2TriggerValue());
		assertEquals(200L, config.settingVppOvStage2TripTime());
		assertEquals(184, config.settingVppUvStage2TripValue());
		assertEquals(200L, config.settingVppUvStage2TripTime());
		assertEquals(270, config.settingVppOvStage3TriggerValue());
		assertEquals(100L, config.settingVppOvStage3TripTime());
		assertEquals(110, config.settingVppUvStage3TripValue());
		assertEquals(1000L, config.settingVppUvStage3TripTime());
		assertEquals(280, config.settingVppOvStage4TriggerValue());
		assertEquals(50L, config.settingVppOvStage4TripTime());
		assertEquals(90, config.settingVppUvStage4TripValue());
		assertEquals(160L, config.settingVppUvStage4TripTime());
		assertEquals(245, config.settingVpp10MinOvTripThreshold());
		assertEquals(60000L, config.settingVpp10MinOvTripTime());
	}

	@Test
	public void testFppSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test7") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingFppOfStage1TriggerValue(5020) //
				.setSettingFppOfStage1TripTime(1000L) //
				.setSettingFppUfStage1TripValue(4980) //
				.setSettingFppUfStage1TripTime(1000L) //
				.setSettingFppOfStage2TriggerValue(5150) //
				.setSettingFppOfStage2TripTime(200L) //
				.setSettingFppUfStage2TripValue(4750) //
				.setSettingFppUfStage2TripTime(200L) //
				.setSettingFppOfStage3TriggerValue(5200) //
				.setSettingFppOfStage3TripTime(100L) //
				.setSettingFppUfStage3TripValue(4700) //
				.setSettingFppUfStage3TripTime(100L) //
				.setSettingFppOfStage4TriggerValue(5250) //
				.setSettingFppOfStage4TripTime(16L) //
				.setSettingFppUfStage4TripValue(4650) //
				.setSettingFppUfStage4TripTime(16L) //
				.build();

		assertEquals(5020, config.settingFppOfStage1TriggerValue());
		assertEquals(1000L, config.settingFppOfStage1TripTime());
		assertEquals(4980, config.settingFppUfStage1TripValue());
		assertEquals(1000L, config.settingFppUfStage1TripTime());
		assertEquals(5150, config.settingFppOfStage2TriggerValue());
		assertEquals(200L, config.settingFppOfStage2TripTime());
		assertEquals(4750, config.settingFppUfStage2TripValue());
		assertEquals(200L, config.settingFppUfStage2TripTime());
		assertEquals(5200, config.settingFppOfStage3TriggerValue());
		assertEquals(100L, config.settingFppOfStage3TripTime());
		assertEquals(4700, config.settingFppUfStage3TripValue());
		assertEquals(100L, config.settingFppUfStage3TripTime());
		assertEquals(5250, config.settingFppOfStage4TriggerValue());
		assertEquals(16L, config.settingFppOfStage4TripTime());
		assertEquals(4650, config.settingFppUfStage4TripValue());
		assertEquals(16L, config.settingFppUfStage4TripTime());
	}

	@Test
	public void testCpSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test8") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingCpRampUpUpperVoltage(253) //
				.setSettingCpRampUpLowerVoltage(184) //
				.setSettingCpRampUpUpperFrequency(5020) //
				.setSettingCpRampUpLowerFrequency(4980) //
				.setSettingCpRampUpObservationTime(60) //
				.setSettingCpSoftRampUpGradientEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingCpSoftRampUpGradient(100) //
				.setSettingCpReconnectionUpperVoltage(253) //
				.setSettingCpReconnectionLowerVoltage(207) //
				.setSettingCpReconnectionUpperFrequency(5020) //
				.setSettingCpReconnectionLowerFrequency(4980) //
				.setSettingCpReconnectionObservationTime(60) //
				.setSettingCpReconnectionGradientEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingCpReconnectionGradient(100) //
				.build();

		assertEquals(253, config.settingCpRampUpUpperVoltage());
		assertEquals(184, config.settingCpRampUpLowerVoltage());
		assertEquals(5020, config.settingCpRampUpUpperFrequency());
		assertEquals(4980, config.settingCpRampUpLowerFrequency());
		assertEquals(60, config.settingCpRampUpObservationTime());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingCpSoftRampUpGradientEnable());
		assertEquals(100, config.settingCpSoftRampUpGradient());
		assertEquals(253, config.settingCpReconnectionUpperVoltage());
		assertEquals(207, config.settingCpReconnectionLowerVoltage());
		assertEquals(5020, config.settingCpReconnectionUpperFrequency());
		assertEquals(4980, config.settingCpReconnectionLowerFrequency());
		assertEquals(60, config.settingCpReconnectionObservationTime());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingCpReconnectionGradientEnable());
		assertEquals(100, config.settingCpReconnectionGradient());
	}

	@Test
	public void testLvrtSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test9") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingLvrtEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingLvrtUv1Voltage(200) //
				.setSettingLvrtUv1Time(1500) //
				.setSettingLvrtUv2Voltage(180) //
				.setSettingLvrtUv2Time(1000) //
				.setSettingLvrtUv3Voltage(160) //
				.setSettingLvrtUv3Time(700) //
				.setSettingLvrtUv4Voltage(140) //
				.setSettingLvrtUv4Time(500) //
				.setSettingLvrtUv5Voltage(120) //
				.setSettingLvrtUv5Time(300) //
				.setSettingLvrtUv6Voltage(100) //
				.setSettingLvrtUv6Time(200) //
				.setSettingLvrtUv7Voltage(50) //
				.setSettingLvrtUv7Time(150) //
				.setSettingLvrtEnterThreshold(190) //
				.setSettingLvrtExitEndpoint(210) //
				.setSettingLvrtK1Slope(2) //
				.setSettingLvrtZeroCurrentModeEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingLvrtZeroCurrentModeEntryThreshold(40) //
				.build();

		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingLvrtEnable());
		assertEquals(200, config.settingLvrtUv1Voltage());
		assertEquals(1500, config.settingLvrtUv1Time());
		assertEquals(180, config.settingLvrtUv2Voltage());
		assertEquals(1000, config.settingLvrtUv2Time());
		assertEquals(160, config.settingLvrtUv3Voltage());
		assertEquals(700, config.settingLvrtUv3Time());
		assertEquals(140, config.settingLvrtUv4Voltage());
		assertEquals(500, config.settingLvrtUv4Time());
		assertEquals(120, config.settingLvrtUv5Voltage());
		assertEquals(300, config.settingLvrtUv5Time());
		assertEquals(100, config.settingLvrtUv6Voltage());
		assertEquals(200, config.settingLvrtUv6Time());
		assertEquals(50, config.settingLvrtUv7Voltage());
		assertEquals(150, config.settingLvrtUv7Time());
		assertEquals(190, config.settingLvrtEnterThreshold());
		assertEquals(210, config.settingLvrtExitEndpoint());
		assertEquals(2, config.settingLvrtK1Slope());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingLvrtZeroCurrentModeEnable());
		assertEquals(40, config.settingLvrtZeroCurrentModeEntryThreshold());
	}

	@Test
	public void testHvrtSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test10") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingHvrtEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingHvrtOv1Voltage(240) //
				.setSettingHvrtOv1Time(1500) //
				.setSettingHvrtOv2Voltage(250) //
				.setSettingHvrtOv2Time(1000) //
				.setSettingHvrtOv3Voltage(260) //
				.setSettingHvrtOv3Time(700) //
				.setSettingHvrtOv4Voltage(270) //
				.setSettingHvrtOv4Time(500) //
				.setSettingHvrtOv5Voltage(280) //
				.setSettingHvrtOv5Time(300) //
				.setSettingHvrtOv6Voltage(290) //
				.setSettingHvrtOv6Time(200) //
				.setSettingHvrtOv7Voltage(300) //
				.setSettingHvrtOv7Time(150) //
				.setSettingHvrtEnterHighCrossingThreshold(245) //
				.setSettingHvrtExitHighCrossingThreshold(235) //
				.setSettingHvrtK2Slope(3) //
				.setSettingHvrtZeroCurrentModeEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingHvrtZeroCurrentModeEntryThreshold(295) //
				.build();

		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingHvrtEnable());
		assertEquals(240, config.settingHvrtOv1Voltage());
		assertEquals(1500, config.settingHvrtOv1Time());
		assertEquals(250, config.settingHvrtOv2Voltage());
		assertEquals(1000, config.settingHvrtOv2Time());
		assertEquals(260, config.settingHvrtOv3Voltage());
		assertEquals(700, config.settingHvrtOv3Time());
		assertEquals(270, config.settingHvrtOv4Voltage());
		assertEquals(500, config.settingHvrtOv4Time());
		assertEquals(280, config.settingHvrtOv5Voltage());
		assertEquals(300, config.settingHvrtOv5Time());
		assertEquals(290, config.settingHvrtOv6Voltage());
		assertEquals(200, config.settingHvrtOv6Time());
		assertEquals(300, config.settingHvrtOv7Voltage());
		assertEquals(150, config.settingHvrtOv7Time());
		assertEquals(245, config.settingHvrtEnterHighCrossingThreshold());
		assertEquals(235, config.settingHvrtExitHighCrossingThreshold());
		assertEquals(3, config.settingHvrtK2Slope());
		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingHvrtZeroCurrentModeEnable());
		assertEquals(295, config.settingHvrtZeroCurrentModeEntryThreshold());
	}

	@Test
	public void testVrtSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test11") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingVrtCurrentDistributionMode(
						SafetyParameterEnums.Vrt.CurrentDistributionMode.ACTIVE_POWER_PRIO) //
				.setSettingVrtActivePowerRecoveryMode(SafetyParameterEnums.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR) //
				.setSettingVrtActivePowerRecoverySpeed(50) //
				.setSettingVrtActivePowerRecoverySlope(1000L) //
				.setSettingVrtReactivePowerRecoveryModeEnd(
						SafetyParameterEnums.Vrt.GeneralRecoveryMode.GRADIENT_CONTROL) //
				.setSettingVrtReactivePowerRecoverySpeed(60) //
				.setSettingVrtReactivePowerRecoverySlope(1200L) //
				.build();

		assertEquals(SafetyParameterEnums.Vrt.CurrentDistributionMode.ACTIVE_POWER_PRIO,
				config.settingVrtCurrentDistributionMode());
		assertEquals(SafetyParameterEnums.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR,
				config.settingVrtActivePowerRecoveryMode());
		assertEquals(50, config.settingVrtActivePowerRecoverySpeed());
		assertEquals(1000L, config.settingVrtActivePowerRecoverySlope());
		assertEquals(SafetyParameterEnums.Vrt.GeneralRecoveryMode.GRADIENT_CONTROL,
				config.settingVrtReactivePowerRecoveryModeEnd());
		assertEquals(60, config.settingVrtReactivePowerRecoverySpeed());
		assertEquals(1200L, config.settingVrtReactivePowerRecoverySlope());
	}

	@Test
	public void testFrtSettings() {
		MyConfig config = MyConfig.create() //
				.setId("test12") //
				.setControlMode(ControlMode.REMOTE) //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.DISABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedPowerPara(1000) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setStartStop(StartStopConfig.AUTO) //
				.setSettingFrtEnable(EnableDisableOrUndefined.ENABLE) //
				.setSettingFrtUf1Frequency(4985) //
				.setSettingFrtUf1Time(1000) //
				.setSettingFrtUf2Frequency(4970) //
				.setSettingFrtUf2Time(500) //
				.setSettingFrtUf3Frequency(4750) //
				.setSettingFrtUf3Time(200) //
				.setSettingFrtOf1Frequency(5015) //
				.setSettingFrtOf1Time(1000) //
				.setSettingFrtOf2Frequency(5030) //
				.setSettingFrtOf2Time(500) //
				.setSettingFrtOf3Frequency(5200) //
				.setSettingFrtOf3Time(200) //
				.build();

		assertEquals(EnableDisableOrUndefined.ENABLE, config.settingFrtEnable());
		assertEquals(4985, config.settingFrtUf1Frequency());
		assertEquals(1000, config.settingFrtUf1Time());
		assertEquals(4970, config.settingFrtUf2Frequency());
		assertEquals(500, config.settingFrtUf2Time());
		assertEquals(4750, config.settingFrtUf3Frequency());
		assertEquals(200, config.settingFrtUf3Time());
		assertEquals(5015, config.settingFrtOf1Frequency());
		assertEquals(1000, config.settingFrtOf1Time());
		assertEquals(5030, config.settingFrtOf2Frequency());
		assertEquals(500, config.settingFrtOf2Time());
		assertEquals(5200, config.settingFrtOf3Frequency());
		assertEquals(200, config.settingFrtOf3Time());
	}
}