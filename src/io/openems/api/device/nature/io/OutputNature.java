package io.openems.api.device.nature.io;

import io.openems.api.device.nature.DeviceNature;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;

public interface OutputNature extends DeviceNature {

	public ModbusCoilWriteChannel[] setOutput();

}
