package io.openems.edge.bridge.can;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.bridge.can.api.BridgeCan;
import io.openems.edge.bridge.can.api.CanProtocol;
import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.element.CanChannelElement;
import io.openems.edge.bridge.can.api.task.ReadTask;
import io.openems.edge.bridge.can.api.task.Task;
import io.openems.edge.bridge.can.api.task.WaitTask;
import io.openems.edge.bridge.can.api.task.WriteTask;
import io.openems.edge.bridge.can.io.hw.CanDeviceException;
import io.openems.edge.common.taskmanager.MetaTasksManager;
import io.openems.edge.common.taskmanager.Priority;

/**
 * The CanWorker schedules the execution of all Can-Tasks
 *
 * <p>
 * It tries to execute all Write-Tasks as early as possible (directly after the
 * TOPIC_CYCLE_EXECUTE_WRITE event) and all Read-Tasks as late as possible to
 * have correct values available exactly when they are needed (i.e. at the
 * TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event).
 *
 * <p>
 * Also it collects all received CAN frames from the low level CAN driver and
 * forwards them to the responsible tasks
 */
class CanWorker extends AbstractImmediateWorker {

	private static final long TASK_DURATION_BUFFER = 50;

	private final Logger log = LoggerFactory.getLogger(CanWorker.class);
	// Measures the Cycle-Length between two consecutive BeforeProcessImage events
	private final Stopwatch cycleStopwatch = Stopwatch.createUnstarted();
	private final LinkedBlockingDeque<Task> tasksQueue = new LinkedBlockingDeque<>();
	private final MetaTasksManager<ReadTask> readTasksManager = new MetaTasksManager<>();
	private final MetaTasksManager<WriteTask> writeTasksManager = new MetaTasksManager<>();
	private final Multimap<String, Multimap<Integer, ReadTask>> tasksByCanAddress = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	// Holds source Component-IDs that are known to have errors.
	private final Set<String> defectiveComponents = new HashSet<>();
	private final AbstractCanBridge parent;
	private final CanReadWorker canReadWorker;

	// The measured duration between BeforeProcessImage event and ExecuteWrite event
	private long durationBetweenBeforeProcessImageTillExecuteWrite = 0;

	private int rcvdFramesPerCycle = 0;

	protected CanWorker(AbstractCanBridge parent, CanReadWorker canReadWorker) {
		this.parent = parent;
		this.canReadWorker = canReadWorker;
	}

