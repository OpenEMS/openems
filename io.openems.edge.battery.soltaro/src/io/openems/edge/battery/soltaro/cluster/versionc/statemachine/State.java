package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.StateHandler;

public enum State implements io.openems.edge.common.statemachine.State<State, Context>, OptionsEnum {
	UNDEFINED(-1, new Undefined()), //

	GO_RUNNING(10, new GoRunning()), //
	RUNNING(11, new Running()), //

	GO_STOPPED(20, new GoStopped()), //
	STOPPED(21, new Stopped()), //

	ERROR_HANDLING(30, new ErrorHandling()), //
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
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
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
