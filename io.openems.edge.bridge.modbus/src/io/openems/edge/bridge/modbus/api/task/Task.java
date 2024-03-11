package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.ManagedTask;

public sealed interface Task extends ManagedTask permits AbstractTask, ReadTask, WriteTask, WaitTask {

	/**
	 * Gets the ModbusElements.
	 *
	 * @return an array of ModbusElements
	 */
	public ModbusElement[] getElements();

	/**
	 * Gets the start Modbus register address.
	 *
	 * @return the address
	 */
	public int getStartAddress();

	/**
	 * Gets the length from first to last Modbus register address.
	 *
	 * @return the address
	 */
	public int getLength();

	/**
	 * Sets the parent.
	 *
	 * @param parent the parent {@link AbstractOpenemsModbusComponent}.
	 */
	public void setParent(AbstractOpenemsModbusComponent parent);

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	public ModbusComponent getParent();

	/**
	 * Set skip cycles.
	 *
	 * @param cycles the task should be delayed cycles
	 * @return Task itself
	 */
	public Task setSkipCycles(int cycles);

	/**
	 * Get skip cycles.
	 *
	 * @return cycles the task should be delayed cycles
	 */
	public int getSkipCycles();

	/**
	 * This is called on deactivate of the Modbus-Bridge. It can be used to clear
	 * any references like listeners.
	 */
	public void deactivate();

	/**
	 * Executes the tasks - i.e. sends the query of a ReadTask or writes a
	 * WriteTask.
	 *
	 * @param bridge the Modbus-Bridge
	 * @return {@link ExecuteState}
	 */
	public ExecuteState execute(AbstractModbusBridge bridge);

	public static enum ExecuteState {
		/** Successfully executed request(s). */
		OK,
		/** No available requests -> no operation. */
		NO_OP,
		/** Executing request(s) failed. */
		ERROR;
	}
}