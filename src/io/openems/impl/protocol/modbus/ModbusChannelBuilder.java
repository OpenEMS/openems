package io.openems.impl.protocol.modbus;

import io.openems.api.channel.ChannelBuilder;

public class ModbusChannelBuilder extends ChannelBuilder<ModbusChannelBuilder> {
	@Override
	public ModbusChannel build() {
		return new ModbusChannel(unit, minValue, maxValue);
	}
}
