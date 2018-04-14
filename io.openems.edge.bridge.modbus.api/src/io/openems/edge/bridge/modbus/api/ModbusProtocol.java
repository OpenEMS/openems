package io.openems.edge.bridge.modbus.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.Priority;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;

public class ModbusProtocol {

	private final Logger log = LoggerFactory.getLogger(ModbusProtocol.class);

	/**
	 * The Unit-ID for this Modbus connection
	 */
	private final int unitId;

	/**
	 * All ReadTasks by their Priority
	 */
	private final ArrayListMultimap<Priority, ReadTask> readTasks = ArrayListMultimap.create();

	/**
	 * Next queue of ReadTasks
	 */
	private final ArrayListMultimap<Priority, ReadTask> nextReadTasks = ArrayListMultimap.create();

	/**
	 * All WriteTasks
	 */
	private final List<WriteTask> writeTasks = new ArrayList<>();

	public ModbusProtocol(int unitId, ReadTask... readTasks) {
		this.unitId = unitId;
		for (ReadTask readTask : readTasks) {
			addReadTask(readTask);
		}
	}

	public int getUnitId() {
		return unitId;
	}

	public void addReadTask(ReadTask readTask) {
		// add the unitId to the task
		readTask.setUnitId(this.unitId);
		// check task for plausibility
		this.checkTask(readTask);
		/*
		 * fill writeTasks
		 */
		if (readTask instanceof WriteTask) {
			WriteTask writetask = (WriteTask) readTask;
			this.writeTasks.add(writetask);
		}
		/*
		 * fill readTasks
		 */
		// find highest priority of an element in the Task
		Priority highestPriorityInTask = Priority.LOW;
		for (ModbusElement<?> element : readTask.getElements()) {
			if (element.getPriority().compareTo(highestPriorityInTask) > 0) {
				highestPriorityInTask = element.getPriority();
			}
		}
		this.readTasks.put(highestPriorityInTask, readTask);
	}

	/**
	 * Returns the next list of ReadTasks that should be executed within one cycle
	 * 
	 * @return
	 */
	public List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>(this.readTasks.get(Priority.HIGH).size() + 1);
		this.readTasks.keySet().forEach(priority -> {
			/*
			 * Evaluates, how many tasks should be taken per priority for the result
			 */
			int take = 0; // how many tasks should be taken?
			switch (priority) {
			case HIGH:
				take = -1; // take all tasks
				break;
			case LOW:
				take = 1; // take one task
				break;
			}
			/*
			 * Apply the 'take' value
			 */
			if (take == 0) {
				// add none
			} else if (take < 0) {
				// take all read tasks
				result.addAll(this.readTasks.get(priority));
			} else {
				synchronized (this.nextReadTasks) {
					if (this.nextReadTasks.get(priority).size() == 0) {
						// refill the queue
						this.nextReadTasks.putAll(priority, this.readTasks.get(priority));
					}
					// take the given number of tasks; using nextReadTasks as a buffer queue.
					Iterator<ReadTask> iter = this.nextReadTasks.get(priority).iterator();
					for (int i = 0; i < take && iter.hasNext(); i++) {
						result.add(iter.next());
						iter.remove();
					}
				}
			}
		});
		return result;
	}

	/**
	 * Returns the next list of WriteTasks that should be executed within one cycle
	 * 
	 * @return
	 */
	public List<WriteTask> getNextWriteTasks() {
		return this.writeTasks;
	}

	/**
	 * Checks a {@link ReadTask} for plausibility
	 *
	 * @param readTask
	 */
	private void checkTask(ReadTask readTask) {
		int address = readTask.getStartAddress();
		for (ModbusElement<?> element : readTask.getElements()) {
			if (element.getStartAddress() != address) {
				log.error("Start address is wrong. It is [" + element.getStartAddress() + "/0x"
						+ Integer.toHexString(element.getStartAddress()) + "] but should be [" + address + "/0x"
						+ Integer.toHexString(address) + "].");
			}
			address += element.getLength();
			// TODO: check BitElements
		}
	}
}
