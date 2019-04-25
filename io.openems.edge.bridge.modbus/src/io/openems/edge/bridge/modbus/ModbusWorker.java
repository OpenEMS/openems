package io.openems.edge.bridge.modbus;

import java.util.Collection;
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
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

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

	/**
	 * Holds the added protocols per source Component-ID.
	 */
	private final Multimap<String, ModbusProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	/**
	 * TaskManager for ReadTasks from all Protocols.
	 */
	private final TasksManager<ReadTask> readTaskManager = new TasksManager<>();

	/**
	 * Holds source Component-IDs that are known to have errors.
	 */
	private final Set<String> defectiveComponents = new HashSet<>();

	private final static long WAIT_TILL_NEXT_READ_DELTA_MILLIS = 10;
	private long waitTillReadHighPriorityTasks = 300;

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
		if (delta >= 0) {
			// sufficient
			if (delta > 2 * WAIT_TILL_NEXT_READ_DELTA_MILLIS) {
				// more than sufficient -> increase waitTillReadHighPriorityTasks
				this.waitTillReadHighPriorityTasks += WAIT_TILL_NEXT_READ_DELTA_MILLIS;
			} else {
				// wait time is exactly sufficient
			}

		} else {
			// not sufficient -> decrease waitTillReadHighPriorityTasks
			if (this.waitTillReadHighPriorityTasks < WAIT_TILL_NEXT_READ_DELTA_MILLIS) {
				this.waitTillReadHighPriorityTasks = WAIT_TILL_NEXT_READ_DELTA_MILLIS * -1;
			} else {
				this.waitTillReadHighPriorityTasks -= WAIT_TILL_NEXT_READ_DELTA_MILLIS;
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
			this.lastFinishedQueue.set(Long.MAX_VALUE);

			// Add one Low-Priority Task
			this.nextTasks.addFirst(this.getOneLowPriorityReadTask());

			// Add all High-Priority Tasks to the tail of the Deque in order to execute them
			// as fast as possible.
			if (cycleTimeIsTooShortChannel.value().orElse(false) == true) {
				// Cycle-Time is too short: add only one
				ReadTask task = this.readTaskManager.getOneTask(Priority.HIGH);
				if (task != null) {
					this.nextTasks.addFirst(task);
				}
			} else {
				// add all
				for (ReadTask task : this.readTaskManager.getTasks(Priority.HIGH)) {
					this.nextTasks.addFirst(task);
				}
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
			task.execute(this.parent);

			// no exception -> remove this component from erroneous list and set the
			// CommunicationFailedChannel to false
			if (task.getParent() != null) {
				this.defectiveComponents.remove(task.getParent().id());
			}
			this.parent.getSlaveCommunicationFailedChannel().setNextValue(false);

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
		ReadTask oncePriorityTask = this.readTaskManager.getOneTask(Priority.ONCE);
		if (oncePriorityTask != null) {
			return oncePriorityTask;

		} else {
			// No more Priority ONCE tasks available -> add Priority LOW task
			ReadTask lowPriorityTask = this.readTaskManager.getOneTask(Priority.LOW);
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
					WriteTask t = protocol.getOneWriteTask();
					if (t != null) {
						result.put(componentId, t);
					}

				} else {
					// get the next read tasks from the protocol
					List<WriteTask> nextWriteTasks = protocol.getNextWriteTasks();
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
		this.updateReadTasksManager();
	}

	/**
	 * Removes the protocol.
	 * 
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		Collection<ModbusProtocol> protocols = this.protocols.removeAll(sourceId);
		this.updateReadTasksManager();
		for (ModbusProtocol protocol : protocols) {
			protocol.deactivate();
		}
	}

	/**
	 * Updates the global Read-Tasks Manager from ModbusProtocols.
	 */
	private synchronized void updateReadTasksManager() {
		this.readTaskManager.clearAll();
		for (ModbusProtocol protocol : this.protocols.values()) {
			this.readTaskManager.addTasks(protocol.getReadTasksManager().getAllTasks());
		}
	}

}