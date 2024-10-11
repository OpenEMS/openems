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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/*
		 * Whenever one of these Level.FAULT states is true, the GenericEss will stop
		 * the battery and the inverter.
		 */
		MAX_BATTERY_START_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
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
	 * Gets the {@link StateChannel} for
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
	 * Gets the {@link StateChannel} for
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
	 * Gets the {@link StateChannel} for
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
	 * Gets the {@link StateChannel} for
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

}
