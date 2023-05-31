package io.openems.edge.batteryinverter.refu88k;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu88k.enums.OperatingState;
import io.openems.edge.batteryinverter.refu88k.enums.PcsSetOperation;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface RefuStore88k
		extends ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent, StartStoppable {

	public static final int WATCHDOG_CYCLES = 10;

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
		STATE_MACHINE(Doc.of(StateMachine.State.values()) //
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
	 * Exit the STANDBY mode.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void exitStandbyMode() throws OpenemsNamedException {
		EnumWriteChannel pcsSetOperation = this.channel(RefuStore88kChannelId.PCS_SET_OPERATION);
		pcsSetOperation.setNextWriteValue(PcsSetOperation.EXIT_STANDBY_MODE);
	}

	/**
	 * Enter the STARTED mode.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void enterStartedMode() throws OpenemsNamedException {
		EnumWriteChannel pcsSetOperation = this.channel(RefuStore88kChannelId.PCS_SET_OPERATION);
		pcsSetOperation.setNextWriteValue(PcsSetOperation.STOP_PCS);
	}

	/**
	 * Enter the Throttled or MPPT mode.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void enterThrottledMpptMode() throws OpenemsNamedException {
		EnumWriteChannel pcsSetOperation = this.channel(RefuStore88kChannelId.PCS_SET_OPERATION);
		pcsSetOperation.setNextWriteValue(PcsSetOperation.START_PCS);
	}

	/**
	 * STOP the inverter by setting the power to zero and entering the STARTED mode.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void stopInverter() throws OpenemsNamedException {
		EnumWriteChannel pcsSetOperation = this.channel(RefuStore88kChannelId.PCS_SET_OPERATION);
		pcsSetOperation.setNextWriteValue(PcsSetOperation.ENTER_STANDBY_MODE);
	}

	public default String getSerialNumber() {
		return this.channel(RefuStore88kChannelId.SN).value().asString();
	}

	public default Channel<Integer> getDcVoltage() {
		return this.channel(RefuStore88kChannelId.DCV);
	}

	public default Channel<Integer> getAcVoltage() {
		return this.channel(RefuStore88kChannelId.PP_VPH_AB);
	}

	public default Channel<Integer> getAcCurrent() {
		return this.channel(RefuStore88kChannelId.A);
	}

	public default Channel<Integer> getApparentPower() {
		return this.channel(RefuStore88kChannelId.VA);
	}

	public default OperatingState getOperatingState() {
		return this.channel(RefuStore88kChannelId.ST).value().asEnum();
	}

}
