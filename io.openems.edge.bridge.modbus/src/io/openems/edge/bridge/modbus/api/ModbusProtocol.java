package io.openems.edge.bridge.modbus.api;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WriteTask;

public class ModbusProtocol {

	private final Logger log = LoggerFactory.getLogger(ModbusProtocol.class);

	/**
	 * The Unit-ID for this Modbus connection
	 */
	private final int unitId;

	/**
	 * All ReadTasks with HIGH priority are always executed
	 */
	private final List<ReadTask> readTasksHighPrio = new ArrayList<>();

	/**
	 * ReadTasks with LOW priority. One at a time is executed
	 */
	private final List<ReadTask> readTasksLowPrio = new ArrayList<>();

	/**
	 * ReadTasks with ONCE priority are executed only once and only one at a time
	 */
	private final List<ReadTask> readTasksOncePrio = new ArrayList<>();

	/**
	 * Next queue of LOW priority ReadTasks
	 */
	private final Queue<ReadTask> nextReadTasksLowPrio = new LinkedList<>();

	/**
	 * Next queue of ONCE priority ReadTasks
	 */
	private final Queue<ReadTask> nextReadTasksOncePrio = new LinkedList<>();

	/**
	 * All WriteTasks
	 */
	private final List<WriteTask> writeTasks = new ArrayList<>();

	public ModbusProtocol(int unitId, Task... tasks) {
		this.unitId = unitId;
		for (Task task : tasks) {
			addTask(task);
		}
	}

	public int getUnitId() {
		return unitId;
	}

	public synchronized void addTask(Task task) {
		// add the unitId to the abstractTask
		task.setUnitId(this.unitId);
		// check abstractTask for plausibility
		this.checkTask(task);
		/*
		 * fill writeTasks
		 */
		if (task instanceof WriteTask) {
			WriteTask writeTask = (WriteTask) task;
			this.writeTasks.add(writeTask);
		}
		/*
		 * fill readTasks
		 */
		if (task instanceof ReadTask) {
			ReadTask readTask = (ReadTask) task;
			switch (readTask.getPriority()) {
			case HIGH:
				this.readTasksHighPrio.add(readTask);
				break;
			case LOW:
				this.readTasksLowPrio.add(readTask);
				break;
			case ONCE:
				this.readTasksOncePrio.add(readTask);
				break;
			}
		}
		// prefill nextReadTasksOncePrio
		this.nextReadTasksOncePrio.addAll(this.readTasksOncePrio);
	}

	/**
	 * Returns the next list of ReadTasks that should be executed within one cycle
	 * 
	 * @return
	 */
	public synchronized List<ReadTask> getNextReadTasks() {
		List<ReadTask> result = new ArrayList<>();
		for (Priority priority : Priority.values()) {
			switch (priority) {
			case HIGH:
				/*
				 * HIGH priority -> always take everything
				 */
				result.addAll(this.readTasksHighPrio);
				break;
			case LOW: {
				/*
				 * LOW priority -> one at a time. Start again (refill) when empty
				 */
				if (this.nextReadTasksLowPrio.isEmpty()) {
					this.nextReadTasksLowPrio.addAll(this.readTasksLowPrio);
				}
				ReadTask task = this.nextReadTasksLowPrio.poll();
				if (task != null) {
					result.add(task);
				}
			}
				break;
			case ONCE: {
				/*
				 * ONCE priority -> one at a time till queue is empty
				 */
				ReadTask task = this.nextReadTasksLowPrio.poll();
				if (task != null) {
					result.add(task);
				}
			}
				break;
			}
		}
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
	 * Checks a {@link AbstractTask} for plausibility
	 *
	 * @param task
	 */
	private synchronized void checkTask(Task task) {
		int address = task.getStartAddress();
		for (ModbusElement<?> element : task.getElements()) {
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
