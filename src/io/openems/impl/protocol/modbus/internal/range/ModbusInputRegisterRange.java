package io.openems.impl.protocol.modbus.internal.range;

import io.openems.impl.protocol.modbus.ModbusElement;

public class ModbusInputRegisterRange extends ModbusRegisterRange {

	public ModbusInputRegisterRange(int startAddress, ModbusElement... elements) {
		super(startAddress, elements);
	}

}
