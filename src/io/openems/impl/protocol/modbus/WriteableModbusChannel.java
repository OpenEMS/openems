package io.openems.impl.protocol.modbus;

import java.util.Map;

import io.openems.api.channel.Channel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.DeviceNature;

public class WriteableModbusChannel extends WriteableChannel {

	public WriteableModbusChannel(DeviceNature nature, String unit, Long minValue, Long maxValue, Long multiplier,
			Long delta, Map<Long, String> labels, Long minWriteValue, Channel minWriteValueChannel, Long maxWriteValue,
			Channel maxWriteValueChannel) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels, minWriteValue, minWriteValueChannel,
				maxWriteValue, maxWriteValueChannel);
	}

	@Override
	protected void updateValue(Long value) {
		super.updateValue(value);
	}
}
