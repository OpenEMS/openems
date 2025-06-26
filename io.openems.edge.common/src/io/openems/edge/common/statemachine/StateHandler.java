package io.openems.edge.common.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * Defines a Handler for a State of a {@link AbstractStateMachine}.
 *
 * @param <STATE>   the actual {@link State} type
 * @param <CONTEXT> the context type
 */
public abstract class StateHandler<STATE extends State<STATE>, CONTEXT> {

	/**
	 * Runs the main logic of StateMachine State and returns the next State.
	 *
	 * @param context the {@link CONTEXT}.
	 * @return the next State
	 */
	protected abstract STATE runAndGetNextState(CONTEXT context) throws OpenemsNamedException;

	/**
	 * Gets called before the StateMachine changes from another State to this State.
	 * 
	 * @param context the Context object
	 * @throws OpenemsNamedException on error
	 */
	protected void onEntry(CONTEXT context) throws OpenemsNamedException {
	}

	/**
	 * Gets called after the StateMachine changes from this State to another State.
	 * 
	 * @param context the Context object
	 * @throws OpenemsNamedException on error
	 */
	protected void onExit(CONTEXT context) throws OpenemsNamedException {
	}

	/**
	 * Gets a message that is suitable for a continuous Debug log. Returns 'null' by
	 * default which causes output of the name of the State in Camel-Case.
	 *
	 * @return the debug log output
	 */
	protected String debugLog() {
		return null;
	}
}