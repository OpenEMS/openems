package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Base Nature of MqttComponents extending/being
 * {@link AbstractOpenEmsMqttComponent}.
 */
public interface MqttComponent extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		MQTT_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Mqtt Communication failed")), //
		CONFIGURATION_ISSUE(Doc.of(Level.FAULT) //
				.text("Configuration is wrong, please Check available Channel and Channel config."))//
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#MQTT_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMqttCommunicationFailedChannel() {
		return this.channel(ChannelId.MQTT_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Mqtt Communication Failed State. See
	 * {@link ChannelId#MQTT_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMqttCommunicationFailed() {
		return this.getMqttCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MQTT_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMqttCommunicationFailed(boolean value) {
		this.getMqttCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONFIGURATION_ISSUE}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getConfigurationFailedChannel() {
		return this.channel(ChannelId.CONFIGURATION_ISSUE);
	}

	/**
	 * Gets the Configuration Failed State. See
	 * {@link ChannelId#CONFIGURATION_ISSUE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getConfigurationFailed() {
		return this.getMqttCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONFIGURATION_ISSUE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConfigurationFail(boolean value) {
		this.getConfigurationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the referenced Component of the MqttComponent (e.g. an electricityMeter)
	 * 
	 * @return the Referenced {@link OpenemsComponent}.
	 */
	OpenemsComponent getReferenceComponent();
}
