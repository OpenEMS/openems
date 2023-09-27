package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;

/**
 * The Interface/Nature of a MqttBridge. Provides base methods and Channels,
 * that should be called by e.g. {@link AbstractOpenEmsMqttComponent}, when
 * creating a {@link MqttProtocol} and adding it to the bridge.
 */
public interface BridgeMqtt extends OpenemsComponent {
	String DEFAULT_COMMUNICATION_FAILED_MESSAGE = "Communication with the Broker failed.";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Is the Component Disabled.
		 *
		 * <ul>
		 * <li>Interface: MqttBridge
		 * <li>Readable
		 * <li>Level: INFO
		 * </ul>
		 */
		DISABLED(Doc.of(Level.INFO) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Failed state channel for a failed communication to the Mqtt Broker. Broker
		 * unavailable.
		 * <ul>
		 * <li>Interface: MqttBridge
		 * <li>Readable
		 * <li>Level: FAULT
		 * </ul>
		 */
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text(DEFAULT_COMMUNICATION_FAILED_MESSAGE)), //

		/**
		 * Failed state channel for an Authentication Error to the Mqtt Broker.
		 *
		 * <ul>
		 * <li>Interface: MqttBridge
		 * <li>Readable
		 * <li>Level: FAULT
		 * </ul>
		 */
		AUTHENTICATION_FAILED(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_HIGH)
				.text("Authentication failed. Please check Username and Password.")),

		/**
		 * Failed state channel for Access Denied Case at the MqttBroker. Some Brokers
		 * disconnect you when trying to access a Topic you are not allowed to.
		 * <ul>
		 * <li>Interface: MqttBridge
		 * <li>Readable
		 * <li>Level: FAULT
		 * </ul>
		 */
		ACCESS_DENIED(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_HIGH)
				.text("Access denied, a topic you want to subscribe/publish to is not allowed.")), //

		CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		EXECUTION_DURATION(Doc.of(OpenemsType.LONG) //
				.unit(Unit.MILLISECONDS)),

		CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.INFO) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)) //
		; //

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}

	}

	/**
	 * Gets the Channel for {@link ChannelId#DISABLED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDisabledChannel() {
		return this.channel(ChannelId.DISABLED);
	}

	/**
	 * Gets the Failed state channel for a failed communication. See
	 * {@link ChannelId#DISABLED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getDisabled() {
		return this.getDisabledChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DISABLED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDisabled(boolean value) {
		this.getDisabledChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCommunicationFailedChannel() {
		return this.channel(ChannelId.COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Failed state channel for a failed communication. See
	 * {@link ChannelId#COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCommunicationFailed() {
		return this.getCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommunicationFailed(boolean value) {
		this.getCommunicationFailedChannel().setNextValue(value);
		this._setCommunicationFailed(value, DEFAULT_COMMUNICATION_FAILED_MESSAGE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMUNICATION_FAILED} Channel.
	 *
	 * @param value       the next value
	 * @param description the new description of the failed Communication state.
	 */
	public default void _setCommunicationFailed(boolean value, String description) {
		this.getCommunicationFailedChannel().setNextValue(value);
		this.getCommunicationFailedChannel().channelDoc().text(description);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AUTHENTICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAuthenticationFailedChannel() {
		return this.channel(ChannelId.AUTHENTICATION_FAILED);
	}

	/**
	 * Gets the Failed state channel for a failed communication. See
	 * {@link ChannelId#AUTHENTICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAuthenticationFailed() {
		return this.getAuthenticationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AUTHENTICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAuthenticationFailed(boolean value) {
		this.getAuthenticationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACCESS_DENIED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAccessDeniedChannel() {
		return this.channel(ChannelId.ACCESS_DENIED);
	}

	/**
	 * Gets the Failed state channel for a failed communication. See
	 * {@link ChannelId#ACCESS_DENIED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAccessDenied() {
		return this.getAccessDeniedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACCESS_DENIED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAccessDenied(boolean value) {
		this.getAccessDeniedChannel().setNextValue(value);
	}

	/**
	 * Adds Task to the MQtt Bridge.
	 *
	 * @param id           ComponentId
	 * @param mqttProtocol MqttTask created by the AbstractMqttComponent.
	 */
	void addMqttProtocol(String id, MqttProtocol mqttProtocol);

	/**
	 * Removes a Protocol from this Mqtt Bridge.
	 *
	 * @param sourceId the unique source identifier
	 */
	public void removeMqttProtocol(String sourceId);

	/**
	 * Gets the Channel for {@link ChannelId#CONNECTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getConnectedChannel() {
		return this.channel(ChannelId.CONNECTED);
	}

	/**
	 * Gets the Value of the Channel Connected. See {@link ChannelId#CONNECTED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getConnected() {
		return this.getConnectedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONNECTED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConnected(boolean value) {
		this.getConnectedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXECUTION_DURATION}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getExecutionDurationChannel() {
		return this.channel(ChannelId.EXECUTION_DURATION);
	}

	/**
	 * Gets the Execution Duration in [ms], see
	 * {@link ChannelId#EXECUTION_DURATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getExecutionDuration() {
		return this.getExecutionDurationChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EXECUTION_DURATION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setExecutionDuration(long value) {
		this.getExecutionDurationChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CYCLE_TIME_IS_TOO_SHORT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCycleTimeIsTooShortChannel() {
		return this.channel(ChannelId.CYCLE_TIME_IS_TOO_SHORT);
	}

	/**
	 * Gets the Cycle-Time-is-too-short State. See
	 * {@link ChannelId#CYCLE_TIME_IS_TOO_SHORT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCycleTimeIsTooShort() {
		return this.getCycleTimeIsTooShortChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CYCLE_TIME_IS_TOO_SHORT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCycleTimeIsTooShort(boolean value) {
		this.getCycleTimeIsTooShortChannel().setNextValue(value);
	}

	/**
	 * Get the Cycle of the BridgeMqtt. Usually used by the
	 * {@link MqttWorker}.
	 * 
	 * @return the {@link Cycle}
	 */
	Cycle getCycle();

	/**
	 * Get the ComponentManager of the BridgeMqtt. Usually used by the
	 * {@link MqttWorker}.
	 * 
	 * @return the {@link ComponentManager}
	 */

	ComponentManager getComponentManager();

}
