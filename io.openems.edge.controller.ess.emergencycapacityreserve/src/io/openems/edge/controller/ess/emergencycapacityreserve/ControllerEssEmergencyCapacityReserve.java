package io.openems.edge.controller.ess.emergencycapacityreserve;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface ControllerEssEmergencyCapacityReserve extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current state of the StateMachine.
		 */
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		/**
		 * Holds {@link ManagedSymmetricEss.ChannelId#SET_ACTIVE_POWER_LESS_OR_EQUALS}
		 * for debug purpose.
		 */
		DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("The debug SetActivePowerLessOrEquals")), //

		/**
		 * Holds target power to reach.
		 */
		DEBUG_TARGET_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.text("The debug target power to reach")), //

		/**
		 * Holds power to increase/decrease ramp for every cycle.
		 */
		DEBUG_RAMP_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.text("The debug ramp power to decrease power")), //

		/**
		 * Configured reserve SoC is out of range [5,100].
		 */
		RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE(Doc.of(Level.WARNING) //
				.text("Reserve SoC does not fit in range 5 to 100")),

		/**
		 * Holds the actual reserve soc value. Holds null if reserve soc is disabled.
		 */
		ACTUAL_RESERVE_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("The reserve soc value") //
				.persistencePriority(PersistencePriority.HIGH)); //

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
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerLessOrEqualsChannel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS);
	}

	/**
	 * Gets the debug active power less or equals constraint in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePowerLessOrEquals() {
		return this.getDebugSetActivePowerLessOrEqualsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerLessOrEquals(Integer value) {
		this.getDebugSetActivePowerLessOrEqualsChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerLessOrEquals(int value) {
		this.getDebugSetActivePowerLessOrEqualsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_TARGET_POWER}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getDebugTargetPowerChannel() {
		return this.channel(ChannelId.DEBUG_TARGET_POWER);
	}

	/**
	 * Gets the debug ramp power in [W]. See {@link ChannelId#DEBUG_TARGET_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Float> getDebugTargetPower() {
		return this.getDebugTargetPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_TARGET_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugTargetPower(Float value) {
		this.getDebugTargetPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_TARGET_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugTargetPower(float value) {
		this.getDebugTargetPowerChannel().setNextValue(value);
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
		return this.getDebugTargetPowerChannel().value();
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRangeOfReserveSocOutsideAllowedValue(boolean value) {
		this.channel(ChannelId.RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE).setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTUAL_RESERVE_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActualReserveSocChannel() {
		return this.channel(ChannelId.ACTUAL_RESERVE_SOC);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTUAL_RESERVE_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActualReserveSoc(Integer value) {
		this.getActualReserveSocChannel().setNextValue(value);
	}

	/**
	 * Gets the SoC value if Reserve SoC is enabled and returns null otherwise. See
	 * {@link ChannelId#ACTUAL_RESERVE_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActualReserveSoc() {
		return this.getActualReserveSocChannel().value();
	}

}
