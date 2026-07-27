package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		GO_RUNNING(10), //
		RUNNING(11), //

		FIRMWARE_UPDATE(12), //

		GO_STOPPED(20), //
		STOPPED(21), //

		ERROR(30), //
		;

		private final int value;

		private State(int value) {
			this.value = value;
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
		public State[] getStates() {
			return State.values();
		}
	}

	/**
	 * Abstract StateHandler base class for fenecon home battery.
	 */
	public abstract static class BatteryStateHandler extends StateHandler<State, Context> {
		/**
		 * Returns whether charge is allowed in the current state. Called every cycle by
		 * battery protection.
		 *
		 * @param context the Context object
		 * @return true if charge is allowed, false if not.
		 */
		public abstract boolean isChargeAllowed(Context context);

		/**
		 * Returns whether discharge is allowed in the current state. Called every cycle
		 * by battery protection.
		 *
		 * @param context the Context object
		 * @return true if discharge is allowed, false if not.
		 */
		public abstract boolean isDischargeAllowed(Context context);
	}

	private volatile boolean isChargeAllowed = false;
	private volatile boolean isDischargeAllowed = false;

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public BatteryStateHandler getStateHandler(State state) {
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case GO_RUNNING -> new GoRunningHandler();
		case RUNNING -> new RunningHandler();
		case FIRMWARE_UPDATE -> new FirmwareUpdateHandler();
		case GO_STOPPED -> new GoStoppedHandler();
		case STOPPED -> new StoppedHandler();
		case ERROR -> new ErrorHandler();
		};
	}

	@Override
	protected void applyStateData(Context context) {
		var currentState = this.getStateHandler(this.getCurrentState());
		this.isChargeAllowed = currentState.isChargeAllowed(context);
		this.isDischargeAllowed = currentState.isDischargeAllowed(context);
	}

	public boolean isChargeAllowed() {
		return this.isChargeAllowed;
	}

	public boolean isDischargeAllowed() {
		return this.isDischargeAllowed;
	}
}