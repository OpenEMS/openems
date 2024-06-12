package io.openems.edge.bridge.mqtt.api.payloads;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.task.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.task.MqttSubscribeTaskImpl;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;
import io.openems.edge.common.component.OpenemsComponent;

public interface Payload {

	// Those are usually Used by the Broker

	/**
	 * This method is called by the {@link MqttWorker}. After Subscribing to a Topic
	 * and receiving a Callback with the Broker Payload as a String, the internal
	 * Payload will be updated.
	 * 
	 * @param payload the payload from the Broker as a String.
	 */
	void updatePayloadAfterCallback(String payload);

	/**
	 * This method is called by the {@link MqttWorker}. After Subscribing to a Topic
	 * and receiving a Callback with the Broker Payload as a String, the internal
	 * Payload will be updated.
	 * 
	 * @param payload the payload from the Broker as a JsonObject.
	 */
	void updatePayloadAfterCallback(JsonObject payload);

	// those are called by the subscribetask -> updates the worker payload to
	// subscribe payload

	/**
	 * This method is usually Called by the {@link MqttSubscribeTaskImpl} After
	 * receiving an update of the {@link MqttWorker} The SubscribeTask updates its
	 * Payloads. The ModbusWorker does not overwrite the Payload directly, because
	 * multiple Messages can be send to the topic and this should be able to be
	 * handled differently, than purely overwriting everything the SubscribeTask
	 * has. E.g. You subscribe to Topic Foo and you wait for Message containing "A"
	 * "B" and "C", multiple devices could send messages, One for "A", one for "B"
	 * and one for "C" to prevent overwriting and prevent starvation, handle a more
	 * "addative" approach instead.
	 * 
	 * @param payload the Payload, stored in the {@link MqttWorker}.
	 */
	void updatePayloadAfterCallback(Payload payload);

	/**
	 * Sets the Time within the Payload. This is usually called by the
	 * {@link MqttPublishTask}.
	 * 
	 * @param time the Time appearing in the Payload. Formatting is set within the
	 *             Payload itself.
	 */
	void setTime(ZonedDateTime time);

	/**
	 * Getter Method for the currently stored Payload Message as a String.
	 * 
	 * @return The Payload as a String.
	 */
	String getPayloadMessage();

	/**
	 * Builds the current available Payload (Mapping the value of a configured
	 * OpenemsChannel to a configured "key") e.g. if you want your _sum/ActivePower
	 * value to appear as "Foo" within your Message. The Payload will add "Foo":
	 * Value Where Value == current Channel Value of _sum/ActivePower.
	 * 
	 * @param component the referenced OpenEmsComponent containing the configured
	 *                  channels.
	 * @return the PayloadObject
	 */
	Payload build(OpenemsComponent component);

	/**
	 * CalledBySubscribeTasks -> Usually to Update the Component based on the
	 * subscribed Payload.
	 * 
	 * @param comp the referenced Component usually from {@link MqttComponent}.
	 */
	void handlePayloadToComponent(OpenemsComponent comp);

}