	/**
	 * This is called on TOPIC_CYCLE_BEFORE_PROCESS_IMAGE cycle event.
	 */
	protected void onBeforeProcessImage() {
		try {
			this.evaluateIncomingCanFrames();
		} catch (CanDeviceException cde) {
			this.parent.logWarn(this.log, "CAN RX failed: " + cde.getMessage());
		} catch (OpenemsException oe) {
			this.parent.logWarn(this.log, "CAN RX failed " + oe.getMessage());
		}

		// Measure the actual cycle-time; and starts the next measure cycle
		var cycleTime = 1000L; // default to 1000 [ms] for the first run
		if (this.cycleStopwatch.isRunning()) {
			cycleTime = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
		}
		this.cycleStopwatch.reset();
		this.cycleStopwatch.start();

		// If the current tasks queue spans multiple cycles and we are in-between ->
		// stop here
		if (!this.tasksQueue.isEmpty()) {
			return;
		}

		// Collect the next read-tasks
		List<ReadTask> nextReadTasks = new ArrayList<>();
		var lowPriorityTask = this.getOneLowPriorityReadTask();
		if (lowPriorityTask != null) {
			nextReadTasks.add(lowPriorityTask);
		}
		nextReadTasks.addAll(this.getAllHighPriorityReadTasks());
		var readTasksDuration = 0L;
		for (ReadTask task : nextReadTasks) {
			readTasksDuration += task.getExecuteDuration();
		}

		// collect the next write-tasks
		var writeTasksDuration = 0L;
		var nextWriteTasks = this.getAllWriteTasks();
		for (WriteTask task : nextWriteTasks) {
			writeTasksDuration += task.getExecuteDuration();
		}

		// plan the execution for the next cycles
		var totalDuration = readTasksDuration + writeTasksDuration;
		var totalDurationWithBuffer = totalDuration + TASK_DURATION_BUFFER;
		var noOfRequiredCycles = ceilDiv(totalDurationWithBuffer, cycleTime);

		// Set EXECUTION_DURATION channel
		var executionDurationChannel = this.parent.channel(BridgeCan.ChannelId.EXECUTION_DURATION);
		executionDurationChannel.setNextValue(totalDuration);

		// Set CYCLE_TIME_IS_TOO_SHORT state-channel if more than one cycle is required;
		// but only if SlaveCommunicationFailed-Channel is not set
		var cycleTimeIsTooShortChannel = this.parent.channel(BridgeCan.ChannelId.CYCLE_TIME_IS_TOO_SHORT);
		if (noOfRequiredCycles > 1 && !this.parent.getSlaveCommunicationFailedChannel().value().orElse(false)) {
			cycleTimeIsTooShortChannel.setNextValue(true);
		} else {
			cycleTimeIsTooShortChannel.setNextValue(false);
		}

		var durationOfTasksBeforeExecuteWriteEvent = 0L;
		var noOfTasksBeforeExecuteWriteEvent = 0;
		for (ReadTask task : nextReadTasks) {
			if (durationOfTasksBeforeExecuteWriteEvent > this.durationBetweenBeforeProcessImageTillExecuteWrite) {
				break;
			}
			noOfTasksBeforeExecuteWriteEvent++;
			durationOfTasksBeforeExecuteWriteEvent += task.getExecuteDuration();
		}

		// Build Queue
		Deque<Task> tasksQueue = new LinkedList<>();

		// Add all write-tasks to the queue
		tasksQueue.addAll(nextWriteTasks);

		// Add all read-tasks to the queue
		for (var i = 0; i < nextReadTasks.size(); i++) {
			var task = nextReadTasks.get(i);
			if (i < noOfTasksBeforeExecuteWriteEvent) {
				// this Task will be executed before ExecuteWrite event -> add it to the end of
				// the queue
				tasksQueue.addLast(task);
			} else {
				// this Task will be executed after ExecuteWrite event -> add it to the
				// beginning of the queue
				tasksQueue.addFirst(task);
			}
		}

		// Add a waiting-task to the end of the queue
		var waitTillStart = noOfRequiredCycles * cycleTime - totalDurationWithBuffer;
		tasksQueue.addLast(new WaitTask(waitTillStart));

		// Copy all Tasks to the global tasks-queue
		this.tasksQueue.clear();
		this.tasksQueue.addAll(tasksQueue);
	}

	/**
	 * This is called on TOPIC_CYCLE_EXECUTE_WRITE cycle event.
	 */
	public void onExecuteWrite() {
		// calculate the duration between BeforeProcessImage event and ExecuteWrite
		// event. This duration is used for planning the queue in onBeforeProcessImage()
		if (this.cycleStopwatch.isRunning()) {
			this.durationBetweenBeforeProcessImageTillExecuteWrite = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
		} else {
			this.durationBetweenBeforeProcessImageTillExecuteWrite = 0;
		}
	}

	private void evaluateIncomingCanFrames() throws CanDeviceException, OpenemsException {
		var cntAllChannels = this.parent.channel(BridgeCan.ChannelId.COUNT_CAN_MESSAGES_PER_CYCLE);
		var cntReadTaskCycles = this.parent.channel(BridgeCan.ChannelId.COUNT_READ_TASK_CYCLES);

		var rxFrames = this.canReadWorker.fetchQueuedFrames();
		cntAllChannels.setNextValue(this.canReadWorker.getFramesCountAll());
		cntReadTaskCycles.setNextValue(this.canReadWorker.getCountReadTaskCyclces());

		if (rxFrames == null || rxFrames.length == 0) {
			return;
		}
		this.rcvdFramesPerCycle = rxFrames.length;
		this.parent.channel(BridgeCan.ChannelId.COUNT_PROCESSED_CAN_MESSAGES_PER_CYCLE)
				.setNextValue(this.rcvdFramesPerCycle);

		// TODO this probably fails; if there is more than one OSGI CAN module
		// running...
		var tasks = this.tasksByCanAddress.values();
		for (CanRxTxData rxFrame : rxFrames) {
			for (Multimap<Integer, ReadTask> task : tasks) {
				var ct = task.get(Integer.valueOf(rxFrame.getAddress()));
				for (ReadTask rt : ct) {
					rt.onReceivedFrame(rxFrame);
				}
			}
		}
	}

