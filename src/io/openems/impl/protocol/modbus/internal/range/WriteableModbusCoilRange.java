package io.openems.impl.protocol.modbus.internal.range;

import io.openems.impl.protocol.modbus.ModbusElement;

public class WriteableModbusCoilRange extends ModbusCoilRange implements WriteableModbusRange {

	public WriteableModbusCoilRange(int startAddress, ModbusElement... elements) {
		super(startAddress, elements);
	}

}
