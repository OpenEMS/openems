package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.TypeUtils;

/**
 * A ModbusRegisterElement represents one or more Modbus Registers.
 * 
 * @param <T> the target type
 */
public interface ModbusRegisterElement<T> extends ModbusElement<T> {

	/**
	 * Sets the value of this Element from InputRegisters.
	 *
	 * @param registers the InputRegisters
	 * @throws OpenemsException on error
	 */
	public void setInputRegisters(InputRegister... registers) throws OpenemsException;

	/**
	 * Sets a value that should be written to the Modbus device.
	 *
	 * @param valueOpt the Optional value
	 * @throws OpenemsException         on error
	 * @throws IllegalArgumentException on error
	 */
	public default void setNextWriteValue(Optional<Object> valueOpt) throws OpenemsException, IllegalArgumentException {
		if (valueOpt.isPresent()) {
			this._setNextWriteValue(//
					Optional.of(//
							TypeUtils.<T>getAsType(this.getType(), valueOpt.get())));
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
	 * @return the next value as an Optional array of Registers
	 */
	public default Optional<Register[]> getNextWriteValueAndReset() {
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
	 * @return the next value as an Optional array of Registers
	 */
	public Optional<Register[]> getNextWriteValue();
}
