package io.openems.impl.protocol.modbus.internal.range;

import io.openems.impl.protocol.modbus.ModbusElement;

public class ModbusRegisterRange extends ModbusRange {

	public ModbusRegisterRange(int startAddress, ModbusElement... elements) {
		super(startAddress, elements);
	}

}
