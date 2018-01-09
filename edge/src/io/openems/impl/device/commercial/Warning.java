package io.openems.impl.device.commercial;

import io.openems.api.channel.thingstate.WarningEnum;

public enum Warning implements WarningEnum {
	EmergencyStop(0),KeyManualStop(1),TransformerPhaseBTemperatureSensorInvalidation(2),SDMemoryCardInvalidation(3);

	private final int value;

	private Warning(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

}
