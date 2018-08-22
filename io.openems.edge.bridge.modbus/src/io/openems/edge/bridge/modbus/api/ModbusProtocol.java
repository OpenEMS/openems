package io.openems.edge.bridge.modbus.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.taskmanager.TaskManager;

public class ModbusProtocol {

	private final Logger log = LoggerFactory.getLogger(ModbusProtocol.class);

	/**
	 * The Unit-ID for this Modbus connection
	 */
	private final int unitId;

	/**
	 * TaskManager for ReadTasks
	 */
	private final TaskManager<ReadTask> readTaskManager = new TaskManager<>();

	/**
	 * TaskManager for WriteTasks
	 */
	private final TaskManager<WriteTask> writeTaskManager = new TaskManager<>();

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
			this.writeTaskManager.addTask((WriteTask) task);
		}
		/*
		 * fill readTaskManager
		 */
		if (task instanceof ReadTask) {
			this.readTaskManager.addTask((ReadTask) task);
		}
	}

	public synchronized void removeTask(Task task) {
	}

	/**
	 * Returns the next list of WriteTasks that should be executed within one cycle
	 * 
	 * @return
	 */
	public List<WriteTask> getNextWriteTasks() {
		return this.writeTaskManager.getNextReadTasks();
	}
	
	/**
	 * Returns the next list of ReadTasks that should be executed within one cycle
	 * 
	 * @return
	 */
	public List<ReadTask> getNextReadTasks() {
		return this.readTaskManager.getNextReadTasks();
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
