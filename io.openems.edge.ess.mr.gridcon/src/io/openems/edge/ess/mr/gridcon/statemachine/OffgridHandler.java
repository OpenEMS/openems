package io.openems.edge.ess.mr.gridcon.statemachine;

import io.openems.common.types.OptionsEnum;

public class OffgridHandler {

	private final StateMachine parent;

	private State state = State.UNDEFINED;

	public OffgridHandler(StateMachine parent) {
		this.parent = parent;
	}

	public void initialize() {
		this.state = State.UNDEFINED;
	}

	protected StateMachine.State run() {
		return StateMachine.State.OFFGRID;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"); //

		private final int value;
		private final String name;

		private State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}
}
