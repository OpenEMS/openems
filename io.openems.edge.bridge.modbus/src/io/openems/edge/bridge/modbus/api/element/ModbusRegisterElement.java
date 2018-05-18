package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.TypeUtils;

public interface ModbusRegisterElement<T> extends ModbusElement<T> {

	/**
	 * Sets the value of this Element from InputRegisters
	 * 
	 * @param registers
	 * @throws OpenemsException
	 */
	public void setInputRegisters(InputRegister... registers) throws OpenemsException;

	/**
	 * Sets a value that should be written to the Modbus device
	 * 
	 * @param valueOpt
	 * @throws OpenemsException
	 */
	public default void setNextWriteValue(Optional<Object> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			this._setNextWriteValue( //
					Optional.of( //
							TypeUtils.<T>getAsType(this.getType(), valueOpt.get())));
		} else {
			this._setNextWriteValue(Optional.empty());
		}
	}

	/**
	 * Gets the next write value and resets it.
	 * 
	 * @return
	 */
	public default Optional<Register[]> getNextWriteValueAndReset() {
		Optional<Register[]> valueOpt = this.getNextWriteValue();
		try {
			this._setNextWriteValue(Optional.empty());
		} catch (OpenemsException e) {
			// can be safely ignored
		}
		return valueOpt;
	}

	public Optional<Register[]> getNextWriteValue();
}
