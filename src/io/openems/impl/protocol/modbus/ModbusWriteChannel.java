package io.openems.impl.protocol.modbus;

import io.openems.api.channel.WriteChannel;
import io.openems.api.thing.Thing;

public class ModbusWriteChannel<T> extends WriteChannel<T> implements ModbusChannel<T> {

	public ModbusWriteChannel(String id, Thing parent) {
		super(id, parent);
	}

	@Override protected void updateValue(T value) {
		super.updateValue(value);
	}

}
