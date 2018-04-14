package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.task.ReadTask;

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
	 * Set the {@link ReadTask}, where this Element belongs to. This is called
	 * during {@link ReadTask}.add()
	 *
	 * @param readTask
	 */
	public void setModbusTask(ReadTask readTask);

	/**
	 * Whether this Element should be ignored (= DummyElement)
	 * 
	 * @return
	 */
	public boolean isIgnored();

	/**
	 * Gets the {@link Priority} of this Element. It is used to priority queries of
	 * elements in order to best utilize the speed of the Modbus bus.
	 * 
	 * @return
	 */
	public Priority getPriority();

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
}
