package io.openems.edge.bridge.modbus.api.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;

/**
 * A CoilElement has a size of one Modbus Coil or 1 bit.
 */
public class CoilElement extends ModbusElement<CoilElement, Boolean, Boolean> {

	private final Logger log = LoggerFactory.getLogger(CoilElement.class);

	// TODO private?
	protected final List<Consumer<Optional<Boolean>>> onSetNextWriteCallbacks = new ArrayList<>();

	private Optional<Boolean> nextWriteValue = Optional.empty();

	public CoilElement(int startAddress) {
		super(OpenemsType.BOOLEAN, startAddress, 1);
	}

	/**
	 * Gets the next write value.
	 * 
	 * @return the Optional next write value
	 */
	public Optional<Boolean> getNextWriteValue() {
		return this.nextWriteValue;
	}

//	@Override
//	public void _setNextWriteValue(Optional<Boolean> valueOpt) throws OpenemsException {
//		if (this.isDebug()) {
//			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
//		}
//		this.nextWriteValue = valueOpt;
//		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
//	}

	@Override
	/**
	 * Sets the boolean value of this Element from Modbus Coil.
	 *
	 * @param coil the value
	 */
	public void setInputValue(Boolean coil) {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set input coil to [" + coil + "]");
		}
		// set value
		super.setValue(coil);
	}

	@Override
	protected CoilElement self() {
		return this;
	}

	/**
	 * Sets a value that should be written to the Modbus device.
	 *
	 * @param valueOpt the Optional value
	 * @throws OpenemsException on error
	 */
//	public final void setNextWriteValue(Optional<Boolean> valueOpt) throws OpenemsException {
//		if (valueOpt.isPresent()) {
//			this._setNextWriteValue(//
//					Optional.of(//
//							TypeUtils.<Boolean>getAsType(OpenemsType.BOOLEAN, valueOpt.get())));
//		} else {
//			this._setNextWriteValue(Optional.empty());
//		}
//	}

	/**
	 * Gets the next write value and resets it.
	 *
	 * <p>
	 * This method should be called once in every cycle on the
	 * TOPIC_CYCLE_EXECUTE_WRITE event. It makes sure, that the nextWriteValue gets
	 * initialized in every Cycle. If registers need to be written again in every
	 * cycle, next setNextWriteValue()-method needs to called on every Cycle.
	 *
	 * @return the Optional next write value
	 */
	public final Optional<Boolean> getNextWriteValueAndReset() {
		var valueOpt = this.getNextWriteValue();
//		try {
//			if (valueOpt.isPresent()) {
//				this._setNextWriteValue(Optional.empty());
//			}
//		} catch (OpenemsException e) {
//			 can be safely ignored
//		}
		return valueOpt;
	}

}
