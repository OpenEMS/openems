package io.openems.edge.controller.ess.fixstateofcharge.api;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface FixStateOfCharge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Current state of the StateMachine.
		 */
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		/**
		 * Holds {@link ManagedSymmetricEss.ChannelId#DEBUG_SET_ACTIVE_POWER} for debug
		 * purpose.
		 */
		DEBUG_SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		/**
		 * Holds {@link ManagedSymmetricEss.ChannelId#DEBUG_SET_ACTIVE_POWER_RAW} for
		 * debug purpose.
		 */
		DEBUG_SET_ACTIVE_POWER_RAW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		/**
		 * Holds power to increase/decrease ramp for every cycle.
		 */
		DEBUG_RAMP_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT).text("The debug ramp power to decrease power")), //

		/**
		 * Holds {@link SymmetricEss.ChannelId#CAPACITY} for detecting a capacity
		 * change.
		 */
		ESS_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		NO_VALID_TARGET_TIME(Doc.of(Level.WARNING) //
				.text("Target time is not valid.")), //

		/**
		 * At SoC target as epoch seconds.
		 * 
		 * <p>
		 * Set, when the target SoC was reached. Used for terminating the Controller
		 * after a configured fallback time.
		 * 
		 * <p>
		 * When the target SoC not reached or the controller not running the default is
		 * 0, to be able to use the pastValues and get the last valid (not Null) value.
		 */
		AT_TARGET_EPOCH_SECONDS(Doc.of(OpenemsType.LONG) //
				.text("Time when the target SoC was reached a epoch seconds.")),

		/**
		 * Expected start epoch seconds.
		 * 
		 * <p>
		 * Set when a target time is configured and the controller is waiting for the
		 * start.
		 * 
		 * <p>
		 * Controller keep staying in "NotStarted" if there is still enough time left to
		 * reach the target soc.s
		 */
		EXPECTED_START_EPOCH_SECONDS(Doc.of(OpenemsType.LONG) //
				.text("Time when the controller is starting to charge or discharge depending on the target date and time.")),

		/**
		 * ESS is at target SoC. Discharging and charging is blocked by Controller.
		 */
		CTRL_IS_BLOCKING_ESS(Doc.of(Level.WARNING) //
				.text("The ESS is at the target state of charge. Charging/discharging is blocked by the controller as long as a fallback time is reached or a condition, e.g. capacity change, is fulfilled.")), //

		/**
		 * ESS is at below SoC. Ess is forced to charge with a minimum power.
		 */
		CTRL_IS_CHARGING_ESS(Doc.of(Level.WARNING) //
				.text("The system will not behave as usual. Controller is charging the ESS to reach the target state of charge.")), //

		/**
		 * ESS is at above SoC. Ess is forced to discharge with a minimum power.
		 */
		CTRL_IS_DISCHARGING_ESS(Doc.of(Level.WARNING) //
				.text("The system will not behave as usual. Controller is discharging the ESS to reach the target state of charge.")), //

		/**
		 * Controller has set the property isRunning to false if there was a termination
		 * by a condition or fallback time.
		 */
		CTRL_WAS_SELF_TERMINATED(Doc.of(Level.INFO) //
				.text("Charging/Discharging to fix State of Charge deactivated. Capacity changed or SoC reached and fallback time passed.")), //

		/**
		 * Controller has detected a change in the ess capacity.
		 */
		CAPACTITY_CHANGED(Doc.of(Level.INFO) //
				.text("The capacity changed.")), //
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
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets current state of the {@link StateMachine}. See
	 * {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default State getStateMachine() {
		return this.getStateMachineChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateMachine(State value) {
		this.getStateMachineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerChannel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER);
	}

	/**
	 * Gets the active power limit set in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePower() {
		return this.getDebugSetActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePower(Integer value) {
		this.getDebugSetActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePower(int value) {
		this.getDebugSetActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER_RAW}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerRawChannel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER_RAW);
	}

	/**
	 * Gets the active power limit set in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_RAW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePowerRaw() {
		return this.getDebugSetActivePowerRawChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_RAW} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerRaw(Integer value) {
		this.getDebugSetActivePowerRawChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_RAW} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerRaw(int value) {
		this.getDebugSetActivePowerRawChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_RAMP_POWER}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getDebugRampPowerChannel() {
		return this.channel(ChannelId.DEBUG_RAMP_POWER);
	}

	/**
	 * Gets the debug ramp power in [W]. See {@link ChannelId#DEBUG_RAMP_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Float> getDebugRampPower() {
		return this.getDebugRampPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DEBUG_RAMP_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugRampPower(Float value) {
		this.getDebugRampPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DEBUG_RAMP_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugRampPower(float value) {
		this.getDebugRampPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_CAPACITY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssCapacityChannel() {
		return this.channel(ChannelId.ESS_CAPACITY);
	}

	/**
	 * Gets the Capacity in [Wh]. See {@link ChannelId#ESS_CAPACITY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssCapacity() {
		return this.getEssCapacityChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_CAPACITY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssCapacity(Integer value) {
		this.getEssCapacityChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_CAPACITY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssCapacity(int value) {
		this.getEssCapacityChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AT_TARGET_EPOCH_SECONDS}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getAtTargetEpochSecondsChannel() {
		return this.channel(ChannelId.AT_TARGET_EPOCH_SECONDS);
	}

	/**
	 * Gets the time when the target SoC was reached as epoch seconds. See
	 * {@link ChannelId#AT_TARGET_EPOCH_SECONDS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getAtTargetEpochSeconds() {
		return this.getAtTargetEpochSecondsChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AT_TARGET_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAtTargetEpochSeconds(Long value) {
		this.getAtTargetEpochSecondsChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AT_TARGET_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAtTargetEpochSeconds(long value) {
		this.getAtTargetEpochSecondsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXPECTED_START_EPOCH_SECONDS}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getExpectedStartEpochSecondsChannel() {
		return this.channel(ChannelId.EXPECTED_START_EPOCH_SECONDS);
	}

	/**
	 * Gets the time when the controller is expected to charge or discharge the ess.
	 * See {@link ChannelId#EXPECTED_START_EPOCH_SECONDS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getExpectedStartEpochSeconds() {
		return this.getExpectedStartEpochSecondsChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EXPECTED_START_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setExpectedStartEpochSeconds(Long value) {
		this.getExpectedStartEpochSecondsChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EXPECTED_START_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setExpectedStartEpochSeconds(long value) {
		this.getExpectedStartEpochSecondsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CTRL_IS_BLOCKING_ESS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCtrlIsBlockingEssChannel() {
		return this.channel(ChannelId.CTRL_IS_BLOCKING_ESS);
	}

	/**
	 * Gets the State Channel as Boolean Value. See
	 * {@link ChannelId#CTRL_IS_BLOCKING_ESS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCtrlIsBlockingEss() {
		return this.getCtrlIsBlockingEssChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CTRL_IS_BLOCKING_ESS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCtrlIsBlockingEss(boolean value) {
		this.getCtrlIsBlockingEssChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CTRL_IS_CHARGING_ESS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCtrlIsChargingEssChannel() {
		return this.channel(ChannelId.CTRL_IS_CHARGING_ESS);
	}

	/**
	 * Gets the State Channel as Boolean Value. See
	 * {@link ChannelId#CTRL_IS_CHARGING_ESS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCtrlIsChargingEss() {
		return this.getCtrlIsChargingEssChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CTRL_IS_CHARGING_ESS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCtrlIsChargingEss(boolean value) {
		this.getCtrlIsChargingEssChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CTRL_IS_DISCHARGING_ESS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCtrlIsDischargingEssChannel() {
		return this.channel(ChannelId.CTRL_IS_DISCHARGING_ESS);
	}

	/**
	 * Gets the State Channel as Boolean Value. See
	 * {@link ChannelId#CTRL_IS_DISCHARGING_ESS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCtrlIsDischargingEss() {
		return this.getCtrlIsDischargingEssChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CTRL_IS_DISCHARGING_ESS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCtrlIsDischargingEss(boolean value) {
		this.getCtrlIsDischargingEssChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CTRL_WAS_SELF_TERMINATED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCtrlWasSelfTerminatedChannel() {
		return this.channel(ChannelId.CTRL_WAS_SELF_TERMINATED);
	}

	/**
	 * Gets the State Channel as Boolean Value. See
	 * {@link ChannelId#CTRL_WAS_SELF_TERMINATED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCtrlWasSelfTerminated() {
		return this.getCtrlWasSelfTerminatedChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CTRL_WAS_SELF_TERMINATED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCtrlWasSelfTerminated(boolean value) {
		this.getCtrlWasSelfTerminatedChannel().setNextValue(value);
	}
}
