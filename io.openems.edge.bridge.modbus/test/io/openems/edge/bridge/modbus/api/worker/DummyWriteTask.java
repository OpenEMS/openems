package io.openems.edge.bridge.modbus.api.worker;

import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.taskmanager.Priority;

public class DummyWriteTask extends AbstractDummyTask implements WriteTask {

	public DummyWriteTask(String name, long delay) {
		super(name, delay);
	}

	@Override
	public String toString() {
		return "DummyWriteTask [name=" + this.name + ", delay=" + this.getDelay() + "]";
	}

	@Override
	public Priority getPriority() {
		return Priority.HIGH;
	}

}