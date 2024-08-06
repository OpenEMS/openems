package io.openems.edge.bridge.mqtt.api.task;

import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;

import java.util.Collection;

/**
 * This Interface extends the {@link MqttTask}. This is mostly important for the
 * {@link MqttWorker}. Whenever a Callback of Subscribed topics is received, the
 * Worker checks if the {@link MqttSubscribeTask} has subscribed to a
 * {@link Topic}, that was updated.
 */
public interface MqttSubscribeTask extends MqttTask {

	/**
	 * This method asks the {@link MqttSubscribeTask} if it listens to the given
	 * topic ({@link Topic#getTopicName()}).
	 * 
	 * @param topic the {@link Topic}.
	 * @return a Boolean
	 */
	boolean hasTopic(Topic topic);

	/**
	 * Usually called by the {@link MqttWorker}. The Worker asks the Task, if they
	 * listen to a topic, that were updated.
	 * 
	 * @param topics the updated Topics.
	 * @return a Boolean
	 */
	boolean hasTopic(Collection<Topic> topics);
}