	@Override
	protected void forever() throws InterruptedException {
		var task = this.tasksQueue.takeLast();
		try {
			// execute the task
			var noOfExecutedSubTasks = task.execute(this.parent);

			if (noOfExecutedSubTasks > 0) {
				// no exception & at least one sub-task executed -> remove this component from
				// erroneous list and set the CommunicationFailedChannel to false
				if (task.getParent() != null) {
					this.defectiveComponents.remove(task.getParent().id());
				}

				this.parent.getSlaveCommunicationFailedChannel().setNextValue(false);
			}

		} catch (OpenemsException e) {
			this.parent.logWarn(this.log, task.toString() + " execution failed: " + e.getMessage());

			// mark this component as erroneous
			if (task.getParent() != null) {
				this.defectiveComponents.add(task.getParent().id());
			}

			// set the CommunicationFailedChannel to true
			this.parent.getSlaveCommunicationFailedChannel().setNextValue(true);

			// invalidate elements of this task
			for (CanChannelElement<?> element : task.getElements()) {
				element.invalidate(this.parent);
			}
		}
	}

	/**
	 * Gets one Read-Tasks with priority Low or Once.
	 *
	 * @return a list of ReadTasks by Source-ID
	 */
	private ReadTask getOneLowPriorityReadTask() {
		return this.readTasksManager.getOneTask(Priority.LOW);
	}

	/**
	 * Gets all the High-Priority Read-Tasks.
	 *
	 * <p>
	 * This checks if a device is listed as defective and - if it is - adds only one
	 * ReadTask of this Source-Component to the queue
	 *
	 * @return a list of ReadTasks
	 */
	private List<ReadTask> getAllHighPriorityReadTasks() {
		var tasks = this.readTasksManager.getAllTasksBySourceId(Priority.HIGH);
		return this.filterDefectiveComponents(tasks);
	}

	/**
	 * Gets the Write-Tasks by Source-ID.
	 *
	 * <p>
	 * This checks if a device is listed as defective and - if it is - adds only one
	 * WriteTask of this Source-Component to the queue
	 *
	 * @return a list of WriteTasks by Source-ID
	 */
	private List<WriteTask> getAllWriteTasks() {
		var tasks = this.writeTasksManager.getAllTasksBySourceId();
		return this.filterDefectiveComponents(tasks);
	}

	/**
	 * Filters a Multimap with Tasks by Component-ID. For Components that are known
	 * to be defective, only one task is added; otherwise all tasks are added to the
	 * result. The idea is to not execute tasks that are known to fail.
	 *
	 * @param <T>   the Task type
	 * @param tasks Tasks by Component-ID
	 * @return a list of filtered tasks
	 */
	private <T extends Task> List<T> filterDefectiveComponents(Multimap<String, T> tasks) {
		List<T> result = new ArrayList<>();
		for (Entry<String, Collection<T>> entry : tasks.asMap().entrySet()) {
			var componentId = entry.getKey();

			if (this.defectiveComponents.contains(componentId)) {
				// Component is known to be erroneous -> add only one Task
				var iterator = entry.getValue().iterator();
				if (iterator.hasNext()) {
					result.add(iterator.next());
				}

			} else {
				// Component is ok. All all tasks.
				result.addAll(entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the CanProtocol
	 */
	public void addProtocol(String sourceId, CanProtocol protocol) {
		this.readTasksManager.addTasksManager(sourceId, protocol.getReadTasksManager());
		this.writeTasksManager.addTasksManager(sourceId, protocol.getWriteTasksManager());
		this.tasksByCanAddress.put(sourceId, protocol.getTasksByCanAddress());

	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		this.readTasksManager.removeTasksManager(sourceId);
		this.writeTasksManager.removeTasksManager(sourceId);
		this.tasksByCanAddress.removeAll(sourceId);
	}

	/**
	 * This is a helper function. It calculates the opposite of Math.floorDiv().
	 *
	 * <p>
	 * Source:
	 * https://stackoverflow.com/questions/27643616/ceil-conterpart-for-math-floordiv-in-java
	 *
	 * @param x the dividend
	 * @param y the divisor
	 * @return the result of the division, rounded up
	 */
	private static long ceilDiv(long x, long y) {
		return -Math.floorDiv(-x, y);
	}

	public String debugStats() {
		return "RX " + this.rcvdFramesPerCycle + " frames/cycle";
	}

}