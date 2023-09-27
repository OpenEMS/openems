package io.openems.edge.bridge.mqtt.api.task;

import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;

import java.time.Clock;

/**
 * An Interface extending the {@link MqttTask} by providing the method
 * {@link #shouldBePublished(Clock)} This tells the
 * {@link MqttWorker} if the
 * corresponding task needs to be handled / put into the Task Queue.
 */
public interface MqttPublishTask extends MqttTask {

	/**
	 * Tells the
	 * {@link MqttWorker} if this
	 * task is ready to be published.
	 * 
	 * @param clock the {@link Clock} usually from the
	 *              {@link io.openems.edge.common.component.ComponentManager}.
	 * @return a Boolean
	 */
	boolean shouldBePublished(Clock clock);
}
