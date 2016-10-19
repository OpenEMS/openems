package io.openems.api.channel;

import java.math.BigInteger;
import java.util.Map;

import io.openems.api.device.nature.DeviceNature;

public class ConfigChannel extends Channel {

	public ConfigChannel(DeviceNature nature, String unit, BigInteger minValue, BigInteger maxValue,
			BigInteger multiplier, BigInteger delta, Map<BigInteger, String> labels, BigInteger defaultValue) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels);
		updateValue(defaultValue, false);
	}

	@Override
	public void updateValue(BigInteger value) {
		// TODO: update to ConfigChannel should be represented in JsonConfig
		super.updateValue(value);
	}
}
