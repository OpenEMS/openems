package io.openems.edge.bridge.modbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.taskmanager.MetaTasksManager;
import io.openems.edge.common.taskmanager.Priority;

/**
 * The ModbusWorker schedules the execution of all Modbus-Tasks, like reading
 * and writing modbus registers.
 * 
 * <p>
 * It tries to execute all Write-Tasks as early as possible (directly after the
 * TOPIC_CYCLE_EXECUTE_WRITE event) and all Read-Tasks as late as possible to
 * have correct values available exactly when they are needed (i.e. at the
 * TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event).
 */
class ModbusWorker extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(ModbusWorker.class);

	private final MetaTasksManager<ReadTask> readTasksManager = new MetaTasksManager<>();

	/**
	 * Holds the added protocols per source Component-ID.
	 */
	private final Multimap<String, ModbusProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	/**
	 * Holds source Component-IDs that are known to have errors.
	 */
	private final Set<String> defectiveComponents = new HashSet<>();

	private static final long WAIT_TILL_NEXT_READ_DELTA_MILLIS = 10;
	private long waitTillReadHighPriorityTasks = 100;

	/*
	 * The central Deque for queuing tasks.
	 */
	private final LinkedBlockingDeque<Task> nextTasks = new LinkedBlockingDeque<>();
	private final AtomicLong lastFinishedQueue = new AtomicLong(0);
	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	private final AbstractModbusBridge parent;

	protected ModbusWorker(AbstractModbusBridge parent) {
		this.parent = parent;
	}

	private ScheduledFuture<?> addReadTasksFuture = null;

	/**
	 * This is called on TOPIC_CYCLE_BEFORE_PROCESS_IMAGE cycle event.
	 */
	protected void onBeforeProcessImage() {
		StateChannel cycleTimeIsTooShortChannel = this.parent.channel(BridgeModbus.ChannelId.CYCLE_TIME_IS_TOO_SHORT);

		/*
		 * Was Cycle-Time sufficient to empty the Queue? 'Learn' the optimal waiting
		 * time.
		 */
		long lastFinishedQueue = this.lastFinishedQueue.getAndSet(Long.MAX_VALUE);
		long delta = System.currentTimeMillis() - lastFinishedQueue;

		/*
		 * Increase, decrease or keep "waitTillReadHighPriorityTasks"
		 */
		if (delta < 0) {
			// not sufficient -> decrease waitTillReadHighPriorityTasks
			if (this.waitTillReadHighPriorityTasks < WAIT_TILL_NEXT_READ_DELTA_MILLIS) {
				this.waitTillReadHighPriorityTasks = WAIT_TILL_NEXT_READ_DELTA_MILLIS * -1;
			} else {
				this.waitTillReadHighPriorityTasks -= WAIT_TILL_NEXT_READ_DELTA_MILLIS;
			}
		} else {
			// sufficient
			if (delta > 3 * WAIT_TILL_NEXT_READ_DELTA_MILLIS) {
				// more than sufficient -> increase waitTillReadHighPriorityTasks
				this.waitTillReadHighPriorityTasks += WAIT_TILL_NEXT_READ_DELTA_MILLIS;
			} else {
				// wait time is exactly sufficient
			}
		}

		/*
		 * waiting time should never be lower than 0.
		 */
		if (this.waitTillReadHighPriorityTasks < 0) {
			// Cycle-Time is too short
			cycleTimeIsTooShortChannel.setNextValue(true);

		} else {
			cycleTimeIsTooShortChannel.setNextValue(false);
		}

		// Cycle-Time is higher than required -> schedule execution of Read-Tasks.
		if (this.addReadTasksFuture != null && !this.addReadTasksFuture.isDone()) {
			this.addReadTasksFuture.cancel(false);
		}
		this.addReadTasksFuture = this.executorService.schedule(() -> { //
			// reset the lastFinishedQueue timestamp
			this.lastFinishedQueue.set(Long.MAX_VALUE);

			// make sure to not overfill the queue
			this.nextTasks.clear();

			// Create temporary list...
			List<ReadTask> theseNextTasks = new ArrayList<>();
			// add one Once-/Low-Priority Task
			theseNextTasks.add(this.getOneLowPriorityReadTask());
			// add all High-Priority Tasks
			theseNextTasks.addAll(this.readTasksManager.getAllTasks(Priority.HIGH));
			// shuffle the list to make sure each Tasks gets executed from time to time even
			// if cycle-time is too short
			Collections.shuffle(theseNextTasks);
			// and add all Tasks to the tail of the Deque in order to execute them
			// as fast as possible.
			for (ReadTask task : theseNextTasks) {
				this.nextTasks.addFirst(task);
			}

			// Add a WaitTask as marker
			this.nextTasks.addFirst(new MarkerTask());

		}, this.waitTillReadHighPriorityTasks, TimeUnit.MILLISECONDS);
	}

	/**
	 * This is called on TOPIC_CYCLE_EXECUTE_WRITE cycle event.
	 */
	public void onExecuteWrite() {
		// Add all Write-Tasks to the tail of the Deque in order to execute them as fast
		// as possible.
		Multimap<String, WriteTask> tasks = this.getNextWriteTasks();
		for (WriteTask task : tasks.values()) {
			this.nextTasks.addLast(task);
		}
	}

	@Override
	protected void forever() throws InterruptedException {
		// get the next task or wait for a new task
		Task task = this.nextTasks.takeLast();

		// Reached the "Marker-Task" and it is the last element of the Queue?
		if (task instanceof MarkerTask && this.nextTasks.peekLast() == null) {
			this.lastFinishedQueue.set(System.currentTimeMillis());
			return;
		}

		try {
			// execute the task
			int noOfExecutedSubTasks = task.execute(this.parent);

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
			for (ModbusElement<?> element : task.getElements()) {
				element.invalidate();
			}
		}
	}

	/**
	 * Gets the Read-Tasks by Source-ID.
	 * 
	 * <p>
	 * This checks if a device is listed as defective and - if it is - adds only one
	 * ReadTask of this Source-Component to the queue
	 * 
	 * @return a list of ReadTasks by Source-ID
	 */
	private ReadTask getOneLowPriorityReadTask() {
		// Get next Priority ONCE task
		ReadTask oncePriorityTask = this.readTasksManager.getOneTask(Priority.ONCE);
		if (oncePriorityTask != null && !oncePriorityTask.hasBeenExecuted()) {
			return oncePriorityTask;

		} else {
			// No more Priority ONCE tasks available -> add Priority LOW task
			ReadTask lowPriorityTask = this.readTasksManager.getOneTask(Priority.LOW);
			if (lowPriorityTask != null) {
				return lowPriorityTask;
			}
		}
		return null;

		// TODO Handle defective components
		// // Remove all but one tasks from defective components
		// Set<String> alreadyHandledComponents = new HashSet<>();
		// for (Iterator<ReadTask> iterTasks = result.iterator(); iterTasks.hasNext();)
		// {
		// ReadTask task = iterTasks.next();
		// String componentId = task.getParent().id();
		// if (this.defectiveComponents.contains(componentId)) {
		// if (alreadyHandledComponents.contains(componentId)) {
		// iterTasks.remove();
		// } else {
		// alreadyHandledComponents.add(componentId);
		// }
		// }
		// }
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
	private Multimap<String, WriteTask> getNextWriteTasks() {
		Multimap<String, WriteTask> result = HashMultimap.create();
		for (Entry<String, Collection<ModbusProtocol>> entry : this.protocols.asMap().entrySet()) {
			String componentId = entry.getKey();

			for (ModbusProtocol protocol : entry.getValue()) {
				if (this.defectiveComponents.contains(componentId)) {
					// Component is known to be erroneous -> add only one Task
					WriteTask t = protocol.getWriteTasksManager().getOneTask();
					if (t != null) {
						result.put(componentId, t);
					}

				} else {
					// get the next write tasks from the protocol
					List<WriteTask> nextWriteTasks = protocol.getWriteTasksManager().getAllTasks();
					result.putAll(entry.getKey(), nextWriteTasks);
				}
			}
		}
		return result;
	}

	/**
	 * Adds the protocol.
	 * 
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.protocols.put(sourceId, protocol);
		this.readTasksManager.addTasksManager(protocol.getReadTasksManager());
	}

	/**
	 * Removes the protocol.
	 * 
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		Collection<ModbusProtocol> protocols = this.protocols.removeAll(sourceId);
		for (ModbusProtocol protocol : protocols) {
			this.readTasksManager.removeTasksManager(protocol.getReadTasksManager());
		}
	}

}