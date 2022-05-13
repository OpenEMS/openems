package io.openems.edge.bridge.modbus.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.ManagedTask;

public interface Task extends ManagedTask {

	/**
	 * Gets the ModbusElements.
	 *
	 * @return an array of ModbusElements
	 */
	ModbusElement<?>[] getElements();

	/**
	 * Gets the start Modbus register address.
	 *
	 * @return the address
	 */
	int getStartAddress();

	/**
	 * Gets the length from first to last Modbus register address.
	 *
	 * @return the address
	 */
	int getLength();

	/**
	 * Sets the parent.
	 *
	 * @param parent the parent {@link AbstractOpenemsModbusComponent}.
	 */
	void setParent(AbstractOpenemsModbusComponent parent);

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	public ModbusComponent getParent();

	/**
	 * This is called on deactivate of the Modbus-Bridge. It can be used to clear
	 * any references like listeners.
	 */
	void deactivate();

	/**
	 * Executes the tasks - i.e. sends the query of a ReadTask or writes a
	 * WriteTask.
	 *
	 * @param bridge the Modbus-Bridge
	 * @param <T>    the Modbus-Element
	 * @throws OpenemsException on error
	 * @return the number of executed Sub-Tasks
	 */
	<T> int execute(AbstractModbusBridge bridge) throws OpenemsException;

	/**
	 * Gets whether this ReadTask has been successfully executed before.
	 *
	 * @return true if this Task has been executed successfully at least once
	 */
	boolean hasBeenExecuted();

	/**
	 * Gets the execution duration of the last execution (successful or not not
	 * successful) in [ms].
	 *
	 * @return the duration in [ms]
	 */
	long getExecuteDuration();

}