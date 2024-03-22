package io.openems.edge.bridge.can.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.LogVerbosity;
import io.openems.edge.bridge.can.api.AbstractOpenemsCanComponent;
import io.openems.edge.bridge.can.api.element.CanChannelElement;
import io.openems.edge.common.taskmanager.Priority;

public class WaitTask implements Task {

	private final Logger log = LoggerFactory.getLogger(WaitTask.class);
	private final long delay;

	private AbstractOpenemsCanComponent parent = null;

	public WaitTask(long delay) {
		this.delay = delay;
	}

	@Override
	public Priority getPriority() {
		return Priority.LOW;
	}

	@Override
	public void setParent(AbstractOpenemsCanComponent parent) {
		this.parent = parent;
	}

	@Override
	public AbstractOpenemsCanComponent getParent() {
		return this.parent;
	}

	@Override
	public void deactivate() {
	}

	@Override
	public <T> int execute(AbstractCanBridge bridge) throws OpenemsException {
		if (bridge.getLogVerbosity() == LogVerbosity.ALL) {
			this.log.info("CAN Wait task sleep for " + this.delay + " ms");
		}
		try {
			Thread.sleep(this.delay);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}
		return 0;
	}

	@Override
	public boolean hasBeenExecuted() {
		return true;
	}

	@Override
	public long getExecuteDuration() {
		return this.delay;
	}

	@Override
	public CanChannelElement<?>[] getElements() {
		return new CanChannelElement[0];
	}

	@Override
	public Integer getCanAddress() {
		return 0;
	}

}