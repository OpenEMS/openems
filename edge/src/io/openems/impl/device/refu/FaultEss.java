package io.openems.impl.device.refu;

import io.openems.api.channel.thingstate.FaultEnum;
import io.openems.common.types.ThingStateInfo;

@ThingStateInfo(reference = RefuEss.class)
public enum FaultEss implements FaultEnum {

	BMSInError(0), //
	BMSInErrorSecond(1), //
	BMSUndervoltage(2), //
	BMSOvercurrent(3), //
	ErrorBMSLimitsNotInitialized(4), //
	ConnectError(5), //
	OvervoltageWarning(6), //
	UndervoltageWarning(7), //
	OvercurrentWarning(8), //
	BMSReady(9), //
	TREXReady(10), //
	NoEnableBateryGroupOrUsableBatteryGroup(11), //
	NormalLeakageOfBatteryGroup(12), //
	SeriousLeakageOfBatteryGroup(13), //
	BatteryStartFailure(14), //
	BatteryStopFailure(15), //
	InterruptionOfCANCommunication(16), //
	InterruptionOfCANCommunicationBetweenBatteryGroupAndController(17), //
	EmergencyStopAbnormalOfAuxiliaryCollector(18), //
	LeakageSelfDetectionOnNegative(19), //
	LeakageSelfDetectionOnPositive(20), //
	SelfDetectionFailureOnBattery(21), //
	CANCommunicationInterruptionBetweenBatteryGroupAndGroup1(22), //
	CANCommunicationInterruptionBetweenBatteryGroupAndGroup2(23), //
	CANCommunicationInterruptionBetweenBatteryGroupAndGroup3(24), //
	CANCommunicationInterruptionBetweenBatteryGroupAndGroup4(25), //
	MainContractorAbnormalInBatterySelfDetectGroup1(26), //
	MainContractorAbnormalInBatterySelfDetectGroup2(27), //
	MainContractorAbnormalInBatterySelfDetectGroup3(28), //
	MainContractorAbnormalInBatterySelfDetectGroup4(29), //
	PreChargeContractorAbnormalOnBatterySelfDetectGroup1(30), //
	PreChargeContractorAbnormalOnBatterySelfDetectGroup2(31), //
	PreChargeContractorAbnormalOnBatterySelfDetectGroup3(32), //
	PreChargeContractorAbnormalOnBatterySelfDetectGroup4(33), //
	MainContactFailureOnBatteryControlGroup1(34), //
	MainContactFailureOnBatteryControlGroup2(35), //
	MainContactFailureOnBatteryControlGroup3(36), //
	MainContactFailureOnBatteryControlGroup4(37), //
	PreChargeFailureOnBatteryControlGroup1(38), //
	PreChargeFailureOnBatteryControlGroup2(39), //
	PreChargeFailureOnBatteryControlGroup3(40), //
	PreChargeFailureOnBatteryControlGroup4(41), //
	SamplingCircuitAbnormalForBMU(42), //
	PowerCableDisconnectFailure(43), //
	SamplingCircuitDisconnectFailure(44), //
	CANDisconnectForMasterAndSlave(45), //
	SammplingCircuitFailure(46), //
	SingleBatteryFailure(47), //
	CircuitDetectionAbnormalForMainContactor(48), //
	CircuitDetectionAbnormalForMainContactorSecond(49), //
	CircuitDetectionAbnormalForFancontactor(50), //
	BMUPowerContactorCircuitDetectionAbnormal(51), //
	CentralContactorCircuitDetectionAbnormal(52), //
	SeriousTemperatureFault(53), //
	CommunicationFaultForSystemController(54), //
	FrogAlarm(55), //
	FuseFault(56), //
	NormalLeakage(57), //
	SeriousLeakage(58), //
	CANDisconnectionBetweenBatteryGroupAndBatteryStack(59), //
	CentralContactorCircuitOpen(60), //
	BMUPowerContactorOpen(61);

	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
