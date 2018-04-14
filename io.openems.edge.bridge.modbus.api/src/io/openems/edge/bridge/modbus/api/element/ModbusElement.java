package io.openems.edge.bridge.modbus.api.element;

import io.openems.edge.bridge.modbus.api.task.Task;

/**
 * A ModbusElement represents one or more registers or coils in a {@link Task}.
 * 
 * @author stefan.feilmeier
 */
public interface ModbusElement {
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
	 * Set the {@link Task}, where this Element belongs to. This is called during
	 * {@link Task}.add()
	 *
	 * @param task
	 */
	public void setModbusTask(Task task);

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
}
