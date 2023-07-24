package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

/**
 * A CoilElement has a size of one Modbus Coil or 1 bit.
 */
public class CoilElement extends ModbusElement<CoilElement, Boolean, Boolean> {

	public CoilElement(int startAddress) {
		super(OpenemsType.BOOLEAN, startAddress, 1);
	}

	@Override
	protected CoilElement self() {
		return this;
	}

	@Override
	protected Boolean valueToRaw(Boolean value) {
		return value;
	}

	@Override
	protected Boolean rawToValue(Boolean value) {
		return value;
	}

}