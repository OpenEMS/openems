package io.openems.edge.batteryinverter.api;

import java.util.List;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BatteryInverterErrorAcknowledge extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TIMEOUT_START_BATTERY_INVERTER(Doc.of(Level.FAULT)//
				.text("Start battery inverter timeout passed!")), //
		TIMEOUT_STOP_BATTERY_INVERTER(Doc.of(Level.FAULT) //
				.text("Stop battery inverter timeout passed!")) //

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
	 * {@link #defineBatteryInverterErrorAcknowledgeChannels()}.
	 */
	default void executeBatteryInverterErrorAcknowledge() {
		this.defineBatteryInverterErrorAcknowledgeChannels().forEach(c -> c.setNextValue(null));
	}

	/**
	 * Defines every channel to be reset.
	 *
	 * @return a {@link List} of {@link Channel}.
	 */
	default List<Channel<?>> defineBatteryInverterErrorAcknowledgeChannels() {
		return List.of(this.getTimeoutStartBatteryInverterChannel(), this.getTimeoutStopBatteryInverterChannel());
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_START_BATTERY_INVERTER}.
	 *
	 * @return the Channel
	 */
	default StateChannel getTimeoutStartBatteryInverterChannel() {
		return this.channel(ChannelId.TIMEOUT_START_BATTERY_INVERTER);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#TIMEOUT_START_BATTERY_INVERTER}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTimeoutStartBatteryInverter() {
		return this.getTimeoutStartBatteryInverterChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_START_BATTERY_INVERTER} Channel.
	 *
	 * @param value the next value
	 */
	default void _setTimeoutStartBatteryInverter(boolean value) {
		this.getTimeoutStartBatteryInverterChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_STOP_BATTERY_INVERTER}.
	 *
	 * @return the Channel
	 */
	default StateChannel getTimeoutStopBatteryInverterChannel() {
		return this.channel(ChannelId.TIMEOUT_STOP_BATTERY_INVERTER);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY_INVERTER}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTimeoutStopBatteryInverter() {
		return this.getTimeoutStopBatteryInverterChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY_INVERTER} Channel.
	 *
	 * @param value the next value
	 */
	default void _setTimeoutStopBatteryInverter(boolean value) {
		this.getTimeoutStopBatteryInverterChannel().setNextValue(value);
	}
}