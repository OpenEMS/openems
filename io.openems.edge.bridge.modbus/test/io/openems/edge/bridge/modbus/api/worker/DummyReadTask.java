package io.openems.edge.bridge.modbus.api.worker;

import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.common.taskmanager.Priority;

public class DummyReadTask extends AbstractDummyTask implements ReadTask {

	private final Priority priority;

	public DummyReadTask(String name, long delay, Priority priority) {
		super(name, delay);
		this.priority = priority;
	}

	@Override
	public String toString() {
		return "DummyReadTask [name=" + this.name + ", delay=" + this.delay + ", priority=" + this.priority + "]";
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}
}