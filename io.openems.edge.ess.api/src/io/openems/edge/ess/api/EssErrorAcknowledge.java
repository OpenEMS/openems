package io.openems.edge.ess.api;

import static io.openems.common.channel.Level.FAULT;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EssErrorAcknowledge extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TIMEOUT_START_BATTERY(Doc.of(FAULT)//
				.text("Start battery timeout passed!")), //
		TIMEOUT_START_BATTERY_INVERTER(Doc.of(FAULT)//
				.text("Start battery inverter timeout passed!")), //
		TIMEOUT_STOP_BATTERY(Doc.of(FAULT)//
				.text("Stop battery timeout passed!")), //
		TIMEOUT_STOP_BATTERY_INVERTER(Doc.of(FAULT) //
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
	 * This method attempts to set the individual component error channels to null.
	 */
	void executeErrorAcknowledge();

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_START_BATTERY}.
	 *
	 * @return the Channel
	 */
	default StateChannel getTimeoutStartBatteryChannel() {
		return this.channel(ChannelId.TIMEOUT_START_BATTERY);
	}

	/**
	 * Gets the StateChannel value for {@link ChannelId#TIMEOUT_START_BATTERY}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTimeoutStartBattery() {
		return this.getTimeoutStartBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_START_BATTERY} Channel.
	 *
	 * @param value the next value
	 */
	default void _setTimeoutStartBattery(boolean value) {
		this.getTimeoutStartBatteryChannel().setNextValue(value);
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

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_STOP_BATTERY}.
	 *
	 * @return the Channel
	 */
	default StateChannel getTimeoutStopBatteryChannel() {
		return this.channel(ChannelId.TIMEOUT_STOP_BATTERY);
	}

	/**
	 * Gets the StateChannel value for {@link ChannelId#TIMEOUT_STOP_BATTERY}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTimeoutStopBattery() {
		return this.getTimeoutStopBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY} Channel.
	 *
	 * @param value the next value
	 */
	default void _setTimeoutStopBattery(boolean value) {
		this.getTimeoutStopBatteryChannel().setNextValue(value);
	}
}