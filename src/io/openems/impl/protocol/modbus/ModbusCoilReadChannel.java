package io.openems.impl.protocol.modbus;

import io.openems.api.device.nature.DeviceNature;

public class ModbusCoilReadChannel extends ModbusReadChannel<Boolean> {

	public ModbusCoilReadChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	@Override public ModbusCoilReadChannel required() {
		super.required();
		return this;
	}

}
