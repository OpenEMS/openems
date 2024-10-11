package io.openems.edge.ess.generic.common;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.generic.symmetric.ChannelManager;

public interface GenericManagedEss extends ManagedSymmetricEss, StartStoppable, ModbusSlave {

	/**
	 * Efficiency factor to calculate AC Charge/Discharge limits from DC. Used at
	 * {@link ChannelManager}.
	 */
	public static double EFFICIENCY_FACTOR = 0.95;

	/**
	 * Retry set-command after x Seconds, e.g. for starting battery or
	 * battery-inverter.
	 */
	public static int RETRY_COMMAND_SECONDS = 30;

	/**
	 * Retry x attempts for set-command.
	 */
	public static int RETRY_COMMAND_MAX_ATTEMPTS = 30;

	/**
	 * Retry set-command after x Seconds, e.g. for starting battery or
	 * battery-inverter.
	 */
	public static int TIMEOUT = 300;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TIMEOUT_START_BATTERY(Doc.of(Level.FAULT) //
				.text("Start battery timeout passed!")), //
		TIMEOUT_START_BATTERY_INVERTER(Doc.of(Level.FAULT) //
				.text("Start battery inverter timeout passed!")), //
		TIMEOUT_STOP_BATTERY(Doc.of(Level.FAULT) //
				.text("Stop battery timeout passed!")), //
		TIMEOUT_STOP_BATTERY_INVERTER(Doc.of(Level.FAULT) //
				.text("Stop battery inverter timeout passed!")), //
		MAX_BATTERY_START_ATTEMPTS_FAULT(Doc.of(Level.WARNING) //
				.text("The maximum number of Battery start attempts failed")), //
		MAX_BATTERY_STOP_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery stop attempts failed")), //
		MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery-Inverter start attempts failed")), //
		MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery-Inverter stop attempts failed")); //

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
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_START_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryStartAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_START_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#MAX_BATTERY_START_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryStartAttemptsFault() {
		return this.getMaxBatteryStartAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_START_ATTEMPTS_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxBatteryStartAttemptsFault(boolean value) {
		this.getMaxBatteryStartAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryStopAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_STOP_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryStopAttemptsFault() {
		return this.getMaxBatteryStopAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxBatteryStopAttemptsFault(boolean value) {
		this.getMaxBatteryStopAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryInverterStartAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryInverterStartAttemptsFault() {
		return this.getMaxBatteryInverterStartAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxBatteryInverterStartAttemptsFault(boolean value) {
		this.getMaxBatteryInverterStartAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryInverterStopAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryInverterStopAttemptsFault() {
		return this.getMaxBatteryInverterStopAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxBatteryInverterStopAttemptsFault(boolean value) {
		this.getMaxBatteryInverterStopAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_START_BATTERY}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStartBatteryChannel() {
		return this.channel(ChannelId.TIMEOUT_START_BATTERY);
	}

	/**
	 * Gets the StateChannel value for {@link ChannelId#TIMEOUT_START_BATTERY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStartBattery() {
		return this.getTimeoutStartBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_START_BATTERY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTimeoutStartBattery(boolean value) {
		this.getTimeoutStartBatteryChannel().setNextValue(value);
	}

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

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_STOP_BATTERY}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStopBatteryChannel() {
		return this.channel(ChannelId.TIMEOUT_STOP_BATTERY);
	}

	/**
	 * Gets the StateChannel value for {@link ChannelId#TIMEOUT_STOP_BATTERY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStopBattery() {
		return this.getTimeoutStopBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTimeoutStopBattery(boolean value) {
		this.getTimeoutStopBatteryChannel().setNextValue(value);
	}

}
