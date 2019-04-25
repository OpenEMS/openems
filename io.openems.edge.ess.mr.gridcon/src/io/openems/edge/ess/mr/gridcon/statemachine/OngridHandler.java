package io.openems.edge.ess.mr.gridcon.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;

public class OngridHandler {

	private final StateMachine parent;

	private State state = State.UNDEFINED;

	public OngridHandler(StateMachine parent) {
		this.parent = parent;
	}

	public void initialize() {
		this.state = State.UNDEFINED;
	}

	protected StateMachine.State run() throws IllegalArgumentException, OpenemsNamedException {
		// Always set OutputSyncDeviceBridge OFF in On-Grid state
		this.parent.parent.setOutputSyncDeviceBridge(false);

		switch (this.state) {
		case UNDEFINED:
			this.state = this.doUndefined();
			break;

		case IDLE:
			this.state = this.doIdle();
			break;

		case RUN:
			this.state = this.doRun();
			break;
		}

		return StateMachine.State.ONGRID;
	}

	/**
	 * @return the next state
	 */
	private State doUndefined() {
		CCUState ccuState = this.parent.getCcuState();
		switch (ccuState) {
		case RUN:
			return State.RUN;

		case IDLE:
			return State.IDLE;

		case ERROR:
		case DERATING_HARMONICS:
		case DERATING_POWER:
		case OVERLOAD:
		case PAUSE:
		case PRECHARGE:
		case READY:
		case SHORT_CIRCUIT_DETECTED:
		case SIA_ACTIVE:
		case STOP_PRECHARGE:
		case UNDEFINED:
		case VOLTAGE_RAMPING_UP:
			break;
		}
		return State.UNDEFINED;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		IDLE(1, "Idle"), //
		RUN(2, "Run");

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
