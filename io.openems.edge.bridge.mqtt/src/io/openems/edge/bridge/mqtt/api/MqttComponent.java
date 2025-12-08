package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Marker interface for OpenEMS components that use MQTT communication.
 *
 * <p>
 * Components implementing this interface can communicate via MQTT through a
 * {@link BridgeMqtt}.
 */
public interface MqttComponent extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * MQTT Communication Failed.
		 *
		 * <ul>
		 * <li>Interface: MqttComponent
		 * <li>Type: State
		 * <li>Level: WARNING
		 * <li>Description: MQTT communication with this component failed
		 * </ul>
		 */
		MQTT_COMMUNICATION_FAILED(Doc.of(Level.WARNING)//
				.text("MQTT communication failed"));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Retries MQTT communication for this component.
	 *
	 * <p>
	 * This method resets any error states and attempts to re-establish
	 * communication.
	 */
	void retryMqttCommunication();

}
