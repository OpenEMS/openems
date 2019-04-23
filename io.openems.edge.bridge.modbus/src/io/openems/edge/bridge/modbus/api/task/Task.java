package io.openems.edge.bridge.modbus.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
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
	AbstractOpenemsModbusComponent getParent();

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
	 */
	<T> void execute(AbstractModbusBridge bridge) throws OpenemsException;
}