package io.openems.impl.device.pro;

import io.openems.api.channel.thingstate.FaultEnum;

public enum FaultEss implements FaultEnum {
	ControlCurrentOverload100PercentL1(0), //
	ControlCurrentOverload110PercentL1(1), //
	ControlCurrentOverload150PercentL1(2), //
	ControlCurrentOverload200PercentL1(3), //
	ControlCurrentOverload120PercentL1(4), //
	ControlCurrentOverload300PercentL1(5), //
	ControlTransientLoad300PercentL1(6), //
	GridOverCurrentL1(7), //
	LockingWaveformTooManyTimesL1(8), //
	InverterVoltageZeroDriftErrorL1(9), //
	GridVoltageZeroDriftErrorL1(10), //
	ControlCurrentZeroDriftErrorL1(11), //
	InverterCurrentZeroDriftErrorL1(12), //
	GridCurrentZeroDriftErrorL1(13), //
	PDPProtectionL1(14), //
	HardwareControlCurrentProtectionL1(15), //
	HardwareACVoltageProtectionL1(16), //
	HardwareDCCurrentProtectionL1(17), //
	HardwareTemperatureProtectionL1(18), //
	NoCapturingSignalL1(19), //
	DCOvervoltageL1(20), //
	DCDisconnectedL1(21), //
	InverterUndervoltageL1(22), //
	InverterOvervoltageL1(23), //
	CurrentSensorFailL1(24), //
	VoltageSensorFailL1(25), //
	PowerUncontrollableL1(26), //
	CurrentUncontrollableL1(27), //
	FanErrorL1(28), //
	PhaseLackL1(29), //
	InverterRelayFaultL1(30), //
	GridRealyFaultL1(31), //
	ControlPanelOvertempL1(32), //
	PowerPanelOvertempL1(33), //
	DCInputOvercurrentL1(34), //
	CapacitorOvertempL1(35), //
	RadiatorOvertempL1(36), //
	TransformerOvertempL1(37), //
	CombinationCommErrorL1(38), //
	EEPROMErrorL1(39), //
	LoadCurrentZeroDriftErrorL1(40), //
	CurrentLimitRErrorL1(41), //
	PhaseSyncErrorL1(42), //
	ExternalPVCurrentZeroDriftErrorL1(43), //
	ExternalGridCurrentZeroDriftErrorL1(44), //
	ControlCurrentOverload100PercentL2(45), //
	ControlCurrentOverload110PercentL2(46), //
	ControlCurrentOverload150PercentL2(47), //
	ControlCurrentOverload200PercentL2(48), //
	ControlCurrentOverload120PercentL2(49), //
	ControlCurrentOverload300PercentL2(50), //
	ControlTransientLoad300PercentL2(51), //
	GridOverCurrentL2(52), //
	LockingWaveformTooManyTimesL2(53), //
	InverterVoltageZeroDriftErrorL2(54), //
	GridVoltageZeroDriftErrorL2(55), //
	ControlCurrentZeroDriftErrorL2(56), //
	InverterCurrentZeroDriftErrorL2(57), //
	GridCurrentZeroDriftErrorL2(58), //
	PDPProtectionL2(59), //
	HardwareControlCurrentProtectionL2(60), //
	HardwareACVoltageProtectionL2(61), //
	HardwareDCCurrentProtectionL2(62), //
	HardwareTemperatureProtectionL2(63), //
	NoCapturingSignalL2(64), //
	DCOvervoltageL2(65), //
	DCDisconnectedL2(66), //
	InverterUndervoltageL2(67), //
	InverterOvervoltageL2(68), //
	CurrentSensorFailL2(69), //
	VoltageSensorFailL2(70), //
	PowerUncontrollableL2(71), //
	CurrentUncontrollableL2(72), //
	FanErrorL2(73), //
	PhaseLackL2(74), //
	InverterRelayFaultL2(75), //
	GridRealyFaultL2(76), //
	ControlPanelOvertempL2(77), //
	PowerPanelOvertempL2(78), //
	DCInputOvercurrentL2(79), //
	CapacitorOvertempL2(80), //
	RadiatorOvertempL2(81), //
	TransformerOvertempL2(82), //
	CombinationCommErrorL2(83), //
	EEPROMErrorL2(84), //
	LoadCurrentZeroDriftErrorL2(85), //
	CurrentLimitRErrorL2(86), //
	PhaseSyncErrorL2(87), //
	ExternalPVCurrentZeroDriftErrorL2(88), //
	ExternalGridCurrentZeroDriftErrorL2(89), //
	ControlCurrentOverload100PercentL3(90), //
	ControlCurrentOverload110PercentL3(91), //
	ControlCurrentOverload150PercentL3(92), //
	ControlCurrentOverload200PercentL3(93), //
	ControlCurrentOverload120PercentL3(94), //
	ControlCurrentOverload300PercentL3(95), //
	ControlTransientLoad300PercentL3(96), //
	GridOverCurrentL3(97), //
	LockingWaveformTooManyTimesL3(98), //
	InverterVoltageZeroDriftErrorL3(99), //
	GridVoltageZeroDriftErrorL3(100), //
	ControlCurrentZeroDriftErrorL3(101), //
	InverterCurrentZeroDriftErrorL3(102), //
	GridCurrentZeroDriftErrorL3(103), //
	PDPProtectionL3(104), //
	HardwareControlCurrentProtectionL3(105), //
	HardwareACVoltageProtectionL3(106), //
	HardwareDCCurrentProtectionL3(107), //
	HardwareTemperatureProtectionL3(108), //
	NoCapturingSignalL3(109), //
	DCOvervoltageL3(110), //
	DCDisconnectedL3(111), //
	InverterUndervoltageL3(112), //
	InverterOvervoltageL3(113), //
	CurrentSensorFailL3(114), //
	VoltageSensorFailL3(115), //
	PowerUncontrollableL3(116), //
	CurrentUncontrollableL3(117), //
	FanErrorL3(118), //
	PhaseLackL3(119), //
	InverterRelayFaultL3(120), //
	GridRealyFaultL3(121), //
	ControlPanelOvertempL3(122), //
	PowerPanelOvertempL3(123), //
	DCInputOvercurrentL3(124), //
	CapacitorOvertempL3(125), //
	RadiatorOvertempL3(126), //
	TransformerOvertempL3(127), //
	CombinationCommErrorL3(128), //
	EEPROMErrorL3(129), //
	LoadCurrentZeroDriftErrorL3(130), //
	CurrentLimitRErrorL3(131), //
	PhaseSyncErrorL3(132), //
	ExternalPVCurrentZeroDriftErrorL3(133), //
	ExternalGridCurrentZeroDriftErrorL3(134), //
	SystemFault(135), //
	BatteryFault(136), //
	PCSFault(137);

	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
