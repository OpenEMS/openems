package io.openems.edge.bridge.mqtt.api;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.task.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.task.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.task.MqttTask;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;
import io.openems.edge.common.taskmanager.TasksManager;

/**
 * The MqttProtocol, oriented to the ModbusProtocol. This protocol is created by
 * each OpenEmsComponent extending the
 * {@link AbstractOpenEmsMqttComponent}. It provides collections of publish and
 * subscribe Tasks. Those tasks are handled by the
 * {@link MqttWorker}.
 */
public class MqttProtocol {

	/**
	 * The Parent component.
	 */
	private final MqttComponent parent;

	/**
	 * TaskManager for MqttSubscribeTasks.
	 */
	private final TasksManager<MqttSubscribeTask> mqttSubscribeTaskManager = new TasksManager<>();

	/**
	 * TaskManager for MqttPublishTasks.
	 */
	private final TasksManager<MqttPublishTask> mqttPublishTaskManager = new TasksManager<>();

	/**
	 * Creates a new {@link MqttProtocol}.
	 *
	 * @param parent the {@link MqttComponent} parent
	 * @param tasks  the {@link MqttTask}s
	 * @throws OpenemsException on error
	 */
	public MqttProtocol(MqttComponent parent, MqttTask... tasks) throws OpenemsException {
		this.parent = parent;
		for (MqttTask task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds Tasks to the Protocol.
	 *
	 * @param tasks the tasks
	 * @throws OpenemsException on error
	 */
	public synchronized void addTasks(MqttTask... tasks) throws OpenemsException {
		for (MqttTask task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds a Task to the Protocol.
	 *
	 * @param task the task
	 * @throws OpenemsException on plausibility error
	 */
	public synchronized void addTask(MqttTask task) throws OpenemsException {
		// add the parent to the Task
		task._setParent(this.parent);
		/*
		 * fill writeTasks
		 */
		if (task instanceof MqttPublishTask) {
			this.mqttPublishTaskManager.addTask((MqttPublishTask) task);
		}
		/*
		 * fill readTaskManager
		 */
		if (task instanceof MqttSubscribeTask) {
			this.mqttSubscribeTaskManager.addTask((MqttSubscribeTask) task);
		}
	}

	/**
	 * Removes a Task from the Protocol.
	 *
	 * @param task the task
	 */
	public synchronized void removeTask(MqttTask task) {
		if (task instanceof MqttSubscribeTask) {
			this.mqttSubscribeTaskManager.removeTask((MqttSubscribeTask) task);
		}
		if (task instanceof MqttPublishTask) {
			this.mqttPublishTaskManager.removeTask((MqttPublishTask) task);
		}
	}

	/**
	 * Gets the Read-Tasks Manager.
	 *
	 * @return a the TaskManager
	 */
	public TasksManager<MqttSubscribeTask> getSubscribeTaskManager() {
		return this.mqttSubscribeTaskManager;
	}

	/**
	 * Gets the Write-Tasks Manager.
	 *
	 * @return a writeTaskManager
	 */
	public TasksManager<MqttPublishTask> getPublishTaskManager() {
		return this.mqttPublishTaskManager;
	}

	/**
	 * Deactivate the {@link MqttProtocol}.
	 */
	public void deactivate() {
		var readTasks = this.mqttSubscribeTaskManager.getTasks();
		for (MqttSubscribeTask readTask : readTasks) {
			readTask.deactivate();
		}

		var writeTasks = this.mqttPublishTaskManager.getTasks();
		for (MqttPublishTask writeTask : writeTasks) {
			writeTask.deactivate();
		}
	}
}
