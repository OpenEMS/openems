package io.openems.impl.protocol.modbus.internal;

import io.openems.impl.protocol.modbus.ModbusElement;

public class WritableModbusRange extends ModbusRange {

	public WritableModbusRange(int startAddress, ModbusElement... elements) {
		super(startAddress, elements);
	}

}
