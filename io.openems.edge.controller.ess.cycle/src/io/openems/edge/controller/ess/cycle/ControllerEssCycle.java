package io.openems.edge.controller.ess.cycle;

import java.time.LocalDateTime;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public interface ControllerEssCycle extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values())//
				.text("Current State of State-Machine")), //
		AWAITING_HYSTERESIS(Doc.of(StateMachine.State.values()) //
				.text("Awaiting for active hysteresis, to change the state.")), //
		COMPLETED_CYCLES(Doc.of(OpenemsType.INTEGER) //
				.text("Number of cycles completed")); //

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
	 * @return the Channel {@link State}
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Value {@link State}
	 */
	public default Value<State> getStateMachine() {
		return this.getStateMachineChannel().value();
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
	 * Gets the Channel for {@link ChannelId#COMPLETED_CYCLES}.
	 *
	 * @return tthe Channel {@link Integer}
	 */
	public default Channel<Integer> getCompletedCyclesChannel() {
		return this.channel(ChannelId.COMPLETED_CYCLES);
	}

	/**
	 * Gets the Completed Cycles. See {@link ChannelId#COMPLETED_CYCLES}.
	 *
	 * @return the Value {@link Integer}
	 */
	public default Value<Integer> getCompletedCycles() {
		return this.getCompletedCyclesChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#COMPLETED_CYCLES}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCompletedCycles(int value) {
		this.getCompletedCyclesChannel().setNextValue(value);
	}

	/**
	 * Gets the current {@link State} state of the {@link StateMachine}.
	 *
	 * @return the {@link State}
	 */
	public State getCurrentState();

	/**
	 * Sets the next {@link StateMachine} {@link State}.
	 * 
	 * @param state as a next {@link StateMachine} {@link State} to be set.
	 */
	public void setNextState(State state);

	/**
	 * Gets the next {@link State} state of the {@link StateMachine}.
	 * 
	 * @return next {@link StateMachine} {@link State}
	 */
	public State getNextState();

	/**
	 * 
	 * @return
	 */
	public LocalDateTime getLastStateChangeTime();

	/**
	 *
	 * @param time
	 */
	public void setLastStateChangeTime(LocalDateTime time);
}