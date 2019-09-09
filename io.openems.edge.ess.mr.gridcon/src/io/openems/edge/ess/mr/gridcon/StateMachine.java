package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;

public class StateMachine {

	private static final int TIME_TOLERANCE_LINK_VOLTAGE = 15;

	protected final GridconPCS parent;

	private LocalDateTime ccuStateIsRunningSince = null;

	private final Logger log = LoggerFactory.getLogger(StateMachine.class);
	private final GoingOngridHandler goingOngridHandler = new GoingOngridHandler(this);
	private final GoingOffgridHandler goingOffgridHandler = new GoingOffgridHandler(this);
	private final OngridHandler ongridHandler = new OngridHandler(this);
	private final OffgridHandler offgridHandler = new OffgridHandler(this);
	private final ErrorHandler errorHandler = new ErrorHandler(this);

	private State state = State.UNDEFINED;
	private CCUState lastCcuState = CCUState.UNDEFINED;

	public StateMachine(GridconPCS parent) {
		this.parent = parent;

		/*
		 * Call back for ccu state when ccu state is set to run a time variable is set
		 * this is important for checking the link voltage because the link voltage is
		 * not present at start up
		 */
		BooleanReadChannel ccuStateRunChannel = this.parent.channel(GridConChannelId.CCU_STATE_RUN);
		ccuStateRunChannel.onChange(v -> {
			Optional<Boolean> val = v.asOptional();
			if (!val.isPresent()) {
				return;
			}

			if (this.ccuStateIsRunningSince == null && val.get()) {
				this.ccuStateIsRunningSince = LocalDateTime.now();
			}

			if (this.ccuStateIsRunningSince != null && !val.get()) {
				this.ccuStateIsRunningSince = null; // it is not running
			}
		});
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
			this.log.info("StateMachine.handleUndefined() -> staying UNDEFINED, Grid-Mode is [" + gridMode + "]");
			return State.UNDEFINED;
		}
		// should never come here
		assert (true);
		return State.UNDEFINED;
	}

	private boolean isError() {
		boolean result = false;
		CCUState ccuState = this.getCcuState();
		// CCU State Error
		if (this.lastCcuState != ccuState && ccuState == CCUState.ERROR) {
			result = true;
		}
		this.lastCcuState = ccuState;

		if (ccuState == CCUState.RUN && this.isLinkVoltageTooLow()) {
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

		FloatReadChannel frc = this.parent.channel(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE);
		Optional<Float> linkVoltageOpt = frc.value().asOptional();
		if (!linkVoltageOpt.isPresent()) {
			return false;
		}

		float linkVoltage = linkVoltageOpt.get();
		float difference = Math.abs(GridconPCS.DC_LINK_VOLTAGE_SETPOINT - linkVoltage);

		return (difference > GridconPCS.DC_LINK_VOLTAGE_TOLERANCE_VOLT);
	}
	
	// Checks the modbus bridge if communication is available or not
	protected boolean isCommunicationBroken() {
		String modbusId = this.parent.config.modbus_id();
		ComponentManager manager = this.parent.componentManager;
		AbstractModbusBridge modbusBridge = null;
		try {
			modbusBridge = manager.getComponent(modbusId);
		} catch (OpenemsNamedException e) {			
			log.debug("Cannot get modbus component");
		}
		if (modbusBridge == null) {
			return true;	
		}
		
		 Channel<Boolean> slaveCommunicationFailedChannel = modbusBridge.getSlaveCommunicationFailedChannel();		 
		 Optional<Boolean> communicationFailedOpt = slaveCommunicationFailedChannel.value().asOptional();
		 
		 // If the channel value is present and it is set then the communication is broken
		 if (communicationFailedOpt.isPresent() && communicationFailedOpt.get()) {
			 return true;
		 }
		  
		 return false;
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
//		this.errorHandler.initialize(); //if error handler is always initialized newly, it has always state undef

		// TODO check set state to next state?
		this.state = nextState;
	}

	public State getState() {
		return state;
	}

	public OngridHandler getOngridHandler() {
		return ongridHandler;
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
