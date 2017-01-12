package io.openems.impl.protocol.modbus;

import io.openems.api.channel.ReadChannel;
import io.openems.api.thing.Thing;

public class ModbusReadChannel<T> extends ReadChannel<T> implements ModbusChannel<T> {

	public ModbusReadChannel(String id, Thing parent) {
		super(id, parent);
	}

	@Override protected void updateValue(T value) {
		super.updateValue(value);
	}

}
