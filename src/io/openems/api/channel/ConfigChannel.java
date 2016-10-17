package io.openems.api.channel;

import java.math.BigInteger;

public class ConfigChannel extends Channel {

	public ConfigChannel(String unit, BigInteger minValue, BigInteger maxValue, BigInteger defaultValue) {
		super(unit, minValue, maxValue);
		updateValue(defaultValue, false);
	}

	@Override
	public void updateValue(BigInteger value) {
		// TODO: update to ConfigChannel should be represented in JsonConfig
		super.updateValue(value);
	}
}
