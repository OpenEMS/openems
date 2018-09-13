package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class DummyCoilElement extends AbstractModbusElement<Boolean> implements ModbusCoilElement {

	public DummyCoilElement(int startAddress) {
		super(OpenemsType.BOOLEAN, startAddress);
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public void _setNextWriteValue(Optional<Boolean> valueOpt) {
		// ignore write
		return;
	}

	/**
	 * We are not setting a value for a DummyElement.
	 */
	@Override
	public void setInputCoil(Boolean coil) {
		return;
	}

	@Override
	public Optional<Boolean> getNextWriteValue() {
		return Optional.empty();
	}
}
