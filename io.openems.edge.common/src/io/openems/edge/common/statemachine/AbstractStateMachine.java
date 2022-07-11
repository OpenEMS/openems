package io.openems.edge.common.statemachine;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * Manages the States of the StateMachine.
 *
 * @param <STATE>   the {@link State} type, e.g. typically an enum
 * @param <CONTEXT> the context type, i.e. a class wrapping a State-Machine
 *                  context
 */
public abstract class AbstractStateMachine<STATE extends State<STATE>, CONTEXT extends AbstractContext<?>> {

	private final Logger log = LoggerFactory.getLogger(AbstractStateMachine.class);

	private final Map<STATE, StateHandler<STATE, CONTEXT>> stateHandlers = new HashMap<>();

	private final STATE initialState;

	private STATE previousState;
	private STATE state;

	/**
	 * Initialize the State-Machine and set an initial State.
	 *
	 * <p>
	 * TODO Note that for the initialState the {@link StateHandler#onEntry(Object)}
	 * method is not called in the beginning.
	 *
	 * @param initialState the initial State
	 */
	public AbstractStateMachine(STATE initialState) {
		this.initialState = initialState;
		this.previousState = initialState;
		this.state = initialState;
		for (STATE state : initialState.getStates()) {
			this.stateHandlers.put(state, this.getStateHandler(state));
		}
	}

	/**
	 * Gets the {@link StateHandler} for each State.
	 *
	 * <p>
	 * This method is called once for every available State during construction of
	 * the StateMachine in order to initialize an internal list of StateHandlers.
	 *
	 * @param state the State
	 * @return the {@link StateHandler} for the given State
	 */
	public abstract StateHandler<STATE, CONTEXT> getStateHandler(STATE state);

	/**
	 * Gets the previously activate State.
	 *
	 * @return the State
	 */
	public STATE getPreviousState() {
		return this.previousState;
	}

	/**
	 * Gets the currently activate State.
	 *
	 * @return the State
	 */
	public STATE getCurrentState() {
		return this.state;
	}

	/**
	 * Forcibly change the next State from outside. Use with care!
	 *
	 * <p>
	 * Note that transition events will get called.
	 *
	 * @param state the next State
	 */
	public void forceNextState(STATE state) {
		this.forceNextState = state;
	}

	private STATE forceNextState = null;

	/**
	 * Execute the StateMachine.
	 *
	 * @param context the Context object
	 * @throws OpenemsNamedException on error
	 */
	public void run(CONTEXT context) throws OpenemsNamedException {
		// Keep last State
		this.previousState = this.state;

		OpenemsNamedException exception = null;

		// Evaluate the next State
		STATE nextState;
		if (this.forceNextState != null) {
			// Apply Force-Next-State
			nextState = this.forceNextState;
			this.forceNextState = null;

		} else {
			try {
				// Call the State Handler and receive next State.
				nextState = this.stateHandlers //
						.get(this.state) //
						.runAndGetNextState(context);
			} catch (OpenemsNamedException e) {
				exception = e;
				nextState = this.initialState; // set to initial state on error
			}
		}

		// save next State
		this.state = nextState;

		// Call StateMachine events on transition
		if (this.previousState != this.state) {
			context.logInfo(this.log,
					"Changing StateMachine from [" + this.previousState + "] to [" + this.state + "]");

			// On-Exit of the last State
			try {
				this.stateHandlers //
						.get(this.previousState) //
						.onExit(context);
			} catch (OpenemsNamedException e) {
				if (exception != null) {
					e.addSuppressed(exception);
				}
				exception = e;
			}

			// On-Entry of next State
			try {
				this.stateHandlers //
						.get(this.state) //
						.onEntry(context);
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
