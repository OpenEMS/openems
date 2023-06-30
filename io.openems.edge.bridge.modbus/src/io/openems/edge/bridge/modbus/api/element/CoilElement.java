package io.openems.edge.bridge.modbus.api.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

/**
 * A CoilElement has a size of one Modbus Coil or 1 bit.
 */
public class CoilElement extends AbstractModbusElement<Boolean> implements ModbusCoilElement {

	private final Logger log = LoggerFactory.getLogger(CoilElement.class);
	private final List<Consumer<Optional<Boolean>>> onSetNextWriteCallbacks = new ArrayList<>();

	private Optional<Boolean> nextWriteValue = Optional.empty();

	public CoilElement(int startAddress) {
		super(OpenemsType.BOOLEAN, startAddress);
	}

	@Override
	public Optional<Boolean> getNextWriteValue() {
		return this.nextWriteValue;
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public void _setNextWriteValue(Optional<Boolean> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		this.nextWriteValue = valueOpt;
		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	@Override
	public void setInputCoil(Boolean coil) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set input coil to [" + coil + "]");
		}
		// set value
		super.setValue(coil);
	}
}
