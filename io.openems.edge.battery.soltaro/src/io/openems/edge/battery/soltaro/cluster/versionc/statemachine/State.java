package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.StateHandler;

public enum State implements io.openems.edge.common.statemachine.State<State, Context>, OptionsEnum {
	UNDEFINED(-1, new UndefinedHandler()), //

	GO_RUNNING(10, new GoRunningHandler()), //
	RUNNING(11, new RunningHandler()), //

	GO_STOPPED(20, new GoStoppedHandler()), //
	STOPPED(21, new StoppedHandler()), //

	ERROR(30, new ErrorHandler()), //
	;

	protected abstract static class Handler {
		/**
		 * Holds the main logic of StateMachine State.
		 * 
		 * @param context the {@link Context}.
		 * @return the next State
		 */
		protected abstract State getNextState(Context context) throws OpenemsNamedException;

		/**
		 * Gets called before the StateMachine changes from another State to this State.
		 * 
		 * @return
		 */
		protected void onEntry(Context context) throws OpenemsNamedException {
		}

		/**
		 * Gets called after the StateMachine changes from this State to another State.
		 */
		protected void onExit(Context context) throws OpenemsNamedException {
		}
	}

	private final int value;
	protected final StateHandler<State, Context> handler;

	private State(int value, StateHandler<State, Context> handler) {
		this.value = value;
		this.handler = handler;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	@Override
	public StateHandler<State, Context> getHandler() {
		return this.handler;
	}
}
