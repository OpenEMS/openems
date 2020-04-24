package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionc.Config;
import io.openems.edge.battery.soltaro.single.versionc.SingleRackVersionC;

/**
 * Manages the States of the StateMachine.
 */
// TODO convert to abstract, reusable class
public class StateMachine {

	public static class Context {
		protected final SingleRackVersionC component;
		protected final Config config;

		public Context(SingleRackVersionC component, Config config) {
			super();
			this.component = component;
			this.config = config;
		}
	}

	private final Logger log = LoggerFactory.getLogger(StateMachine.class);

	private State state = State.UNDEFINED;

	/**
	 * Gets the currently activate {@link State}.
	 * 
	 * @return the State
	 */
	public State getCurrentState() {
		return this.state;
	}

	/**
	 * Execute the StateMachine.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void run(Context context) throws OpenemsNamedException {
		// Keep last State
		State lastState = this.state;

		OpenemsNamedException exception = null;

		// Call the State Handler and receive next State.
		try {
			this.state = this.state.getNextState(context);
		} catch (OpenemsNamedException e) {
			exception = e;
		}

		// Call StateMachine events on transition
		if (lastState != this.state) {
			this.log.info("Changing StateMachine from [" + lastState + "] to [" + this.state + "]");

			// On-Exit of the last State
			try {
				lastState.onExit(context);
			} catch (OpenemsNamedException e) {
				if (exception != null) {
					e.addSuppressed(exception);
				}
				exception = e;
			}

			// On-Entry of next State
			try {
				this.state.onEntry(context);
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
