package io.openems.edge.common.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public abstract class StateHandler<O, C> {
	/**
	 * Holds the main logic of StateMachine State.
	 * 
	 * @param context the {@link Context}.
	 * @return the next State
	 */
	protected abstract O getNextState(C context) throws OpenemsNamedException;

	/**
	 * Gets called before the StateMachine changes from another State to this State.
	 * 
	 * @return
	 */
	protected void onEntry(C context) throws OpenemsNamedException {
	}

	/**
	 * Gets called after the StateMachine changes from this State to another State.
	 */
	protected void onExit(C context) throws OpenemsNamedException {
	}

}