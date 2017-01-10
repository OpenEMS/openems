package io.openems.impl.protocol.modbus.internal;

import io.openems.impl.protocol.modbus.ModbusElement;

public class InputRegistersModbusRange extends ModbusRange {

	public InputRegistersModbusRange(int startAddress, ModbusElement... elements) {
		super(startAddress, elements);
	}

}
