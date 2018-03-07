package io.openems.impl.device.commercial;

import io.openems.api.channel.thingstate.WarningEnum;

public enum WarningEss implements WarningEnum {
	EmergencyStop(0), //
	KeyManualStop(1), //
	TransformerPhaseBTemperatureSensorInvalidation(2), //
	SDMemoryCardInvalidation(4), //
	InverterCommunicationAbnormity(5), //
	BatteryStackCommunicationAbnormity(6), //
	MultifunctionalAmmeterCommunicationAbnormity(7), //
	RemoteCommunicationAbnormity(8), //
	PVDC1CommunicationAbnormity(9), //
	PVDC2CommunicationAbnormity(10), //
	TransformerSevereOvertemperature(11), //
	DCPrechargeContactorInspectionAbnormity(12), //
	DCBreaker1InspectionAbnormity(13), //
	DCBreaker2InspectionAbnormity(14), //
	ACPrechargeContactorInspectionAbnormity(15), //
	ACMainontactorInspectionAbnormity(16), //
	ACBreakerInspectionAbnormity(17), //
	DCBreaker1CloseUnsuccessfully(18), //
	DCBreaker2CloseUnsuccessfully(19), //
	ControlSignalCloseAbnormallyInspectedBySystem(20), //
	ControlSignalOpenAbnormallyInspectedBySystem(21), //
	NeutralWireContactorCloseUnsuccessfully(22), //
	NeutralWireContactorOpenUnsuccessfully(23), //
	WorkDoorOpen(24), //
	Emergency1Stop(25), //
	ACBreakerCloseUnsuccessfully(26), //
	ControlSwitchStop(27), //
	GeneralOverload(28), SevereOverload(29), //
	BatteryCurrentOverLimit(30), //
	PowerDecreaseCausedByOvertemperature(31), //
	InverterGeneralOvertemperature(32), //
	ACThreePhaseCurrentUnbalance(33), //
	RestoreFactorySettingUnsuccessfully(34), //
	PoleBoardInvalidation(35), //
	SelfInspectionFailed(36), //
	ReceiveBMSFaultAndStop(37), //
	RefrigerationEquipmentinvalidation(38), //
	LargeTemperatureDifferenceAmongIGBTThreePhases(39), //
	EEPROMParametersOverRange(40), //
	EEPROMParametersBackupFailed(41), //
	DCBreakerCloseunsuccessfully(42), //
	CommunicationBetweenInverterAndBSMUDisconnected(43), //
	CommunicationBetweenInverterAndMasterDisconnected(44), //
	CommunicationBetweenInverterAndUCDisconnected(45), //
	BMSStartOvertimeControlledByPCS(46), //
	BMSStopOvertimeControlledByPCS(47), //
	SyncSignalInvalidation(48), //
	SyncSignalContinuousCaputureFault(49), //
	SyncSignalSeveralTimesCaputureFault(50);

	public final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
