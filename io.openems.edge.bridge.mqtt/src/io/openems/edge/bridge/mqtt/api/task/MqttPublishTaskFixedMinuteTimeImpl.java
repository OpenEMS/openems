package io.openems.edge.bridge.mqtt.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.MqttConnectionImpl;
import io.openems.edge.bridge.mqtt.api.Topic;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This Class extends the existing {@link MqttPublishTaskImpl}. This tasks
 * allows a fixed interval time. E.g. the base MqttPublish Tasks pushes its
 * payload after X amounts of seconds. However the FixedMinute looks at the real
 * clock time an tries to push every X minute on the hour. Comparison/Example:
 * Base Publish Tasks -> pushes every 15 minutes -> if its 15:03 -> next push
 * 15:18 -> next push 15:33 Fixed Minute -> pushes every 15 minutes -> if its
 * 15:03 -> next push 15:15 -> next push 15:30 This is important when collecting
 * data of a meter where 15 min intervals are important.
 */
public class MqttPublishTaskFixedMinuteTimeImpl extends MqttPublishTaskImpl {

	private ZonedDateTime nextPublishTime;

	public MqttPublishTaskFixedMinuteTimeImpl(Topic topic, int pushInterval) {
		super(topic, pushInterval);
	}

	@Override
	public boolean shouldBePublished(Clock clock) {
		var now = ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
		if (this.lastPush == null) {
			this.calculateNextPubTime(now.minusMinutes(this.pushIntervalSeconds / 60));
		}
		return now.isAfter(this.nextPublishTime);

	}

	/**
	 * Calculates the next Publish time, by calculating the minute interval,
	 * truncating the time to hours, adding a minute interval and then truncating
	 * the current minutes.
	 * 
	 * @param now the current time, usually provided by the {@link Clock#instant()}.
	 */
	private void calculateNextPubTime(ZonedDateTime now) {
		var minuteInterval = this.pushIntervalSeconds / 60;
		this.nextPublishTime = now.truncatedTo(ChronoUnit.HOURS).plusMinutes(minuteInterval)
				.plusMinutes((long) minuteInterval * (now.getMinute() / minuteInterval));

	}

	@Override
	public int _execute(MqttConnectionImpl mqttConnection, Clock clock) throws OpenemsException {
		// build payload
		// mqttConnection -> push to topic
		try {
			this.topic.getPayload().setTime(this.nextPublishTime);
			this.topic.getPayload().build(this.parent.get().getReferenceComponent());
			mqttConnection.publish(this.topic);
			this.pushSuccessful = true;
			this.lastPush = clock.instant();
			this.calculateNextPubTime(ZonedDateTime.ofInstant(this.lastPush, ZoneOffset.UTC));
		} catch (MqttException e) {
			this.pushSuccessful = false;
			throw new OpenemsException("Publish failed: " + e.getMessage(), e);
		}
		return 1;
	}
}
