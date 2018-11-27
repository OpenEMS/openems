package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;

/**
 * A ModbusElement represents one or more registers or coils in a
 * {@link ReadTask}.
 * 
 * @author stefan.feilmeier
 */
public interface ModbusElement<T> {
	/**
	 * Gets the start address of this modbus element
	 * 
	 * @return
	 */
	public int getStartAddress();

	/**
	 * Number of registers or coils
	 * 
	 * @return
	 */
	public abstract int getLength();

	/**
	 * Set the {@link AbstractTask}, where this Element belongs to. This is called during
	 * {@link AbstractTask}.add()
	 *
	 * @param abstractTask
	 */
	public void setModbusTask(AbstractTask abstractTask);

	/**
	 * Whether this Element should be ignored (= DummyElement)
	 * 
	 * @return
	 */
	public boolean isIgnored();

	/**
	 * Gets the type of this Register, e.g. INTEGER, BOOLEAN,..
	 * 
	 * @return
	 */
	public OpenemsType getType();

	/**
	 * Sets a value that should be written to the Modbus device
	 * 
	 * @param valueOpt
	 * @throws OpenemsException
	 */
	public void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException;

	/**
	 * resets the value in case if value could not be read from the modbus device
	 */
	public void invalidate();
}
