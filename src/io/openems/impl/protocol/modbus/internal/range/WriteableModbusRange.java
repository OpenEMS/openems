package io.openems.impl.protocol.modbus.internal.range;

import io.openems.impl.protocol.modbus.ModbusElement;

public interface WriteableModbusRange {

	public ModbusElement[] getElements();

	public int getLength();

	public int getStartAddress();

}
