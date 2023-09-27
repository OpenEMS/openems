package io.openems.edge.bridge.mqtt.api.task;

import java.time.Clock;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttConnectionImpl;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.payloads.Payload;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;

/**
 * An Implementation of the {@link MqttSubscribeTask} by extending the
 * {@link AbstractMqttTask}. On Execute -> get the updated Topic from the
 * {@link MqttWorker} And
 * update the Topic it stores itself. After that, the corresponding
 * OpenEmsComponent will be updated by the
 * {@link Payload} object.
 */
public class MqttSubscribeTaskImpl extends AbstractMqttTask implements MqttSubscribeTask {

	public MqttSubscribeTaskImpl(Topic topic) {
		super(topic);
	}

	@Override
	public boolean hasTopic(Topic topic) {
		return this.topic.equals(topic);
	}

	@Override
	public boolean hasTopic(Collection<Topic> topics) {
		return topics.contains(this.topic);
	}

	@Override
	public int execute(Collection<Topic> topics, MqttConnectionImpl mqttConnection, Clock clock)
			throws OpenemsException {
		this.stopwatch.reset();
		this.stopwatch.start();
		topics.stream() //
				.filter(topicInCollection -> topicInCollection.getTopicName().equals(this.topic.getTopicName())) //
				.findAny().ifPresent(this::updatePayloadAndUpdate);
		this.lastExecuteDuration = this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
		return 1;
	}

	/**
	 * Called from the
	 * {@link MqttTask#execute(Collection, MqttConnectionImpl, Clock)} method. After
	 * finding the corresponding topic, update the stored
	 * {@link Payload} within
	 * the {@link Topic} object and update the reference Component by calling the
	 * {@link MqttComponent#getReferenceComponent()}. method.
	 * 
	 * @param topic the corresponding {@link Topic}, received from the updated Topic
	 *              collection.
	 */
	private void updatePayloadAndUpdate(Topic topic) {
		this.topic.getPayload().updatePayloadAfterCallback(topic.getPayload());
		var comp = this.parent.get().getReferenceComponent();
		this.topic.getPayload().handlePayloadToComponent(comp);
	}

	@Override
	public int _execute(MqttConnectionImpl mqttConnection, Clock clock) throws OpenemsException {
		throw new OpenemsException("Execute of SubscribeTask failed: use the other execute Method instead");
	}

	@Override
	public void deactivate() {
		// unused atm
	}
}
