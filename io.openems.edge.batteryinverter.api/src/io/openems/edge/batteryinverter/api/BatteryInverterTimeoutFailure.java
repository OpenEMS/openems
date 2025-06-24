package io.openems.edge.batteryinverter.api;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BatteryInverterTimeoutFailure extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TIMEOUT_START_BATTERY_INVERTER(Doc.of(Level.FAULT) //
				.text("Start battery inverter timeout passed!")), //
		TIMEOUT_STOP_BATTERY_INVERTER(Doc.of(Level.FAULT) //
				.text("Stop battery inverter timeout passed!")) //

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
	 * This method attempts to set the individual component timeout channels to
	 * false.
	 */
	public void clearBatteryInverterTimeoutFailure();

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_START_BATTERY_INVERTER}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStartBatteryInverterhannel() {
		return this.channel(ChannelId.TIMEOUT_START_BATTERY_INVERTER);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#TIMEOUT_START_BATTERY_INVERTER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStartBatteryInverter() {
		return this.getTimeoutStartBatteryInverterhannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_START_BATTERY_INVERTER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTimeoutStartBatteryInverter(boolean value) {
		this.getTimeoutStartBatteryInverterhannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_STOP_BATTERY_INVERTER}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStopBatteryInverterChannel() {
		return this.channel(ChannelId.TIMEOUT_STOP_BATTERY_INVERTER);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY_INVERTER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStopBatteryInverter() {
		return this.getTimeoutStopBatteryInverterChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY_INVERTER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTimeoutStopBatteryInverter(boolean value) {
		this.getTimeoutStopBatteryInverterChannel().setNextValue(value);
	}
}