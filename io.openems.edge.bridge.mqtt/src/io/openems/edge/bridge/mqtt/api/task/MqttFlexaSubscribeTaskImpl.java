package io.openems.edge.bridge.mqtt.api.task;

import java.time.Clock;
import java.util.Collection;

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
public class MqttFlexaSubscribeTaskImpl extends MqttSubscribeTaskImpl implements MqttSubscribeTask {

	public MqttFlexaSubscribeTaskImpl(Topic topic) {
		super(topic);
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
	@SuppressWarnings("unused")
	private void updatePayloadAndUpdate(Topic topic) {
		this.topic.getPayload().updatePayloadAfterCallback(topic.getPayload());
		var comp = this.parent.get();
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
