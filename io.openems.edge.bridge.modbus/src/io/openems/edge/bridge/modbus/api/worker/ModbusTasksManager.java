package io.openems.edge.bridge.modbus.api.worker;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

/**
 * Manages a {@link TasksManager}s for ModbusWorker.
 */
public class ModbusTasksManager {

	/**
	 * Component-ID -> TasksManager for {@link ReadTask}s.
	 */
	private final Map<String, TasksManager<ReadTask>> readTaskManagers = new HashMap<>();
	/**
	 * Component-ID -> TasksManager for {@link WriteTask}s.
	 */
	private final Map<String, TasksManager<WriteTask>> writeTaskManagers = new HashMap<>();
	/**
	 * Component-ID -> Queue of {@link ReadTask}s.
	 */
	private final Queue<ReadTask> nextLowPriorityTasks = new LinkedList<>();

	private final DefectiveComponents defectiveComponents;

	public ModbusTasksManager(DefectiveComponents defectiveComponents) {
		this.defectiveComponents = defectiveComponents;
	}

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.readTaskManagers.put(sourceId, protocol.getReadTasksManager());
		this.writeTaskManagers.put(sourceId, protocol.getWriteTasksManager());
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		this.readTaskManagers.remove(sourceId);
		this.writeTaskManagers.remove(sourceId);
	}

	/**
	 * Gets the total number of Read-Tasks.
	 * 
	 * @return number of Read-Tasks
	 */
	public int countReadTasks() {
		return this.readTaskManagers.values().stream() //
				.mapToInt(TasksManager::countTasks) //
				.sum();
	}

	/**
	 * Gets the next {@link ReadTask}s.
	 * 
	 * @return a list of tasks
	 */
	public List<ReadTask> getNextReadTasks() {
		var result = this.getHighPriorityReadTasks();

		var lowPriorityTask = this.getOneLowPriorityReadTask();
		if (lowPriorityTask != null) {
			result.addFirst(lowPriorityTask);
		}
		return result;
	}

	/**
	 * Gets the next {@link WriteTask}s.
	 * 
	 * @return a list of tasks
	 */
	public List<WriteTask> getNextWriteTasks() {
		return this.writeTaskManagers.entrySet().stream() //
				// Take only components that are not defective
				// TODO does not work if component has no read tasks
				.filter(e -> !this.defectiveComponents.isKnown(e.getKey())) //
				.flatMap(e -> e.getValue().getTasks().stream()) //
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Get HIGH priority tasks + handle defective components.
	 * 
	 * @return a list of {@link ReadTask}s
	 */
	private synchronized LinkedList<ReadTask> getHighPriorityReadTasks() {
		var result = new LinkedList<ReadTask>();
		for (var e : this.readTaskManagers.entrySet()) {
			var isDueForNextTry = this.defectiveComponents.isDueForNextTry(e.getKey());
			if (isDueForNextTry == null) {
				// Component is not defective -> get all high prio tasks
				result.addAll(e.getValue().getTasks(Priority.HIGH));

			} else if (isDueForNextTry == true) {
				// Component is defective, but due for next try: get one task
				result.add(e.getValue().getOneTask());

			} else {
				// Component is defective and not due; add no task
			}
		}
		return result;
	}

	/**
	 * Get one LOW priority task; avoid defective components.
	 *
	 * @return the next task; null if there is no available task
	 */
	private synchronized ReadTask getOneLowPriorityReadTask() {
		var refilledBefore = false;
		while (true) {
			var task = this.nextLowPriorityTasks.poll();
			if (task == null) {
				if (refilledBefore) {
					// queue had been refilled before, but still cannot find a matching task -> quit
					return null;
				}
				// refill the queue
				this.nextLowPriorityTasks.addAll(this.readTaskManagers.values().stream() //
						.flatMap(m -> m.getTasks(Priority.LOW).stream()) //
						.collect(Collectors.toUnmodifiableList()));
				refilledBefore = true;
				continue;
			}

			if (!this.defectiveComponents.isKnown(task.getParent().id())) {
				return task;
			}
		}
	}

}