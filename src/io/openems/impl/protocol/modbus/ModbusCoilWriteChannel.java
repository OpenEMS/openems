package io.openems.impl.protocol.modbus;

import io.openems.api.thing.Thing;

public class ModbusCoilWriteChannel extends ModbusWriteChannel<Boolean> {

	public ModbusCoilWriteChannel(String id, Thing parent) {
		super(id, parent);
		// TODO Auto-generated constructor stub
	}

	@Override public ModbusCoilWriteChannel required() {
		super.required();
		return this;
	}

	@Override protected void updateValue(Boolean value) {
		super.updateValue(value);
	}
}
