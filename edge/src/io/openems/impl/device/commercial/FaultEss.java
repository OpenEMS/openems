package io.openems.impl.device.commercial;

import io.openems.api.channel.thingstate.FaultEnum;
import io.openems.common.types.ThingStateInfo;

@ThingStateInfo(reference = FeneconCommercialEss.class)
public enum FaultEss implements FaultEnum {
	DCPrechargeContactorCloseUnsuccessfully(0), //
	ACPrechargeContactorCloseUnsuccessfully(1), //
	ACMainContactorCloseUnsuccessfully(2), //
	DCElectricalBreaker1CloseUnsuccessfully(3), //
	DCMainContactorCloseUnsuccessfully(4), //
	ACBreakerTrip(5), //
	ACMainContactorOpenWhenRunning(6), //
	DCMainContactorOpenWhenRunning(7), //
	ACMainContactorOpenUnsuccessfully(8), //
	DCElectricalBreaker1OpenUnsuccessfully(9), //
	DCMainContactorOpenUnsuccessfully(10), //
	HardwarePDPFault(11), //
	MasterStopSuddenly(12), //
	DCShortCircuitProtection(13), //
	DCOvervoltageProtection(14), //
	DCUndervoltageProtection(15), //
	DCInverseNoConnectionProtection(16), //
	DCDisconnectionProtection(17), //
	CommutingVoltageAbnormityProtection(18), //
	DCOvercurrentProtection(19), //
	Phase1PeakCurrentOverLimitProtection(20), //
	Phase2PeakCurrentOverLimitProtection(21), //
	Phase3PeakCurrentOverLimitProtection(22), //
	Phase1GridVoltageSamplingInvalidation(23), //
	Phase2VirtualCurrentOverLimitProtection(24), //
	Phase3VirtualCurrentOverLimitProtection(25), //
	Phase1GridVoltageSamplingInvalidation2(26), //
	Phase2ridVoltageSamplingInvalidation(27), //
	Phase3GridVoltageSamplingInvalidation(28), //
	Phase1InvertVoltageSamplingInvalidation(29), //
	Phase2InvertVoltageSamplingInvalidation(30), //
	Phase3InvertVoltageSamplingInvalidation(31), //
	ACCurrentSamplingInvalidation(32), //
	DCCurrentSamplingInvalidation(33), //
	Phase1OvertemperatureProtection(34), //
	Phase2OvertemperatureProtection(35), //
	Phase3OvertemperatureProtection(36), //
	Phase1TemperatureSamplingInvalidation(37), //
	Phase2TemperatureSamplingInvalidation(38), //
	Phase3TemperatureSamplingInvalidation(39), //
	Phase1PrechargeUnmetProtection(40), //
	Phase2PrechargeUnmetProtection(41), //
	Phase3PrechargeUnmetProtection(42), //
	UnadaptablePhaseSequenceErrorProtection(43), //
	DSPProtection(44), //
	Phase1GridVoltageSevereOvervoltageProtection(45), //
	Phase1GridVoltageGeneralOvervoltageProtection(46), //
	Phase2GridVoltageSevereOvervoltageProtection(47), //
	Phase2GridVoltageGeneralOvervoltageProtection(48), //
	Phase3GridVoltageSevereOvervoltageProtection(49), //
	Phase3GridVoltageGeneralOvervoltageProtection(50), //
	Phase1GridVoltageSevereUndervoltageProtection(51), //
	Phase1GridVoltageGeneralUndervoltageProtection(52), //
	Phase2GridVoltageSevereUndervoltageProtection(53), //
	Phase2GridVoltageGeneralUndervoltageProtection(54), //
	Phase3GridVoltageSevereUndervoltageProtection(55), //
	Phase3GridVoltageGeneralUndervoltageProtection(56), //
	SevereOverfrequncyProtection(57), //
	GeneralOverfrequncyProtection(58), //
	SevereUnderfrequncyProtection(59), //
	GeneralsUnderfrequncyProtection(60), //
	Phase1Gridloss(61), //
	Phase2Gridloss(62), //
	Phase3Gridloss(63), //
	IslandingProtection(64), //
	Phase1UnderVoltageRideThrough(65), //
	Phase2UnderVoltageRideThrough(66), //
	Phase3UnderVoltageRideThrough(67), //
	Phase1InverterVoltageSevereOvervoltageProtection(68), //
	Phase1InverterVoltageGeneralOvervoltageProtection(69), //
	Phase2InverterVoltageSevereOvervoltageProtection(70), //
	Phase2InverterVoltageGeneralOvervoltageProtection(71), //
	Phase3InverterVoltageSevereOvervoltageProtection(72), //
	Phase3InverterVoltageGeneralOvervoltageProtection(73), //
	InverterPeakVoltageHighProtectionCauseByACDisconnect(74);

	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
