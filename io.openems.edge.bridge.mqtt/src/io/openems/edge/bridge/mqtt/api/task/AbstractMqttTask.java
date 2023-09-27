package io.openems.edge.bridge.mqtt.api.task;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Stopwatch;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.common.taskmanager.Priority;

/**
 * The AbstractClass for a {@link MqttTask}. It provides the ability to measure
 * its execution time (publish/subscribe time), stores its {@link MqttComponent}
 * parent and the Topic. Additionally, it stores its Time of the last Push. This
 * is only used atm within publish tasks. This is used to determine, if the
 * PublishTasks should update its payload and push its data to the Broker. (e.g.
 * push every 100 seconds -> are the 100 seconds up).
 */
public abstract class AbstractMqttTask implements MqttTask {

	protected static final long DEFAULT_EXECUTION_DURATION = 100;
	protected final Stopwatch stopwatch = Stopwatch.createUnstarted();

	protected Instant lastPush;
	protected Topic topic;

	protected AtomicReference<MqttComponent> parent = new AtomicReference<>();
	protected long lastExecuteDuration = DEFAULT_EXECUTION_DURATION;

	protected AbstractMqttTask(Topic topic) {
		this.topic = topic;
	}

	@Override
	public Topic getTopic() {
		return this.topic;
	}

	@Override
	public long getExecuteDuration() {
		return this.lastExecuteDuration;
	}

	// Currently unused -> mqttTasks do not use Priorities
	@Override
	public Priority getPriority() {
		return Priority.HIGH;
	}

	@Override
	public MqttComponent getParent() {
		return this.parent.get();
	}

	@Override
	public void _setParent(MqttComponent parent) {
		this.parent.set(parent);
	}
}
