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
		GRID_FAILURE_LINE_TO_LINE(Doc.of(Level.FAULT) //
				.text("Grid failure phase-to-phase voltage (47)")), //
		LINE_FAILURE_UNDER_FREQ(Doc.of(Level.FAULT) //
				.text("Line failure: Grid frequency is too low (48)")), //
		LINE_FAILURE_OVER_FREQ(Doc.of(Level.FAULT) //
				.text("Line failure: Grid frequency is too high (49)")), //
		PROTECTION_SHUTDOWN_LINE_1(Doc.of(Level.FAULT) //
				.text("Grid Failure: grid voltage L1 protection (81)")), //
		PROTECTION_SHUTDOWN_LINE_2(Doc.of(Level.FAULT) //
				.text("Grid Failure: grid voltage L2 protection (82)")), //
		PROTECTION_SHUTDOWN_LINE_3(Doc.of(Level.FAULT) //
				.text("Grid Failure: grid voltage L3 protection (83)")), //

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
	 * Gets the Channel for ChannelId.GRID_FAILURE_LINE_TO_LINE.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getGridFailureLineToLineChannel() {
		return this.channel(ChannelId.GRID_FAILURE_LINE_TO_LINE);
	}

	/**
	 * Writes the value to the ChannelId.GRID_FAILURE_LINE_TO_LINE.
	 *
	 * @param value the next value
	 */
	public default void _setGridFailureLineToLine(boolean value) {
		this.getGridFailureLineToLineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.LINE_FAILURE_UNDER_FREQ.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getLineFailureUnderFreqChannel() {
		return this.channel(ChannelId.LINE_FAILURE_UNDER_FREQ);
	}

	/**
	 * Writes the value to the ChannelId.LINE_FAILURE_UNDER_FREQ.
	 *
	 * @param value the next value
	 */
	public default void _setLineFailureUnderFreq(boolean value) {
		this.getLineFailureUnderFreqChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.LINE_FAILURE_OVER_FREQ.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getLineFailureOverFreqChannel() {
		return this.channel(ChannelId.LINE_FAILURE_OVER_FREQ);
	}

	/**
	 * Writes the value to the ChannelId.LINE_FAILURE_OVER_FREQ.
	 *
	 * @param value the next value
	 */
	public default void _setLineFailureOverFreq(boolean value) {
		this.getLineFailureOverFreqChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.PROTECTION_SHUTDOWN_LINE_1.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getProtectionShutdownLine1Channel() {
		return this.channel(ChannelId.PROTECTION_SHUTDOWN_LINE_1);
	}

	/**
	 * Writes the value to the ChannelId.PROTECTION_SHUTDOWN_LINE_1.
	 *
	 * @param value the next value
	 */
	public default void _setProtectionShutdownLine1(boolean value) {
		this.getProtectionShutdownLine1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.PROTECTION_SHUTDOWN_LINE_2.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getProtectionShutdownLine2Channel() {
		return this.channel(ChannelId.PROTECTION_SHUTDOWN_LINE_2);
	}

	/**
	 * Writes the value to the ChannelId.PROTECTION_SHUTDOWN_LINE_2.
	 *
	 * @param value the next value
	 */
	public default void _setProtectionShutdownLine2(boolean value) {
		this.getProtectionShutdownLine2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for ChannelId.PROTECTION_SHUTDOWN_LINE_3.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getProtectionShutdownLine3Channel() {
		return this.channel(ChannelId.PROTECTION_SHUTDOWN_LINE_3);
	}

	/**
	 * Writes the value to the ChannelId.PROTECTION_SHUTDOWN_LINE_3.
	 *
	 * @param value the next value
	 */
	public default void _setProtectionShutdownLine3(boolean value) {
		this.getProtectionShutdownLine3Channel().setNextValue(value);
	}

}
