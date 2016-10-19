package io.openems.impl.protocol.modbus;

import java.util.Map;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.DeviceNature;

public class ModbusChannel extends Channel {
	public ModbusChannel(DeviceNature nature, String unit, Long minValue, Long maxValue, Long multiplier, Long delta,
			Map<Long, String> labels) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels);
	}

	@Override
	protected void updateValue(Long value) {
		super.updateValue(value);
	}
}
