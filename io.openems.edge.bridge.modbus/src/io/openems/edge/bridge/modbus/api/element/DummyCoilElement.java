package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

/**
 * A DummyCoilElement is a placeholder for an empty {@link ModbusCoilElement}.
 */
public class DummyCoilElement extends AbstractModbusElement<Boolean> implements ModbusCoilElement, DummyElement {

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
		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	/**
	 * We are not setting a value for a DummyElement.
	 */
	@Override
	public void setInputCoil(Boolean coil) {
	}

	@Override
	public Optional<Boolean> getNextWriteValue() {
		return Optional.empty();
	}
}
