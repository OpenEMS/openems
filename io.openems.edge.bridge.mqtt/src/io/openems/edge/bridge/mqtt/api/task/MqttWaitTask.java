package io.openems.edge.bridge.mqtt.api.task;

import java.time.Clock;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttConnectionImpl;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;
import io.openems.edge.common.taskmanager.Priority;

/**
 * A WaitTask, sets the {@link MqttWorker} to sleep, when to tasks are given.
 */
public class MqttWaitTask implements MqttTask {
	private final Logger log = LoggerFactory.getLogger(MqttWaitTask.class);
	private final long delay;
	private MqttComponent parent = null;

	public MqttWaitTask(long delay) {
		this.delay = delay;
	}

	@Override
	public Topic getTopic() {
		return null;
	}

	@Override
	public long getExecuteDuration() {
		return 0;
	}

	@Override
	public int execute(Collection<Topic> topics, MqttConnectionImpl mqttConnection, Clock clock) {
		return this._execute(mqttConnection, clock);
	}

	@Override
	public int _execute(MqttConnectionImpl mqttConnection, Clock clock) {
		try {
			Thread.sleep(this.delay);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}
		return 0;
	}

	@Override
	public MqttComponent getParent() {
		return this.parent;
	}

	@Override
	public void _setParent(MqttComponent parent) {
		this.parent = parent;
	}

	@Override
	public void deactivate() {

	}

	@Override
	public Priority getPriority() {
		return Priority.LOW;
	}
}
