package io.openems.edge.bridge.mqtt.api.task;

import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.openems.edge.bridge.mqtt.api.MqttConnectionImpl;
import io.openems.edge.bridge.mqtt.api.Topic;
import org.eclipse.paho.client.mqttv3.MqttException;

import io.openems.common.exceptions.OpenemsException;

/**
 * The basic implementation of the {@link MqttPublishTask}. By giving a
 * pushInterval, this task pushes every X amount of Seconds to the Broker. Keep
 * in mind, that the current {@link Clock#instant()} is converted to a Time.
 * When you need to set defined minute intervals/push with a "nice" clock layout
 * consider using {@link MqttPublishTaskFixedMinuteTimeImpl}.
 */
public class MqttPublishTaskImpl extends AbstractMqttTask implements MqttPublishTask {

	protected boolean pushSuccessful;
	protected int pushIntervalSeconds;

	public MqttPublishTaskImpl(Topic topic, int pushInterval) {
		super(topic);
		this.pushIntervalSeconds = pushInterval;
	}

	@Override
	public boolean shouldBePublished(Clock clock) {
		return lastPush == null || clock.instant().isAfter(this.lastPush.plusSeconds(this.pushIntervalSeconds))
				|| !this.pushSuccessful;
	}

	@Override
	public final synchronized int execute(Collection<Topic> topics, MqttConnectionImpl mqttConnection, Clock clock)
			throws OpenemsException {
		this.stopwatch.reset();
		this.stopwatch.start();
		try {
			return this._execute(mqttConnection, clock);
		} finally {
			this.lastExecuteDuration = this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public int _execute(MqttConnectionImpl mqttConnection, Clock clock) throws OpenemsException {
		// build payload
		// mqttConnection -> push to topic
		try {
			var inst = clock.instant();
			this.topic.getPayload().setTime(inst.atZone(UTC));
			this.topic.getPayload().build(this.parent.get().getReferenceComponent());
			mqttConnection.publish(this.topic);
			this.pushSuccessful = true;
			this.lastPush = inst;
		} catch (MqttException e) {
			this.pushSuccessful = false;
			throw new OpenemsException("Publish failed: " + e.getMessage(), e);
		}
		return 1;
	}

	@Override
	public void deactivate() {
		// unused atm
	}

}
