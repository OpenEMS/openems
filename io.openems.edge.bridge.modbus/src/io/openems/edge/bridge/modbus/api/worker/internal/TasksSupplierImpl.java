package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;
import io.openems.edge.common.type.Tuple;

/**
 * Supplies Tasks.
 */
public class TasksSupplierImpl implements TasksSupplier {

	/**
	 * Source-ID -> TasksManager for {@link Task}s.
	 */
	private final Map<String, TasksManager<Task>> taskManagers = new HashMap<>();

	/**
	 * Queue of LOW priority {@link ReadTask}s.
	 */
	private final Queue<Tuple<String, ReadTask>> nextLowPriorityTasks = new LinkedList<>();

	private long cycleIdx = 0;

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	public synchronized void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.taskManagers.put(sourceId, protocol.getTaskManager());
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	public synchronized void removeProtocol(String sourceId) {
		this.taskManagers.remove(sourceId);
	}

	@Override
	public synchronized CycleTasks getCycleTasks(DefectiveComponents defectiveComponents) {
		Map<String, LinkedList<Task>> tasks = new HashMap<>();
		// One Low Priority ReadTask
		{
			var t = this.getOneLowPriorityReadTask();
			if (t != null) {
				tasks.computeIfAbsent(t.a(), (ignore) -> new LinkedList<>()) //
						.add(t.b());
			}
		}
		// All High Priority ReadTasks + all WriteTasks
		this.taskManagers.forEach((id, taskManager) -> {
			var list = tasks.computeIfAbsent(id, (ignore) -> new LinkedList<>());
			taskManager.getTasks().stream() //
					.filter(t -> t instanceof WriteTask || t.getPriority() == Priority.HIGH) //
					.filter(t -> this.cycleIdx % (1 + t.getSkipCycles()) == 0)
					.forEach(list::add);
		});
		// Filter out defective components
		tasks.forEach((id, componentTasks) -> {
			var isDue = defectiveComponents.isDueForNextTry(id);
			if (isDue == null) {
				// Component is not defective -> keep all tasks
			} else if (isDue) {
				// Component is due for next try -> keep only one random Task
				Collections.shuffle(componentTasks);
				while (componentTasks.size() > 1) {
					componentTasks.pop();
				}
			} else {
				// Component is defective and not due -> drop all tasks
				componentTasks.clear();
			}
		});
		this.cycleIdx++;
		return new CycleTasks(//
				tasks.values().stream().flatMap(LinkedList::stream) //
						.filter(ReadTask.class::isInstance).map(ReadTask.class::cast) //
						// Sort HIGH priority to the end
						.sorted((a, b) -> b.getPriority().compareTo(a.getPriority())) //
						.collect(Collectors.toCollection(LinkedList::new)),
				tasks.values().stream().flatMap(LinkedList::stream) //
						.filter(WriteTask.class::isInstance).map(WriteTask.class::cast) //
						.collect(Collectors.toCollection(LinkedList::new)));
	}

	/**
	 * Get one LOW priority task.
	 *
	 * @return the next task; null if there is no available task
	 */
	private synchronized Tuple<String, ReadTask> getOneLowPriorityReadTask() {
		var refilledBefore = false;
		while (true) {
			var tasks = this.nextLowPriorityTasks.stream()
					.filter(item -> this.cycleIdx % (1 + item.b().getSkipCycles()) == 0)
					.collect(Collectors.toList());
			Tuple<String, ReadTask> task = null;
			if (tasks.size() > 0) {
				task = tasks.get(0);
				this.nextLowPriorityTasks.remove(task);
			}
			if (task != null) {
				return task;
			}
			if (refilledBefore) {
				// queue had been refilled before, but still cannot find a matching task -> quit
				return null;
			}
			// refill the queue
			this.taskManagers.forEach((id, taskManager) -> {
				taskManager.getTasks(Priority.LOW).stream() //
						.filter(ReadTask.class::isInstance).map(ReadTask.class::cast) //
						.map(t -> new Tuple<String, ReadTask>(id, t)) //
						.forEach(this.nextLowPriorityTasks::add);
			});
			refilledBefore = true;
		}
	}

	@Override
	public synchronized int getTotalNumberOfTasks() {
		return this.taskManagers.values().stream() //
				.mapToInt(m -> m.countTasks(this.cycleIdx)) //
				.sum();
	}
}
