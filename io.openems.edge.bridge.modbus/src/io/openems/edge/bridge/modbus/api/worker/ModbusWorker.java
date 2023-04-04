package io.openems.edge.bridge.modbus.api.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.Mutex;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WaitTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;

/**
 * The ModbusWorker schedules the execution of all Modbus-Tasks, like reading
 * and writing modbus registers.
 *
 * <p>
 * It tries to execute all Write-Tasks as early as possible (directly after the
 * TOPIC_CYCLE_EXECUTE_WRITE event) and all Read-Tasks as late as possible to
 * have values available exactly when they are needed (i.e. at the
 * TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event). Calculating the required Wait-Time
 * is handled in {@link WaitHandler}.
 * 
 */
public class ModbusWorker extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(ModbusWorker.class);

	// Callbacks
	private final ThrowingFunction<Task, Integer, OpenemsException> execute;
	private final Consumer<Boolean> setCycleTimeIsTooShort;
	private final Consumer<ModbusElement<?>[]> invalidate;
	private final BiConsumer<Logger, String> logWarn;

	private final AtomicReference<LogVerbosity> logVerbosity;
	private final LinkedBlockingDeque<WriteTask> writeTasksQueue = new LinkedBlockingDeque<>();
	private final LinkedBlockingDeque<Task> readTasksQueue = new LinkedBlockingDeque<>();
	private final Mutex signalAvailableTaskInQueue = new Mutex(false);
	private final DefectiveComponents defectiveComponents;
	private final ModbusTasksManager tasksManager;
	private final WaitHandler waitHandler = new WaitHandler();

	/**
	 * 
	 * @param execute                executes a {@link Task}; returns number of
	 *                               actually executed subtasks
	 * @param invalidate             invalidates the given {@link ModbusElement}s
	 *                               after read errors
	 * @param setCycleTimeIsTooShort sets the
	 *                               {@link BridgeModbus.ChannelId#CYCLE_TIME_IS_TOO_SHORT}
	 *                               channel
	 * @param logWarn                logs a warning
	 * @param logVerbosity           the configured {@link LogVerbosity}
	 */
	public ModbusWorker(ThrowingFunction<Task, Integer, OpenemsException> execute,
			Consumer<ModbusElement<?>[]> invalidate, Consumer<Boolean> setCycleTimeIsTooShort,
			BiConsumer<Logger, String> logWarn, AtomicReference<LogVerbosity> logVerbosity) {
		this.execute = execute;
		this.invalidate = invalidate;
		this.logWarn = logWarn;
		this.logVerbosity = logVerbosity;

		this.setCycleTimeIsTooShort = setCycleTimeIsTooShort;
		this.defectiveComponents = new DefectiveComponents();
		this.tasksManager = new ModbusTasksManager(this.defectiveComponents);
	}

	/**
	 * This is called on TOPIC_CYCLE_BEFORE_PROCESS_IMAGE cycle event.
	 */
	public synchronized void onBeforeProcessImage() {
		this.log("-> onBeforeProcessImage");

		// Update internal size of the WaitHandler queue if required. This causes the
		// WaitHandler to automatically adapt to the number of Tasks and the number of
		// required Cycles.
		this.waitHandler.updateSize(this.tasksManager.countReadTasks());

		// Forward TOPIC_CYCLE_BEFORE_PROCESS_IMAGE to WaitHandler
		this.waitHandler.onBeforeProcessImage();

		// Set CYCLE_TIME_IS_TOO_SHORT state-channel
		this.setCycleTimeIsTooShort.accept(this.waitHandler.isCycleTimeTooShort());

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
		this.readTasksQueue.addAll(this.tasksManager.getNextReadTasks());
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
				this.thread.interrupt();
			}

			if (!this.writeTasksQueue.isEmpty()) {
				this.log("Previous WriteTasks queue is not empty on TOPIC_CYCLE_EXECUTE_WRITE");
				return;
			}

			// Add All WriteTasks
			this.writeTasksQueue.addAll(this.tasksManager.getNextWriteTasks());

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
	private Task getNextTask() throws InterruptedException {
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

	@Override
	protected void forever() throws InterruptedException {
		var task = this.getNextTask();
		synchronized (this.waitHandler.activeWaitTask) {
			if (task instanceof WaitTask) {
				this.waitHandler.activeWaitTask.set((WaitTask) task);
			} else {
				this.waitHandler.activeWaitTask.set(null);
			}
		}

		try {
			// execute the task
			this.log("Execute " + task); // TODO remove before merge
			var noOfExecutedSubTasks = this.execute.apply(task);

			if (noOfExecutedSubTasks > 0) {
				// no exception & at least one sub-task executed
				this.markComponentAsDefective(task.getParent(), false);
			}

		} catch (OpenemsException e) {
			this.logWarn.accept(this.log, task.toString() + " execution failed: " + e.getMessage());
			this.markComponentAsDefective(task.getParent(), true);

			// invalidate elements of this task
			this.invalidate.accept(task.getElements());
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

	/**
	 * Marks the given {@link ModbusComponent} as defective or non-defective.
	 * 
	 * <ul>
	 * <li>Sets 'ModbusCommunicationFailed' Channel of the ModbusComponent
	 * <li>Adds/Removes the component to/from the {@link DefectiveComponents}
	 * <li>Informs the {@link WaitHandler}
	 * </ul>
	 * 
	 * @param component   the {@link ModbusComponent}
	 * @param isDefective mark as defective (true) or non-defective (false)
	 */
	private void markComponentAsDefective(ModbusComponent component, boolean isDefective) {
		if (component != null) {
			if (isDefective) {
				// Component is defective
				this.defectiveComponents.add(component.id());
				component._setModbusCommunicationFailed(true);
				this.waitHandler.setCycleContainedDefectiveComponent();

			} else {
				// Read from/Write to Component was successful
				this.defectiveComponents.remove(component.id());
				component._setModbusCommunicationFailed(false);
			}
		}
	}

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.tasksManager.addProtocol(sourceId, protocol);
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		this.tasksManager.removeProtocol(sourceId);
	}

}