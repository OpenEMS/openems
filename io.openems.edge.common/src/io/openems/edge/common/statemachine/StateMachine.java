package io.openems.edge.common.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * Manages the States of the StateMachine.
 */
public class StateMachine<O extends State<O, C>, C> {

	private final Logger log = LoggerFactory.getLogger(StateMachine.class);

	private O state;

	/**
	 * Gets the currently activate State.
	 * 
	 * @return the State
	 */
	public O getCurrentState() {
		return this.state;
	}

	/**
	 * Execute the StateMachine.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void run(C context) throws OpenemsNamedException {
		// Keep last State
		O lastState = this.state;

		OpenemsNamedException exception = null;

		// Call the State Handler and receive next State.
		try {
			this.state = this.state.getHandler().getNextState(context);
		} catch (OpenemsNamedException e) {
			exception = e;
		}

		// Call StateMachine events on transition
		if (lastState != this.state) {
			this.log.info("Changing StateMachine from [" + lastState + "] to [" + this.state + "]");

			// On-Exit of the last State
			try {
				lastState.getHandler().onExit(context);
			} catch (OpenemsNamedException e) {
				if (exception != null) {
					e.addSuppressed(exception);
				}
				exception = e;
			}

			// On-Entry of next State
			try {
				this.state.getHandler().onEntry(context);
			} catch (OpenemsNamedException e) {
				if (exception != null) {
					e.addSuppressed(exception);
				}
				exception = e;
			}
		}

		// Handle Exception
		if (exception != null) {
			throw exception;
		}
	}

}
