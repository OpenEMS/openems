package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;

public enum State implements OptionsEnum {
	UNDEFINED(-1, new Undefined()), //

	GO_RUNNING(10, new GoRunning()), //
	RUNNING(11, new Running()), //

	GO_STOPPED(20, new GoStopped()), //
	STOPPED(21, new Stopped()), //

	ERROR_HANDLING(30, new ErrorHandling()), //

	GO_CONFIGURATION(40, new GoConfiguration()), //
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
	protected final Handler handler;

	private State(int value, Handler handler) {
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

	/*
	 * Map Handler methods to enum for fluent access.
	 */

	/**
	 * Holds the main logic of StateMachine State.
	 * 
	 * @param context the {@link Context}
	 * @return the next State
	 */
	protected State getNextState(Context context) throws OpenemsNamedException {
		return this.handler.getNextState(context);
	}

	/**
	 * Gets called before the StateMachine changes from another State to this State.
	 */
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.handler.onEntry(context);
	}

	/**
	 * Gets called after the StateMachine changes from this State to another State.
	 */
	protected void onExit(Context context) throws OpenemsNamedException {
		this.handler.onExit(context);
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
