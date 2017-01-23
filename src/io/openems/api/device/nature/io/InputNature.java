package io.openems.api.device.nature.io;

import io.openems.api.device.nature.DeviceNature;
import io.openems.impl.protocol.modbus.ModbusCoilReadChannel;

public interface InputNature extends DeviceNature {

	public ModbusCoilReadChannel[] getInput();
}
