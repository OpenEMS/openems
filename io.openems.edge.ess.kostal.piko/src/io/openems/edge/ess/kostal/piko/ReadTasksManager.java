package io.openems.edge.ess.kostal.piko;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.kostal.piko.EssKostalPiko.ChannelId;

public class ReadTasksManager {

	private final EssKostalPiko parent;
	private final ReadTask[] tasks;

	public ReadTasksManager(EssKostalPiko parent, ReadTask... tasks) {
		this.parent = parent;
		this.tasks = tasks;
	}

	private final ArrayListMultimap<Priority, ReadTask> readTasks = ArrayListMultimap.create();
	private final ArrayListMultimap<Priority, ReadTask> nextReadTasks = ArrayListMultimap.create();

	List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>(this.readTasks.get(Priority.HIGH).size() + 1);
		this.readTasks.keySet().forEach(priority -> {
			int take = 0;
			switch (priority) {
			case HIGH:
				take = -1;
				break;
			case LOW:
				take = 1;
				break;
			case ONCE:
				take = 1;
			}

			if (take == 0) {
			} else if (take < 0) {
				result.addAll(this.readTasks.get(priority));
			} else if (take > 1) {
				synchronized (this.nextReadTasks) {
					if (this.nextReadTasks.get(priority).size() == 0) {
						this.nextReadTasks.putAll(priority, this.readTasks.get(priority));
					}
					Iterator<ReadTask> iter = this.nextReadTasks.get(priority).iterator();
					while (iter.hasNext()) {
						result.add(iter.next());
						iter.remove();

					}
				}
			}

		});
		return result;
	}
}
