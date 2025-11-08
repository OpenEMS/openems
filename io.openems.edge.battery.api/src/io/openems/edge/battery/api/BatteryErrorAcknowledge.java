package io.openems.edge.battery.api;

import java.util.List;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BatteryErrorAcknowledge extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TIMEOUT_START_BATTERY(Doc.of(Level.FAULT)//
				.text("Start battery timeout passed!")), //
		TIMEOUT_STOP_BATTERY(Doc.of(Level.FAULT)//
				.text("Stop battery timeout passed!")), //
		;

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
	 * Resets every channel defined in
	 * {@link #defineBatteryErrorAcknowledgeChannels()}.
	 */
	default void executeBatteryErrorAcknowledge() {
		this.defineBatteryErrorAcknowledgeChannels().forEach(c -> c.setNextValue(null));
	}

	/**
	 * Defines every channel to be reset.
	 * 
	 * @return a {@link List} of {@link Channel}.
	 */
	default List<Channel<?>> defineBatteryErrorAcknowledgeChannels() {
		return List.of(this.getTimeoutStartBatteryChannel(), this.getTimeoutStopBatteryChannel());
	}

	/**
	 * Gets the Channel for
	 * {@link BatteryErrorAcknowledge.ChannelId#TIMEOUT_START_BATTERY}.
	 *
	 * @return the Channel
	 */
	default StateChannel getTimeoutStartBatteryChannel() {
		return this.channel(BatteryErrorAcknowledge.ChannelId.TIMEOUT_START_BATTERY);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link BatteryErrorAcknowledge.ChannelId#TIMEOUT_START_BATTERY}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTimeoutStartBattery() {
		return this.getTimeoutStartBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link BatteryErrorAcknowledge.ChannelId#TIMEOUT_START_BATTERY} Channel.
	 *
	 * @param value the next value
	 */
	default void _setTimeoutStartBattery(boolean value) {
		this.getTimeoutStartBatteryChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link BatteryErrorAcknowledge.ChannelId#TIMEOUT_STOP_BATTERY}.
	 *
	 * @return the Channel
	 */
	default StateChannel getTimeoutStopBatteryChannel() {
		return this.channel(BatteryErrorAcknowledge.ChannelId.TIMEOUT_STOP_BATTERY);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link BatteryErrorAcknowledge.ChannelId#TIMEOUT_STOP_BATTERY}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTimeoutStopBattery() {
		return this.getTimeoutStopBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link BatteryErrorAcknowledge.ChannelId#TIMEOUT_STOP_BATTERY} Channel.
	 *
	 * @param value the next value
	 */
	default void _setTimeoutStopBattery(boolean value) {
		this.getTimeoutStopBatteryChannel().setNextValue(value);
	}

}
