package io.openems.impl.device.blueplanet50tl3;

import io.openems.api.channel.thingstate.FaultEnum;

public enum FaultEss implements FaultEnum {
	GroundFault(0), DCOverVolt(1), ACDisconnect(2), DCDisconnect(3), GridDisconnect(4), CabinetOpen(5), ManualShutdown(6),
	OverTemp(7), OverFrequency(8), UnderFrequency(9), ACOverVolt(10), ACUnderVolt(11), BlownStringFuse(12), UnderTemp(13),
	MemoryLoss(14), HWTestFailure(15), GroundFault1(16), DCOverVolt1(17), ACDisconnect1(18), DCDisconnect1(19), GridDisconnect1(20),
	CabinetOpen1(21), ManualShutdown1(22), OverTemp1(23), OverFrequency1(24), UnderFrequency1(25), ACOverVolt1(26),
	ACUnderVolt1(27), BlownStringFuse1(28), UnderTemp1(29), MemoryLoss1(30), HWTestFailure1(31), GroundFault2(32), InputOverVoltage(33),
	DCDisconnect2(34), CabinetOpen2(35), ManualShutdown2(36), OverTemp2(37), BlownFuse(38), UnderTemp2(39), MemoryLoss2(40), ArcDetection(41),
	TestFailed(42), InputUnderVoltage(43), InputOverCurrent(44);

	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
