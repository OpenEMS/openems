package io.openems.edge.ess.mr.gridcon.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;

public class StateMachine {

	protected final GridconPCS parent;

	private final GoingOngridHandler goingOngridHandler = new GoingOngridHandler(this);
	private final GoingOffgridHandler goingOffgridHandler = new GoingOffgridHandler(this);
	private final OngridHandler ongridHandler = new OngridHandler(this);
	private final OffgridHandler offgridHandler = new OffgridHandler(this);
	private final ErrorHandler errorHandler = new ErrorHandler(this);

	private State state = State.UNDEFINED;
	private GridMode lastGridMode = GridMode.UNDEFINED;
	private CCUState lastCcuState = CCUState.UNDEFINED;

	public StateMachine(GridconPCS parent) {
		this.parent = parent;
	}

	public void run() throws IllegalArgumentException, OpenemsNamedException {
		/*
		 * Check if we just went On-/Off-Grid
		 */
		GridMode gridMode = this.parent.getGridMode().getNextValue().asEnum();
		if (this.lastGridMode != gridMode) {
			// Grid-Mode changed
			switch (gridMode) {
			case ON_GRID:
				this.switchState(State.GOING_ONGRID);
				break;
			case OFF_GRID:
				this.switchState(State.GOING_OFFGRID);
				break;
			case UNDEFINED:
				break;
			}
		}
		this.lastGridMode = gridMode;

		/*
		 * Check if we have an Error
		 */
		CCUState ccuState = this.getCcuState();
		if (this.lastCcuState != ccuState) {
			if (ccuState == CCUState.ERROR) {
				this.switchState(State.ERROR);
			}
		}

		/*
		 * Handle State-Machine
		 */
		State nextState = null;
		switch (this.state) {
		case UNDEFINED:
			nextState = this.handleUndefined();
			break;

		case GOING_ONGRID:
			nextState = this.goingOngridHandler.run();
			break;

		case ONGRID:
			nextState = this.ongridHandler.run();
			break;

		case GOING_OFFGRID:
			nextState = this.goingOffgridHandler.run();
			break;

		case OFFGRID:
			nextState = this.offgridHandler.run();
			break;

		case ERROR:
			nextState = this.errorHandler.run();
			break;
		}
		if (nextState != this.state) {
			this.switchState(nextState);
		}
	}

	/**
	 * Evaluates the current State.
	 * 
	 * @return
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private State handleUndefined() {
		GridMode gridMode = this.parent.getGridMode().getNextValue().asEnum();
		CCUState ccuState = this.getCcuState();
		if (ccuState == CCUState.ERROR) {
			return State.ERROR;
		}

		switch (gridMode) {
		case ON_GRID:
			return State.ONGRID;

		case OFF_GRID:
			return State.OFFGRID;

		case UNDEFINED:
			return State.UNDEFINED;
		}
		return State.UNDEFINED;
	}

	/**
	 * Gets the CCUState of the MR internal State-Machine.
	 * 
	 * @return the CCUState
	 */
	protected CCUState getCcuState() {
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_ERROR)).value().asOptional()
				.orElse(false)) {
			return CCUState.ERROR;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_IDLE)).value().asOptional()
				.orElse(false)) {
			return CCUState.IDLE;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_PRECHARGE)).value().asOptional()
				.orElse(false)) {
			return CCUState.PRECHARGE;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_STOP_PRECHARGE)).value().asOptional()
				.orElse(false)) {
			return CCUState.STOP_PRECHARGE;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_READY)).value().asOptional()
				.orElse(false)) {
			return CCUState.READY;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_PAUSE)).value().asOptional()
				.orElse(false)) {
			return CCUState.PAUSE;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_RUN)).value().asOptional()
				.orElse(false)) {
			return CCUState.RUN;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_VOLTAGE_RAMPING_UP)).value()
				.asOptional().orElse(false)) {
			return CCUState.VOLTAGE_RAMPING_UP;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_OVERLOAD)).value().asOptional()
				.orElse(false)) {
			return CCUState.OVERLOAD;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_SHORT_CIRCUIT_DETECTED)).value()
				.asOptional().orElse(false)) {
			return CCUState.SHORT_CIRCUIT_DETECTED;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_DERATING_POWER)).value().asOptional()
				.orElse(false)) {
			return CCUState.DERATING_POWER;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_DERATING_HARMONICS)).value()
				.asOptional().orElse(false)) {
			return CCUState.DERATING_HARMONICS;
		}
		if (((BooleanReadChannel) this.parent.channel(GridConChannelId.CCU_STATE_SIA_ACTIVE)).value().asOptional()
				.orElse(false)) {
			return CCUState.SIA_ACTIVE;
		}
		return CCUState.UNDEFINED;
	}

	/**
	 * Switches to the next state.
	 * 
	 * @param nextState
	 */
	private void switchState(State nextState) {
		// initialize all Handlers
		this.errorHandler.initialize();

	}

	public State getState() {
		return state;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		GOING_ONGRID(1, "Going On-Grid"), //
		ONGRID(2, "On-Grid"), //
		GOING_OFFGRID(3, "Going Off-Grid"), //
		OFFGRID(4, "Off-Grid"), //
		ERROR(5, "Error");

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
