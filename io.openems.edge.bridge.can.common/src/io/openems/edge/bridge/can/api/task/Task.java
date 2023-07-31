package io.openems.edge.bridge.can.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.api.AbstractOpenemsCanComponent;
import io.openems.edge.bridge.can.api.element.CanChannelElement;
import io.openems.edge.common.taskmanager.ManagedTask;

/**
 * Represents a basic CAN task.
 */
public interface Task extends ManagedTask {

	/**
	 * Gets the CanElements.
	 *
	 * @return an array of CanElements
	 */
	public CanChannelElement<?>[] getElements();

	/**
	 * Sets the parent.
	 *
	 * @param parent the parent {@link AbstractOpenemsCanComponent}.
	 */
	public void setParent(AbstractOpenemsCanComponent parent);

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	public AbstractOpenemsCanComponent getParent();

	/**
	 * This is called on deactivate of the CAN-Bridge. It can be used to clear any
	 * references like listeners.
	 */
	public void deactivate();

	/**
	 * Executes the tasks.
	 *
	 * @param bridge the CAN-Bridge
	 * @param <T>    the CAN-Element
	 * @return the number of executed Sub-Tasks
	 * @throws OpenemsException on error
	 */
	public <T> int execute(AbstractCanBridge bridge) throws OpenemsException;

	/**
	 * Gets whether this Task has been successfully executed before.
	 *
	 * @return true if this Task has been executed successfully at least once
	 */
	public boolean hasBeenExecuted();

	/**
	 * Gets the execution duration of the last execution (successful or not not
	 * successful) in [ms].
	 *
	 * @return the duration in [ms]
	 */
	public long getExecuteDuration();

	/**
	 * Gets the CAN address of the task.
	 *
	 * @return the CAN address where this task is responsible for
	 */
	public Integer getCanAddress();

}