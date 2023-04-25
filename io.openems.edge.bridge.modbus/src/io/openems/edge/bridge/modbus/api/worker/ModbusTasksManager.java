package io.openems.edge.bridge.modbus.api.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.Mutex;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

/**
 * Manages a {@link TasksManager}s for ModbusWorker.
 */
public class ModbusTasksManager {

	private final Logger log = LoggerFactory.getLogger(ModbusTasksManager.class);

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
	private final WaitHandler waitHandler;
	private final AtomicReference<LogVerbosity> logVerbosity;
	private final LinkedBlockingDeque<WriteTask> writeTasksQueue = new LinkedBlockingDeque<>();
	private final LinkedBlockingDeque<Task> readTasksQueue = new LinkedBlockingDeque<>();
	private final Mutex signalAvailableTaskInQueue = new Mutex(false);

	public ModbusTasksManager(DefectiveComponents defectiveComponents, WaitHandler waitHandler,
			AtomicReference<LogVerbosity> logVerbosity) {
		this.defectiveComponents = defectiveComponents;
		this.waitHandler = waitHandler;
		this.logVerbosity = logVerbosity;
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
	private int countReadTasks() {
		return this.readTaskManagers.values().stream() //
				.mapToInt(TasksManager::countTasks) //
				.sum();
	}

	/**
	 * This is called on TOPIC_CYCLE_BEFORE_PROCESS_IMAGE cycle event.
	 */
	public synchronized void onBeforeProcessImage() {
		this.log("-> onBeforeProcessImage");

		// Update internal size of the WaitHandler queue if required. This causes the
		// WaitHandler to automatically adapt to the number of Tasks and the number of
		// required Cycles.
		this.waitHandler.updateSize(this.countReadTasks());

		// If the current Read-Tasks queue spans multiple cycles and we are in-between
		// -> stop here
		if (!this.readTasksQueue.isEmpty()) {
			this.log("Previous ReadTasks queue is not empty on TOPIC_CYCLE_BEFORE_PROCESS_IMAGE");
			return;
		}

		// Add Wait-Task if appropriate
		var waitTask = this.waitHandler.getWaitTask();
		if (waitTask != null) {
			this.readTasksQueue.addFirst(waitTask);
		}

		// Collect the next read-tasks
		var nextReadTasks = this.getNextReadTasks();
		this.readTasksQueue.addAll(nextReadTasks);
		this.log.info(nextReadTasks.stream().map(Task::toString).collect(Collectors.joining(", ")));
		this.signalAvailableTaskInQueue.release();
	}

	/**
	 * This is called on TOPIC_CYCLE_EXECUTE_WRITE cycle event.
	 */
	public synchronized void onExecuteWrite() {
		this.log("-> onExecuteWrite");

		synchronized (this.waitHandler.activeWaitTask) {
			// Is currently a WaitTask active? Interrupt now and schedule again later.
			var activeWaitTask = this.waitHandler.activeWaitTask.get();
			if (activeWaitTask != null) {
				// TODO
				// this.thread.interrupt();
			}

			if (!this.writeTasksQueue.isEmpty()) {
				this.log("Previous WriteTasks queue is not empty on TOPIC_CYCLE_EXECUTE_WRITE");
				return;
			}

			// Add All WriteTasks
			this.writeTasksQueue.addAll(this.getNextWriteTasks());

			// Re-Schedule the WaitTask
			if (activeWaitTask != null) {
				this.readTasksQueue.addFirst(activeWaitTask);
			}

			this.signalAvailableTaskInQueue.release();
		}
	}

	/**
	 * Gets the next {@link Task}.
	 * 
	 * <ul>
	 * <li>1st priority: Write-Tasks
	 * <li>2nd priority: Read-Tasks
	 * </ul>
	 * 
	 * @return next {@link Task}
	 * @throws InterruptedException while waiting for
	 *                              {@link #signalAvailableTaskInQueue}
	 */
	public Task getNextTask() throws InterruptedException {
		while (true) {
			// Write-Task available?
			var writeTask = this.writeTasksQueue.pollFirst();
			if (writeTask != null) {
				return writeTask;
			}
			// Read-Task available?
			var readTask = this.readTasksQueue.pollFirst();
			if (readTask != null) {
				return readTask;
			}
			// No available Read-Task. Forward event to WaitHandler
			this.waitHandler.onAllTasksFinished();
			// Wait for signal
			this.signalAvailableTaskInQueue.await();
		}
	}

	/**
	 * Gets the next {@link ReadTask}s.
	 * 
	 * @return a list of tasks
	 */
	private List<ReadTask> getNextReadTasks() {
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
	private List<WriteTask> getNextWriteTasks() {
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

	// TODO remove before release
	private final Instant start = Instant.now();

	// TODO remove before release
	private void log(String message) {
		switch (this.logVerbosity.get()) {
		case DEV_REFACTORING:
			System.out.println(//
					String.format("%,10d %s", Duration.between(this.start, Instant.now()).toMillis(), message));
			break;
		case NONE:
		case READS_AND_WRITES:
		case WRITES:
			break;
		}
	}
}