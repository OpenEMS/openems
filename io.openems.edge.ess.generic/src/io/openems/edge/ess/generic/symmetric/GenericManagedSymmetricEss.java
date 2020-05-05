package io.openems.edge.ess.generic.symmetric;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.generic.symmetric.statemachine.State;

public interface GenericManagedSymmetricEss extends ManagedSymmetricEss, StartStoppable {

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
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_BATTERY_START_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery start attempts failed")), //
		MAX_BATTERY_STOP_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery stop attempts failed")), //
		MAX_BATTERY_INVERTER_START_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery-Inverter start attempts failed")), //
		MAX_BATTERY_INVERTER_STOP_ATTEMPTS(Doc.of(Level.FAULT) //
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
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_START_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_BATTERY_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_START_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryStartAttempts() {
		return this.getMaxBatteryStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_START_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryStartAttempts(Boolean value) {
		this.getMaxBatteryStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_BATTERY_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryStopAttempts() {
		return this.getMaxBatteryStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryStopAttempts(Boolean value) {
		this.getMaxBatteryStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryInverterStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_BATTERY_INVERTER_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryInverterStartAttempts() {
		return this.getMaxBatteryInverterStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryInverterStartAttempts(Boolean value) {
		this.getMaxBatteryInverterStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryInverterStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_BATTERY_INVERTER_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryInverterStopAttempts() {
		return this.getMaxBatteryInverterStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryInverterStopAttempts(Boolean value) {
		this.getMaxBatteryInverterStopAttemptsChannel().setNextValue(value);
	}

}
