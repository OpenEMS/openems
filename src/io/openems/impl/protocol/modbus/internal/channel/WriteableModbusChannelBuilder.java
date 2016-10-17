package io.openems.impl.protocol.modbus.internal.channel;

import io.openems.api.channel.ChannelBuilder;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;

public class WriteableModbusChannelBuilder extends ChannelBuilder<WriteableModbusChannelBuilder> {
	@Override
	public WriteableModbusChannel build() {
		return new WriteableModbusChannel(unit, minValue, maxValue);
	}
}
