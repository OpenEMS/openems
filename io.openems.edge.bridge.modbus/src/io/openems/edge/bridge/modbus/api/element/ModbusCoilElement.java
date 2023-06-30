package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * A ModbusCoilElement represents one or more Modbus Coils.
 */
public interface ModbusCoilElement extends ModbusElement<Boolean> {

	/**
	 * Sets the boolean value of this Element from Modbus Coil.
	 *
	 * @param coil the value
	 * @throws OpenemsException on error
	 */
	public void setInputCoil(Boolean coil) throws OpenemsException;

	/**
	 * Sets a value that should be written to the Modbus device.
	 *
	 * @param valueOpt the Optional value
	 * @throws OpenemsException on error
	 */
	public default void setNextWriteValue(Optional<Boolean> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			this._setNextWriteValue(//
					Optional.of(//
							TypeUtils.<Boolean>getAsType(OpenemsType.BOOLEAN, valueOpt.get())));
		} else {
			this._setNextWriteValue(Optional.empty());
		}
	}

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
	public default Optional<Boolean> getNextWriteValueAndReset() {
		var valueOpt = this.getNextWriteValue();
		try {
			if (valueOpt.isPresent()) {
				this._setNextWriteValue(Optional.empty());
			}
		} catch (OpenemsException e) {
			// can be safely ignored
		}
		return valueOpt;
	}

	/**
	 * Gets the next write value.
	 * 
	 * @return the Optional next write value
	 */
	public Optional<Boolean> getNextWriteValue();
}
