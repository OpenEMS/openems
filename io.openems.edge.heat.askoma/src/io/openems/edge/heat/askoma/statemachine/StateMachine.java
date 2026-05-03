package io.openems.edge.heat.askoma.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.askoma.Mode;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		OFF(1), //
		FAST_HEAT(2), //
		FAST_HEAT_PAUSE(3), //
		SURPLUS(4); //

		private final int value;

		State(int value) {
			this.value = value;
		}

		@Override
		public State[] getStates() {
			return State.values();
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
			return null;
		}
	}

	public StateMachine(State initialState) {
		super(initialState);
	}

	/**
	 * Maps a configured {@link Mode} to the corresponding initial state-machine
	 * state.
	 *
	 * @param mode the resolved operating mode
	 * @return the matching state-machine state
	 */
	public static State fromMode(Mode mode) {
		return switch (mode) {
		case OFF -> State.OFF;
		case FAST_HEAT -> State.FAST_HEAT;
		case SURPLUS -> State.SURPLUS;
		};
	}

	/**
	 * Checks whether the given state-machine state belongs to the resolved
	 * operating mode.
	 *
	 * <p>
	 * This keeps {@link State#FAST_HEAT_PAUSE} within {@link Mode#FAST_HEAT}
	 * without forcing a transition back to {@link State#FAST_HEAT} on every cycle.
	 *
	 * @param state the current state-machine state
	 * @param mode  the resolved operating mode
	 * @return {@code true} if the state already matches the mode
	 */
	public static boolean matchesMode(State state, Mode mode) {
		return switch (mode) {
		case OFF -> state == State.OFF;
		case FAST_HEAT -> state == State.FAST_HEAT || state == State.FAST_HEAT_PAUSE;
		case SURPLUS -> state == State.SURPLUS;
		};
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		return switch (state) {
		case OFF -> new OffHandler();
		case FAST_HEAT -> new FastHeatHandler();
		case FAST_HEAT_PAUSE -> new FastHeatPauseHandler();
		case SURPLUS -> new SurplusHandler();
		};
	}
}
