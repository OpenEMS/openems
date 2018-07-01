package io.openems.edge.ess.kostal.piko;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ReadTasksManager {

	private final List<ReadTask> highReadTasks = new ArrayList<>();
	private final List<ReadTask> lowReadTasks = new ArrayList<>();
	private final List<ReadTask> onceReadTasks = new ArrayList<>();

	private final Queue<ReadTask> lowNextReadTasks = new LinkedList<>();
	private final Queue<ReadTask> onceNextReadTasks = new LinkedList<>();

	public ReadTasksManager(ReadTask... tasks) {
		for (ReadTask task : tasks) {
			switch (task.getPriority()) {
			case HIGH:
				this.highReadTasks.add(task);
				break;
			case LOW:
				this.lowReadTasks.add(task);
				break;
			case ONCE:
				this.onceReadTasks.add(task);
				break;
			}
		}
		this.onceNextReadTasks.addAll(onceReadTasks);
	}

	public synchronized List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>();
		/*
		 * Handle HIGH
		 */
		result.addAll(this.highReadTasks);

		/*
		 * Handle LOW
		 */
		if (lowNextReadTasks.isEmpty()) {
			this.lowNextReadTasks.addAll(lowReadTasks);
		}
		ReadTask task = lowNextReadTasks.poll();
		if (task != null) {
			result.add(task);
		}

		/*
		 * Handle ONCE
		 */
		task = onceNextReadTasks.poll();
		if (task != null) {
			result.add(task);
		}
		return result;
	}
}