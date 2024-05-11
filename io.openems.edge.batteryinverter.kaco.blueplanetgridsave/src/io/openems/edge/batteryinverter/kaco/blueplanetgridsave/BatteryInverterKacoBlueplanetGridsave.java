package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201CurrentState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryInverterKacoBlueplanetGridsave extends ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, ModbusComponent, OpenemsComponent, StartStoppable {

	/**
	 * Sets the KACO watchdog timeout to 60 seconds.
	 */
	public static final int WATCHDOG_TIMEOUT_SECONDS = 60;
	/**
	 * The watchdog gets triggered every WATCHDOG_TRIGGER_CYCLES seconds. This must
	 * be less than WATCHDOG_TIMEOUT_SECONDS.
	 */
	public static final int WATCHDOG_TRIGGER_SECONDS = 10;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_START_TIMEOUT(Doc.of(Level.FAULT) //
				.text("Max start time is exceeded")), //
		MAX_STOP_TIMEOUT(Doc.of(Level.FAULT) //
				.text("Max stop time is exceeded")), //
		INVERTER_CURRENT_STATE_FAULT(Doc.of(Level.FAULT) //
				.text("The 'CurrentState' is invalid")), //
		GRID_DISCONNECTION(Doc.of(Level.FAULT) //
				.text("External grid protection disconnection (17)")), //
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
	 * Gets the Channel for {@link ChannelId#MAX_START_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartTimeoutChannel() {
		return this.channel(ChannelId.MAX_START_TIMEOUT);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartTimeout() {
		return this.getMaxStartTimeoutChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_START_TIMEOUT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartTimeout(boolean value) {
		this.getMaxStartTimeoutChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopTimeoutChannel() {
		return this.channel(ChannelId.MAX_STOP_TIMEOUT);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopTimeout() {
		return this.getMaxStopTimeoutChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_TIMEOUT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopTimeout(boolean value) {
		this.getMaxStopTimeoutChannel().setNextValue(value);
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

	/**
	 * Gets the Channel for ChannelId.INVERTER_CURRENT_STATE_FAULT.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getInverterCurrentStateFaultChannel() {
		return this.channel(ChannelId.INVERTER_CURRENT_STATE_FAULT);
	}

	/**
	 * Writes the value to the ChannelId.INVERTER_CURRENT_STATE_FAULT.
	 *
	 * @param value the next value
	 */
	public default void _setInverterCurrentStateFault(boolean value) {
		this.getInverterCurrentStateFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.RUN_FAILED.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getRunFailedChannel() {
		return this.channel(ChannelId.RUN_FAILED);
	}

	/**
	 * Writes the value to the ChannelId.RUN_FAILED.
	 *
	 * @param value the next value
	 */
	public default void _setRunFailed(boolean value) {
		this.getRunFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.GRID_DISCONNECTION.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getGridDisconnectionChannel() {
		return this.channel(ChannelId.GRID_DISCONNECTION);
	}

	/**
	 * Writes the value to the ChannelId.GRID_DISCONNECTION.
	 *
	 * @param value the next value
	 */
	public default void _setGridDisconnection(boolean value) {
		this.getGridDisconnectionChannel().setNextValue(value);
	}

	/**
	 * Checks if the system is in a running state. This method retrieves the
	 * system's global state and determines whether the system is in a running
	 * state.
	 *
	 * @return true if the system is in a running state, false otherwise.
	 */
	public boolean isRunning();

	/**
	 * Checks if the system is in a stop state. This method retrieves the system's
	 * global state and determines whether the system is in a stop state.
	 *
	 * @return true if the system is in a stop state, false otherwise.
	 */
	public boolean isShutdown();

	/**
	 * Checks if the system is in a fault state. This method retrieves the system's
	 * global state and determines whether the system is in a fault state.
	 *
	 * @return true if the system is in a fault state, false otherwise.
	 */
	public boolean hasFailure();
}
