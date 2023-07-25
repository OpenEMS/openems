package io.openems.edge.bridge.modbus.api.element;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;
import io.openems.edge.bridge.modbus.api.task.Task;

/**
 * This abstract class serves as an Interface-like abstraction to avoid Java
 * Generics for external access.
 */
public abstract sealed class ModbusElement permits AbstractModbusElement<?, ?, ?> {

	/** The start address of this Modbus element. */
	public final int startAddress;
	/** Number of Registers or Coils. */
	public final int length;

	/** The Task - set via {@link #setModbusTask(Task)}. */
	private Task task = null;

	public ModbusElement(int startAddress, int length) {
		this.startAddress = startAddress;
		this.length = length;
	}

	/**
	 * This is called on deactivate of the Modbus-Bridge. It can be used to clear
	 * any references like listeners.
	 */
	public abstract void deactivate();

	/**
	 * Invalidates the Channel in case it could not be read from the Modbus device,
	 * i.e. sets the value to 'UNDEFINED'/null. Applies the
	 * 'invalidateElementsAfterReadErrors' config setting of the bridge.
	 *
	 * @param bridge the {@link AbstractModbusBridge}
	 */
	public abstract void invalidate(AbstractModbusBridge bridge);

	/**
	 * Set the {@link Task}, where this Element belongs to.
	 *
	 * <p>
	 * This is called by the {@link AbstractTask} constructor.
	 *
	 * @param task the {@link Task}
	 */
	public final void setModbusTask(Task task) {
		this.task = task;
	}

	public final Task getModbusTask() {
		return this.task;
	}

}