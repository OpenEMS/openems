package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public interface ModbusCoilElement extends ModbusElement<Boolean> {

	/**
	 * Sets the boolean value of this Element from Modbus Coil
	 * 
	 * @param coil
	 * @throws OpenemsException
	 */
	public void setInputCoil(Boolean coil) throws OpenemsException;

	/**
	 * Sets a value that should be written to the Modbus device
	 * 
	 * @param valueOpt
	 * @throws OpenemsException
	 */
	public default void setNextWriteValue(Optional<Boolean> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			this._setNextWriteValue( //
					Optional.of( //
							TypeUtils.<Boolean>getAsType(OpenemsType.BOOLEAN, valueOpt.get())));
		} else {
			this._setNextWriteValue(Optional.empty());
		}
	}

	/**
	 * Gets the next write value and resets it.
	 * 
	 * @return
	 */
	public default Optional<Boolean> getNextWriteValueAndReset() {
		Optional<Boolean> valueOpt = this.getNextWriteValue();
		try {
			this._setNextWriteValue(Optional.empty());
		} catch (OpenemsException e) {
			// can be safely ignored
		}
		return valueOpt;
	}

	public Optional<Boolean> getNextWriteValue();
}
