package io.openems.edge.ess.mr.gridcon.ongrid.state;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;

public class Error extends BaseState implements State {

	private static final long WAITING_TIME = 20;
	private final Logger log = LoggerFactory.getLogger(Error.class);

	public Error(EssGridcon gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3) {
		super(gridconPCS, b1, b2, b3);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be STOPPED, ERROR or UNDEFINED
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED;
		}
		
		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
	}

	@Override
	public void act() {
		log.info("Handle Errors!");
		
		if (!isBatteriesStarted()) {
			startBatteries();
		}
		
		//handle also link voltage too low!!
		if (isLinkVoltageTooLow()) {
			gridconPCS.stopSystem();
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
		
	}

	private boolean isLinkVoltageTooLow() {
		// TODO Auto-generated method stub
		return false;
	}

	private void doStartErrorHandling() {
		// Error Count = anzahl der Fehler
		// Error code = aktueller fehler im channel
		// error code feedback -> channel fürs rückschreiben der fehler damit im error code der nächste auftaucht
		
		errorMap = new ArrayList<Integer>();
		errorCount = gridconPCS.getErrorCount();
		errorHandlingState = ErrorHandlingState.READING_ERRORS;
		
	}
	
	private void doReadErrors() {
		
		int currentErrorCode = gridconPCS.getCurrentErrorCode();
		if (!errorMap.contains(currentErrorCode)) {
			errorMap.add(currentErrorCode);
			gridconPCS.setErrorCodeFeedBack(currentErrorCode);
		} else {
			gridconPCS.setErrorCodeFeedBack(currentErrorCode);
		}
		
		if (errorMap.size() >= errorCount) {
			errorHandlingState = ErrorHandlingState.ACKNOWLEDGE;
		}
	}
	
	private void doAcknowledge() {
		gridconPCS.acknowledgeErrors();
		errorsAcknowledged = LocalDateTime.now();
		errorHandlingState = ErrorHandlingState.WAITING;
	}

	private void finishing() {
		//reset all maps etc.
		
		errorCount = null;
		errorMap = null; 
		errorHandlingState = null;
		errorsAcknowledged = null;
		
	}	
	private void doWait() {
		if (errorsAcknowledged.plusSeconds(WAITING_TIME).isBefore(LocalDateTime.now())) {
			errorHandlingState = ErrorHandlingState.FINISHED;
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
