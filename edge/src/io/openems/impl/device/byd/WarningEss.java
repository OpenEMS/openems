package io.openems.impl.device.byd;

import io.openems.api.channel.thingstate.WarningEnum;
import io.openems.common.types.ThingStateInfo;

@ThingStateInfo(reference = Bem125ktla01Ess.class)
public enum WarningEss implements WarningEnum {

	WarningState(0), //
	ProtectionState(1), //
	DeratingState(2), //
	ChargeForbidden(3), //
	DischargeForbidden(4), //
	StatusAbnormalOfACSurgeProtector(5), //
	CloseOfControlSwitch(6), //
	EmergencyStop(7), //
	StatusAbnormalOfFrogDetector(8), //
	SeriousLeakage(9), //
	NormalLeakage(10), //
	FailureOfTemperatureSensorInControlCabinet(11), //
	FailureOfHumiditySensorInControlCabinet(12), //
	FailureOfStorageDevice(13), //
	ExceedingOfHumidityInControlCabinet(14);

	public final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
