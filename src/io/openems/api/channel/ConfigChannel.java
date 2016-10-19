package io.openems.api.channel;

import java.util.Map;

import io.openems.api.device.nature.DeviceNature;

public class ConfigChannel extends Channel {

	public ConfigChannel(DeviceNature nature, String unit, Long minValue, Long maxValue, Long multiplier, Long delta,
			Map<Long, String> labels, Long defaultValue) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels);
		updateValue(defaultValue, false);
	}

	@Override
	public void updateValue(Long value) {
		// TODO: update to ConfigChannel should be represented in JsonConfig
		super.updateValue(value);
	}
}
