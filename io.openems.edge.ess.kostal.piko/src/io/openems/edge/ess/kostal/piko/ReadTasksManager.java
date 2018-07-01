package io.openems.edge.ess.kostal.piko;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.kostal.piko.EssKostalPiko.ChannelId;

public class ReadTasksManager {

	private final EssKostalPiko parent;
	private final ReadTask[] tasks;

	public ReadTasksManager(EssKostalPiko parent, ReadTask... tasks) {
		this.parent = parent;
		this.tasks = tasks;
	}

	List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>();
		result.add(this.tasks[0]);
		return result;
	}
}
