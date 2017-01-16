package io.openems.api.device.nature.io;

import io.openems.impl.protocol.modbus.ModbusCoilReadChannel;

public interface InputNature {

	public ModbusCoilReadChannel[] getInput();
}
