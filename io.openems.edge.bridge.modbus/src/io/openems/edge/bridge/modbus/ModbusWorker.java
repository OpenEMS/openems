package io.openems.edge.bridge.modbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

class ModbusWorker extends AbstractCycleWorker {

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

	/**
	 * Set ForceWrite to interrupt the ReadTasks and execute the WriteTasks
	 * immediately.
	 */
	private final AtomicBoolean forceWrite = new AtomicBoolean(false);

	private final AtomicBoolean isCurrentlyRunning = new AtomicBoolean(false);

	private final AtomicInteger cycleTimeIsTooShortCounter = new AtomicInteger(0);

	/**
	 * How many Cycle times exceeded till CycleTimeIsTooShort-Channel is set?.
	 */
	private final static int CYCLE_TIME_IS_TOO_SHORT_THRESHOLD = 10;

	/**
	 * Counts the failed communications in a row. It is used so that
	 * SLAVE_COMMUNICATION_FAILED is not set on each single error.
	 */
	private final AtomicInteger communicationFailedCounter = new AtomicInteger(0);
	private final static int FAILED_COMMUNICATIONS_FOR_ERROR = 5;

	private final AbstractModbusBridge parent;

	protected ModbusWorker(AbstractModbusBridge parent) {
		this.parent = parent;
	}

	@Override
	public void triggerNextRun() {
		this.handleCycleTimeIsTooShortChannel();
		this.forceWrite.set(true);
		super.triggerNextRun();
	}

	/**
	 * Set CycleTimeIsToShort-Channel
	 */
	private void handleCycleTimeIsTooShortChannel() {
		boolean cycleTimeIsTooShort;
		if (this.isCurrentlyRunning.get()) {
			cycleTimeIsTooShort = this.cycleTimeIsTooShortCounter
					.incrementAndGet() >= CYCLE_TIME_IS_TOO_SHORT_THRESHOLD;
		} else {
			this.cycleTimeIsTooShortCounter.set(0);
			cycleTimeIsTooShort = false;
		}
		this.parent.channel(BridgeModbus.ChannelId.CYCLE_TIME_IS_TOO_SHORT).setNextValue(cycleTimeIsTooShort);
	}

	@Override
	protected void forever() {
		this.isCurrentlyRunning.set(true);
		boolean isCommunicationFailed = false;

		// get the read tasks for this run
		List<ReadTask> nextReadTasks = this.getNextReadTasks();

		/*
		 * execute next read tasks
		 */
		for (ReadTask readTask : nextReadTasks) {
			/*
			 * was FORCE WRITE set? -> execute WriteTasks now
			 */
			if (this.forceWrite.getAndSet(false)) {
				Multimap<String, WriteTask> writeTasks = this.getNextWriteTasks();
				for (Entry<String, Collection<WriteTask>> writeEntry : writeTasks.asMap().entrySet()) {
					String componentId = writeEntry.getKey();
					for (WriteTask writeTask : writeEntry.getValue()) {
						try {
							// execute the task
							writeTask.executeWrite(this.parent);

							// remove this component from erroneous list
							this.defectiveComponents.remove(componentId);

						} catch (OpenemsException e) {
							this.parent.logError(this.log, writeTask.toString() + " write failed: " + e.getMessage());

							// mark this component as erroneous
							this.defectiveComponents.add(componentId);

							// remember that at least one communication failed
							isCommunicationFailed = true;
						}
					}
				}
			}

			/*
			 * Execute next Read-Task
			 */
			{
				try {
					// execute the task
					readTask.executeQuery(this.parent);

					// remove this component from erroneous list
					this.defectiveComponents.remove(readTask.getParent().id());

				} catch (OpenemsException e) {
					this.parent.logWarn(this.log, readTask.toString() + " read failed: " + e.getMessage());

					// mark this component as erroneous
					this.defectiveComponents.add(readTask.getParent().id());

					// remember that at least one communication failed
					isCommunicationFailed = true;

					// invalidate elements of this task
					for (ModbusElement<?> element : readTask.getElements()) {
						element.invalidate();
					}
				}
			}
		}

		/*
		 * did communication fail?
		 */
		if (isCommunicationFailed) {
			if (this.communicationFailedCounter.incrementAndGet() > FAILED_COMMUNICATIONS_FOR_ERROR) {
				// Set the "SLAVE_COMMUNICATION_FAILED" State-Channel
				this.parent.getSlaveCommunicationFailedChannel().setNextValue(true);
			} else {
				// Unset the "SLAVE_COMMUNICATION_FAILED" State-Channel
				this.parent.getSlaveCommunicationFailedChannel().setNextValue(false);
			}
		} else {
			// Reset Counter
			this.communicationFailedCounter.set(0);
			// Unset the "SLAVE_COMMUNICATION_FAILED" State-Channel
			this.parent.getSlaveCommunicationFailedChannel().setNextValue(false);
		}

		this.isCurrentlyRunning.set(false);
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
	private List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>();

		// Get next Priority ONCE task
		ReadTask oncePriorityTask = this.readTaskManager.getOneTask(Priority.ONCE);
		if (oncePriorityTask != null) {
			result.add(oncePriorityTask);
		} else {

			// No more Priority ONCE tasks available -> add Priority LOW task
			ReadTask lowPriorityTask = this.readTaskManager.getOneTask(Priority.LOW);
			if (lowPriorityTask != null) {
				result.add(lowPriorityTask);
			}
		}

		// Add all Priority HIGH tasks
		result.addAll(this.readTaskManager.getTasks(Priority.HIGH));

		// Remove all but one tasks from defective components
		Set<String> alreadyHandledComponents = new HashSet<>();
		for (Iterator<ReadTask> iterTasks = result.iterator(); iterTasks.hasNext();) {
			ReadTask task = iterTasks.next();
			String componentId = task.getParent().id();
			if (this.defectiveComponents.contains(componentId)) {
				if (alreadyHandledComponents.contains(componentId)) {
					iterTasks.remove();
				} else {
					alreadyHandledComponents.add(componentId);
				}
			}
		}

		return result;
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