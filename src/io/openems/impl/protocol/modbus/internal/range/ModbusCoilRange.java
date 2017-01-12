package io.openems.impl.protocol.modbus.internal.range;

import io.openems.impl.protocol.modbus.ModbusElement;

public class ModbusCoilRange extends ModbusRange {

	public ModbusCoilRange(int startAddress, ModbusElement<Boolean>[] elements) {
		super(startAddress, elements);
	}

	@Override public ModbusElement<Boolean>[] getElements() {
		// TODO Auto-generated method stub
		return super.getElements();
	}

}
