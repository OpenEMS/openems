package io.openems.edge.ess.mr.gridcon.ongrid.state.gridconstate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Error extends BaseState implements StateObject {

	private static final long WAITING_TIME = 20;
	private final Logger log = LoggerFactory.getLogger(Error.class);
	
	private boolean enableIPU1;
	private boolean enableIPU2;
	private boolean enableIPU3;
	private ParameterSet parameterSet;
	
	public Error(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet ) {
		super(manager, gridconPCSId, b1Id, b2Id, b3Id);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.state.gridconstate.GridconState.ERROR;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be STOPPED, ERROR or UNDEFINED
		
		if (errorHandlingState  != null) {
			return io.openems.edge.ess.mr.gridcon.ongrid.state.gridconstate.GridconState.ERROR;
		}
		
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.state.gridconstate.GridconState.UNDEFINED;
		}
		
		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.state.gridconstate.GridconState.STOPPED;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.state.gridconstate.GridconState.ERROR;
	}

	@Override
	public void act() {
		log.info("Handle Errors!");
		
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();

		if (!isBatteriesStarted()) {			
			keepSystemStopped();
			startBatteries();
			try {
				getGridconPCS().doWriteTasks();
			} catch (OpenemsNamedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
				
		//handle also link voltage too low!!
		if (isLinkVoltageTooLow()) {
			getGridconPCS().setStop(true);
			try {
				getGridconPCS().doWriteTasks();
			} catch (OpenemsNamedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		// first step, read number of errors, then read errors while complete error list is filled
		
		//TODO sub state machine: start -> reading errors -> acknowledging --> waiting for a certain period --> finished
		
		if (errorHandlingState == null) {
			errorHandlingState = ErrorHandlingState.START;
		}
		
		switch (errorHandlingState) {
		case START:
			doStartErrorHandling();
			break;
		case READING_ERRORS:
			doReadErrors();
			break;
		case ACKNOWLEDGE:
			doAcknowledge();
			break;
		case WAITING:
			doWait();
			break;
		case FINISHED:
			finishing();
			break;
		}		
		try {
			getGridconPCS().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void keepSystemStopped() {		
		System.out.println("Keep system stopped!");
		getGridconPCS().setEnableIPU1(false);
		getGridconPCS().setEnableIPU2(false);
		getGridconPCS().setEnableIPU3(false);
		getGridconPCS().disableDCDC();
		
		getGridconPCS().setStop(true);
		getGridconPCS().setPlay(false);
		getGridconPCS().setAcknowledge(false);
		
		getGridconPCS().setSyncApproval(true);
		getGridconPCS().setBlackStartApproval(false);
		getGridconPCS().setModeSelection(Mode.CURRENT_CONTROL);
		getGridconPCS().setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		getGridconPCS().setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		getGridconPCS().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPCS().setQLimit(GridconPCS.Q_LIMIT);
		getGridconPCS().setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		getGridconPCS().setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			getGridconPCS().setPMaxChargeIPU1(maxPower);
			getGridconPCS().setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			getGridconPCS().setPMaxChargeIPU2(maxPower);
			getGridconPCS().setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			getGridconPCS().setPMaxChargeIPU3(maxPower);
			getGridconPCS().setPMaxDischargeIPU3(-maxPower);
		}		
}
	
	private boolean isLinkVoltageTooLow() {
		// TODO Auto-generated method stub
		return false;
	}

	private void doStartErrorHandling() {
		System.out.println("doStartErrorHandling");
		// Error Count = anzahl der Fehler
		// Error code = aktueller fehler im channel
		// error code feedback -> channel fürs rückschreiben der fehler damit im error code der nächste auftaucht
		
		errorMap = new ArrayList<Integer>();
		errorCount = getGridconPCS().getErrorCount();
		errorHandlingState = ErrorHandlingState.READING_ERRORS;
		//getGridconPCS().setStop(true);
		keepSystemStopped();		
	}
	
	private void doReadErrors() {
		System.out.println("doReadErrors");
		int currentErrorCode = getGridconPCS().getErrorCode();
		if (!errorMap.contains(currentErrorCode)) {
			errorMap.add(currentErrorCode);
			getGridconPCS().setErrorCodeFeedback(currentErrorCode);
		} else {
			getGridconPCS().setErrorCodeFeedback(currentErrorCode);
		}
		
		if (errorMap.size() >= errorCount) {
			errorHandlingState = ErrorHandlingState.ACKNOWLEDGE;
		}
	}
	
	private void doAcknowledge() {
		System.out.println("doAcknowledge");
		errorsAcknowledged = LocalDateTime.now();
		errorHandlingState = ErrorHandlingState.WAITING;
		
		
		getGridconPCS().setEnableIPU1(false);
		getGridconPCS().setEnableIPU2(false);
		getGridconPCS().setEnableIPU3(false);
		getGridconPCS().disableDCDC();
		
		getGridconPCS().setStop(true);
		getGridconPCS().setPlay(false);
		getGridconPCS().setAcknowledge(true);
		
		getGridconPCS().setSyncApproval(true);
		getGridconPCS().setBlackStartApproval(false);
		getGridconPCS().setModeSelection(Mode.CURRENT_CONTROL);
		getGridconPCS().setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		getGridconPCS().setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		getGridconPCS().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPCS().setQLimit(GridconPCS.Q_LIMIT);
		getGridconPCS().setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		getGridconPCS().setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			getGridconPCS().setPMaxChargeIPU1(maxPower);
			getGridconPCS().setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			getGridconPCS().setPMaxChargeIPU2(maxPower);
			getGridconPCS().setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			getGridconPCS().setPMaxChargeIPU3(maxPower);
			getGridconPCS().setPMaxDischargeIPU3(-maxPower);
		}		
	}

	private void finishing() {
		System.out.println("finishing");
		//reset all maps etc.
		
		errorCount = null;
		errorMap = null; 
		errorHandlingState = null;
		errorsAcknowledged = null;
		
	}	
	private void doWait() {
		System.out.println("doWait");
		if (errorsAcknowledged.plusSeconds(WAITING_TIME).isBefore(LocalDateTime.now())) {
			
			if (getGridconPCS().isError()) {
				System.out.println("Gridcon has still errors.... :-(  start from the beginning");
				finishing(); // to reset all maps etc...
				
			}
			
			errorHandlingState = ErrorHandlingState.FINISHED;
		} else {
			System.out.println("we are still waiting");
		}
		
	}

	Integer errorCount = null;
	private Collection<Integer> errorMap = null; 
	private ErrorHandlingState errorHandlingState = null;
	LocalDateTime errorsAcknowledged = null;
	
	private enum ErrorHandlingState {
		START,
		READING_ERRORS,
		ACKNOWLEDGE,
		WAITING,
		FINISHED		
	}
}
