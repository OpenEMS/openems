package io.openems.impl.protocol.modbus.internal.channel;

import io.openems.api.channel.ChannelBuilder;
import io.openems.impl.protocol.modbus.ModbusChannel;

public class ModbusChannelBuilder extends ChannelBuilder<ModbusChannelBuilder> {
	@Override
	public ModbusChannel build() {
		return new ModbusChannel(nature, unit, minValue, maxValue, multiplier, delta, labels);
	}
}
