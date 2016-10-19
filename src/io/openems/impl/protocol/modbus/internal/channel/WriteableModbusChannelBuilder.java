package io.openems.impl.protocol.modbus.internal.channel;

import io.openems.api.channel.WriteableChannelBuilder;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;

public class WriteableModbusChannelBuilder extends WriteableChannelBuilder<WriteableModbusChannelBuilder> {
	@Override
	public WriteableModbusChannel build() {
		return new WriteableModbusChannel(nature, unit, minValue, maxValue, multiplier, delta, labels, minWriteValue,
				minWriteValueChannel, maxWriteValue, maxWriteValueChannel);
	}
}
