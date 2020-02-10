package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.sum.GridMode;

public class StateMachine {

	private static final int TIME_TOLERANCE_LINK_VOLTAGE = 15;

	protected final GridconPCS gridconPCS;
	protected final EssGridcon essGridcon;

	private LocalDateTime ccuStateIsRunningSince = null;

	private final Logger log = LoggerFactory.getLogger(StateMachine.class);
//	private final GoingOngridHandler goingOngridHandler = new GoingOngridHandler(this);
//	private final GoingOffgridHandler goingOffgridHandler = new GoingOffgridHandler(this);
//	private final OngridHandler ongridHandler = new OngridHandler(this);
//	private final OffgridHandler offgridHandler = new OffgridHandler(this);
	private final ErrorHandler errorHandler = new ErrorHandler(this);

	private State state = State.UNDEFINED;
//	private CCUState lastCcuState = CCUState.UNDEFINED;

	public StateMachine(GridconPCS gridconPCS, EssGridcon essGridcon) {
		this.gridconPCS = gridconPCS;
		this.essGridcon = essGridcon;

//		/*
//		 * Call back for ccu state when ccu state is set to run a time variable is set
//		 * this is important for checking the link voltage because the link voltage is
//		 * not present at start up
//		 */
//		EnumReadChannel ccuStateRunChannel = this.gridconPCS.channel(GridConChannelId.CCU_STATE);
//		ccuStateRunChannel.onChange((oldValue, newValue) -> {
//			CCUState val = newValue.asEnum();
//			if ( val == CCUState.UNDEFINED) {
//				return;
//			}
//
//			if (this.ccuStateIsRunningSince == null && gridconPCSImpl.isRunning()) {
//				this.ccuStateIsRunningSince = LocalDateTime.now();
//			}
//
//			if (this.ccuStateIsRunningSince != null && !gridconPCSImpl.isRunning()) {
//				this.ccuStateIsRunningSince = null; // it is not running
//			}
//		});
	}

	public void run() throws IllegalArgumentException, OpenemsNamedException {
		/*
		 * Check if we have an Error
		 */
		if (this.isError()) {
			this.switchState(State.ERROR);
		}

		/*
		 * Handle State-Machine
		 */
		State nextState = null;
		switch (this.state) {
		case UNDEFINED:
			nextState = this.handleUndefined();
			break;

//		case GOING_ONGRID:
//			nextState = this.goingOngridHandler.run();
//			break;

		case ONGRID:
//			nextState = this.ongridHandler.run();
			break;

//		case GOING_OFFGRID:
//			nextState = this.goingOffgridHandler.run();
//			break;

//		case OFFGRID:
//			nextState = this.offgridHandler.run();
//			break;

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
		GridMode gridMode = this.essGridcon.getGridMode().getNextValue().asEnum();
		if (gridconPCS.isError()) {
			return State.ERROR;
		}

		switch (gridMode) {
		case ON_GRID:
			return State.ONGRID;

		case OFF_GRID:
//			return State.OFFGRID;

		case UNDEFINED:
			this.log.info("StateMachine.handleUndefined() -> staying UNDEFINED, Grid-Mode is [" + gridMode + "]");
			return State.UNDEFINED;
		}
		// should never come here
		assert (true);
		return State.UNDEFINED;
	}

	private boolean isError() {
		boolean result = false;
//		CCUState ccuState = ((EnumReadChannel) this.gridconPCS.channel(GridConChannelId.CCU_STATE)).value().asEnum();
		// CCU State Error
		if (/*this.lastCcuState != ccuState && */gridconPCS.isError()) {
			result = true;
		}
//		this.lastCcuState = ccuState;

		if (gridconPCS.isRunning() && this.isLinkVoltageTooLow()) {
			result = true;
		}
		
		// Is communication broken?
		if (this.isCommunicationBroken()) {
			result = true;
		}

		return result;
	}

	protected boolean isLinkVoltageTooLow() {

		if (ccuStateIsRunningSince == null) {
			return false; // if system is not running, validation is not possible
		}

		if (ccuStateIsRunningSince.plusSeconds(TIME_TOLERANCE_LINK_VOLTAGE).isAfter(LocalDateTime.now())) {
			return false; // system has to run a certain until validation is senseful
		}

		float dcLinkPositiveVoltage = this.gridconPCS.getDcLinkPositiveVoltage();
		if (dcLinkPositiveVoltage <= 0) { // value not valid
			return false;
		}

		float difference = Math.abs(GridconPCSImpl.DC_LINK_VOLTAGE_SETPOINT - dcLinkPositiveVoltage);

		return (difference > GridconPCSImpl.DC_LINK_VOLTAGE_TOLERANCE_VOLT);
	}
	
	// Checks the modbus bridge if communication is available or not
	protected boolean isCommunicationBroken() {
		return this.gridconPCS.isCommunicationBroken();
	}



	/**
	 * Switches to the next state.
	 * 
	 * @param nextState
	 */
	private void switchState(State nextState) {
		// initialize all Handlers
//		this.errorHandler.initialize(); //if error handler is always initialized newly, it has always state undef

		// TODO check set state to next state?
		this.state = nextState;
	}

	public State getState() {
		return state;
	}

//	public OngridHandler getOngridHandler() {
//		return ongridHandler;
//	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
//		GOING_ONGRID(1, "Going On-Grid"), //
		ONGRID(2, "On-Grid"), //
//		GOING_OFFGRID(3, "Going Off-Grid"), //
//		OFFGRID(4, "Off-Grid"), //
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
