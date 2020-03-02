package io.openems.edge.ess.mr.gridcon.ongrid.state;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Error extends BaseState implements State {

	private static final long WAITING_TIME = 20;
	private final Logger log = LoggerFactory.getLogger(Error.class);

	
	private boolean enableIPU1;
	private boolean enableIPU2;
	private boolean enableIPU3;
	private ParameterSet parameterSet;
	
	public Error(GridconPCS gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet ) {
		super(gridconPCS, b1, b2, b3);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be STOPPED, ERROR or UNDEFINED
		
		if (errorHandlingState  != null) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
		}
		
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
		
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();

		if (!isBatteriesStarted()) {			
			keepSystemStopped();
			startBatteries();
			return;
		}
				
		//handle also link voltage too low!!
		if (isLinkVoltageTooLow()) {
			gridconPCS.setStop(true);
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
	
	private void keepSystemStopped() {		
		System.out.println("Keep system stopped!");
		gridconPCS.setEnableIPU1(false);
		gridconPCS.setEnableIPU2(false);
		gridconPCS.setEnableIPU3(false);
		gridconPCS.disableDCDC();
		
		gridconPCS.setStop(true);
		gridconPCS.setPlay(false);
		gridconPCS.setAcknowledge(false);
		
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(GridconPCS.Q_LIMIT);
		gridconPCS.setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		gridconPCS.setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
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
		errorCount = gridconPCS.getErrorCount();
		errorHandlingState = ErrorHandlingState.READING_ERRORS;
		//gridconPCS.setStop(true);
		keepSystemStopped();		
	}
	
	private void doReadErrors() {
		System.out.println("doReadErrors");
		int currentErrorCode = gridconPCS.getErrorCode();
		if (!errorMap.contains(currentErrorCode)) {
			errorMap.add(currentErrorCode);
			gridconPCS.setErrorCodeFeedback(currentErrorCode);
		} else {
			gridconPCS.setErrorCodeFeedback(currentErrorCode);
		}
		
		if (errorMap.size() >= errorCount) {
			errorHandlingState = ErrorHandlingState.ACKNOWLEDGE;
		}
	}
	
	private void doAcknowledge() {
		System.out.println("doAcknowledge");
		errorsAcknowledged = LocalDateTime.now();
		errorHandlingState = ErrorHandlingState.WAITING;
		
		
		gridconPCS.setEnableIPU1(false);
		gridconPCS.setEnableIPU2(false);
		gridconPCS.setEnableIPU3(false);
		gridconPCS.disableDCDC();
		
		gridconPCS.setStop(true);
		gridconPCS.setPlay(false);
		gridconPCS.setAcknowledge(true);
		
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(GridconPCS.Q_LIMIT);
		gridconPCS.setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		gridconPCS.setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
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
			
			if (gridconPCS.isError()) {
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
