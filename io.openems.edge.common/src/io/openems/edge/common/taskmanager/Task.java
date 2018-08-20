package io.openems.edge.common.taskmanager;

public class Task {

	private final Priority priority;

	public Task(Priority priority) {
		this.priority = priority;
	}

	public Priority getPriority() {
		return this.priority;
	}

}
