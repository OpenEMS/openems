package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201CurrentState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryInverterKacoBlueplanetGridsave
		extends ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent, StartStoppable {

	/**
	 * Sets the KACO watchdog timeout to 60 seconds.
	 */
	public static final int WATCHDOG_TIMEOUT_SECONDS = 60;
	/**
	 * The watchdog gets triggered every WATCHDOG_TRIGGER_CYCLES seconds. This must
	 * be less than WATCHDOG_TIMEOUT_SECONDS.
	 */
	public static final int WATCHDOG_TRIGGER_SECONDS = 10;

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
		MAX_START_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of stop attempts failed")), //
		INVERTER_CURRENT_STATE_FAULT(Doc.of(Level.FAULT) //
				.text("The 'CurrentState' is invalid")), //
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
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Get the Channel for the given Point or throw an error if it is not available.
	 *
	 * @param <T>   the Channel type
	 * @param point the SunSpec Point
	 * @return the optional Channel
	 * @throws OpenemsException if Channel is not available
	 */
	public <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint point) throws OpenemsException;

	/**
	 * Gets the Current State.
	 *
	 * @return the {@link S64201CurrentState}
	 */
	public S64201CurrentState getCurrentState();

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for KacoSunSpecModel.S64201.REQUESTED_STATE.
	 *
	 * @return the Channel
	 * @throws OpenemsException on error
	 */
	public default WriteChannel<S64201RequestedState> getRequestedStateChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(KacoSunSpecModel.S64201.REQUESTED_STATE);
	}

	/**
	 * Writes the value to the KacoSunSpecModel.S64201.REQUESTED_STATE Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRequestedState(S64201RequestedState value) throws OpenemsNamedException {
		this.getRequestedStateChannel().setNextWriteValue(value);
	}
}
