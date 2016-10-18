package io.openems.impl.protocol.modbus.internal.channel;

import io.openems.api.channel.ChannelBuilder;
import io.openems.impl.protocol.modbus.ModbusChannel;

public class ModbusChannelBuilder extends ChannelBuilder<ModbusChannelBuilder> {
	@Override
	public ModbusChannel build() {
		return new ModbusChannel(unit, minValue, maxValue, multiplier, delta);
	}
}
