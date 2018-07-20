package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

public class CoilElement extends AbstractModbusElement<Boolean> implements ModbusCoilElement {

	private final Logger log = LoggerFactory.getLogger(CoilElement.class);

	private Optional<Boolean> nextWriteValue = Optional.empty();

	public CoilElement(int startAddress) {
		super(OpenemsType.BOOLEAN, startAddress);
	}

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
			log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		this.nextWriteValue = valueOpt;
	}

	@Override
	public void setInputCoil(Boolean coil) throws OpenemsException {
		if (this.isDebug()) {
			log.info("Element [" + this + "] set input coil to [" + coil + "]");
		}
		// set value
		super.setValue(coil);
	}
}
