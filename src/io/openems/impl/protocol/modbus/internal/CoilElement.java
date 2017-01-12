package io.openems.impl.protocol.modbus.internal;

import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class CoilElement extends ModbusElement<Boolean> {
	public CoilElement(int address, ModbusChannel<Boolean> channel) {
		super(address, channel);
	}

	/**
	 * Updates the value of this Element from a Coil.
	 *
	 * @param registers
	 */
	@Override public void setValue(Boolean value) {
		super.setValue(value);
	}

	@Override public int getLength() {
		return 1;
	}
}
