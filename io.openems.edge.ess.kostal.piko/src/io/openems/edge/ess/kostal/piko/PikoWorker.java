package io.openems.edge.ess.kostal.piko;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.edge.common.worker.AbstractWorker;

public class PikoWorker extends AbstractWorker {

	private final Multimap<String, ReadTasksManager> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	@Override
	protected void forever() {
		// get the read tasks for this run
		List<ReadTask> nextReadTasks = this.getNextReadTasks();
		/**
		 * execute next read tasks
		 */
		nextReadTasks.forEach(readTask -> {
		});

	}

	private List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>();
		protocols.values().forEach(protocol -> {
			// get the next read tasks from the protocol
			List<ReadTask> nextReadTasks = protocol.getNextReadTasks();
			result.addAll(nextReadTasks);
		});
		return result;
	}

	@Override
	protected int getCycleTime() {
		return 1000;
	}
}
