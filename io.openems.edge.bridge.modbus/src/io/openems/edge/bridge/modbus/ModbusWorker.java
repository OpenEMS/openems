package io.openems.edge.bridge.modbus;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;

class ModbusWorker extends AbstractCycleWorker {

	private final Logger log = LoggerFactory.getLogger(ModbusWorker.class);

	/**
	 * Holds the added protocols per source Component-ID.
	 */
	private final Multimap<String, ModbusProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	/**
	 * Holds source Component-IDs that are known to have errors.
	 */
	private final Set<String> errorComponents = new HashSet<>();

	/**
	 * Set ForceWrite to interrupt the ReadTasks and execute the WriteTasks
	 * immediately.
	 */
	private final AtomicBoolean forceWrite = new AtomicBoolean(false);
	private final AbstractModbusBridge parent;

	protected ModbusWorker(AbstractModbusBridge parent) {
		this.parent = parent;
	}

	@Override
	public void triggerNextRun() {
		this.forceWrite.set(true);
		super.triggerNextRun();
	}

	@Override
	protected void forever() {
		boolean isCommunicationFailed = false;

		// get the read tasks for this run
		Multimap<String, ReadTask> nextReadTasks = this.getNextReadTasks();

		/*
		 * execute next read tasks
		 */
		for (Entry<String, Collection<ReadTask>> readEntry : nextReadTasks.asMap().entrySet()) {
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
							this.errorComponents.remove(componentId);

						} catch (OpenemsException e) {
							this.parent.logError(this.log, writeTask.toString() + " write failed: " + e.getMessage());

							// mark this component as erroneous
							this.errorComponents.add(componentId);

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
				String componentId = readEntry.getKey();
				for (ReadTask readTask : readEntry.getValue()) {
					try {
						// execute the task
						readTask.executeQuery(this.parent);

						// remove this component from erroneous list
						this.errorComponents.remove(componentId);

					} catch (OpenemsException e) {
						this.parent.logWarn(this.log, readTask.toString() + " read failed: " + e.getMessage());

						// mark this component as erroneous
						this.errorComponents.add(componentId);

						// remember that at least one communication failed
						isCommunicationFailed = true;

						// invalidate elements of this task
						for (ModbusElement<?> element : readTask.getElements()) {
							element.invalidate();
						}
					}
				}
			}
		}

		// Set the "SlaveCommunicationFailed" State-Channel
		this.parent.getSlaveCommunicationFailedChannel().setNextValue(isCommunicationFailed);
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
	private Multimap<String, ReadTask> getNextReadTasks() {
		Multimap<String, ReadTask> result = HashMultimap.create();
		for (Entry<String, Collection<ModbusProtocol>> entry : this.protocols.asMap().entrySet()) {
			String componentId = entry.getKey();

			for (ModbusProtocol protocol : entry.getValue()) {
				if (this.errorComponents.contains(componentId)) {
					// Component is known to be erroneous -> add only one Task
					ReadTask t = protocol.getOneReadTask();
					if (t != null) {
						result.put(componentId, t);
					}

				} else {
					// get the next read tasks from the protocol
					List<ReadTask> nextReadTasks = protocol.getNextReadTasks();
					result.putAll(entry.getKey(), nextReadTasks);
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
				if (this.errorComponents.contains(componentId)) {
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
	}

	/**
	 * Removes the protocol.
	 * 
	 * @param sourceId Component-ID of the source
	 */
	public void removeProtocol(String sourceId) {
		this.protocols.removeAll(sourceId);
	}
}