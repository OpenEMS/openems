package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import java.util.List;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.soltaro.single.versionc.Config;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.common.channel.ChannelId;

public enum StateMachine implements OptionsEnum {
	UNDEFINED(-1, new Undefined()), //

	GO_RUNNING(10, new GoRunning()), //
	RUNNING(11, new Running()), //

	GO_STOPPED(20, new GoStopped()), //
	STOPPED(21, new Stopped()), //

	GO_ERROR_HANDLING(30, new GoErrorHandling()), //
	ERROR_HANDLING(31, new ErrorHandling());

	public abstract static class Handler {
		/**
		 * Holds the main logic of StateMachine State.
		 * 
		 * @param context the Context.
		 * @return the next State
		 */
		protected abstract StateMachine getNextState(Context context);

		/**
		 * Gets called before the StateMachine changes from another State to this State.
		 * 
		 * @return
		 */
		protected void onEntry() {
		}

		/**
		 * Gets called after the StateMachine changes from this State to another State.
		 */
		protected void onExit() {
		}
	}

	public static class Context {

		// Input values
		public final Config config;
		public final PreChargeControl preChargeControl;
		public final List<ChannelId> faults;

		// Output values
		public Boolean setReadyForWorking = null;
		public PreChargeControl setPreChargeControl = null;

		public Context(Config config, PreChargeControl preChargeControl, List<ChannelId> faults) {
			this.config = config;
			this.faults = faults;
			this.preChargeControl = preChargeControl;
		}

	}

	private final int value;
	private final Handler handler;

	private StateMachine(int value, Handler handler) {
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
	 * @param context the Context.
	 * @return the next State
	 */
	public StateMachine getNextState(Context context) {
		return this.handler.getNextState(context);
	}

	/**
	 * Gets called before the StateMachine changes from another State to this State.
	 * 
	 * @return
	 */
	public void onEntry() {
		this.handler.onEntry();
	}

	/**
	 * Gets called after the StateMachine changes from this State to another State.
	 */
	public void onExit() {
		this.handler.onExit();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
