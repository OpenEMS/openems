package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class Error extends BaseState {

	// TODO hard restart zeit für jeden durchlauf mit einem faktor z.b. 2
	// multiplizieren -->
	// 1 Versuch nach 1 minute, 2. versuch nach 2 min., 3 vers. nach 4 min...usw..
	//

	private static final long WAITING_TIME_ERRORS = 20;
	private static final long WAITING_TIME_HARD_RESTART = 70;
	private static final float MAX_ALLOWED_DELTA_LINK_VOLTAGE = 20;
	private static final long COMMUNICATION_TIMEOUT = 70; // After 70 seconds gridcon should be ready
	private final Logger log = LoggerFactory.getLogger(Error.class);

	private boolean enableIpu1;
	private boolean enableIpu2;
	private boolean enableIpu3;

	private long secondsToWait = WAITING_TIME_ERRORS;
	private LocalDateTime communicationBrokenSince;

	public Error(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id,
			boolean enableIpu1, boolean enableIpu2, boolean enableIpu3, // ParameterSet parameterSet,
			String hardRestartRelayAdress) {
		super(manager, gridconPcsId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
		this.enableIpu1 = enableIpu1;
		this.enableIpu2 = enableIpu2;
		this.enableIpu3 = enableIpu3;
		// this.parameterSet = parameterSet;
	}

	@Override
	public IState getState() {
		return GridconState.ERROR;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be STOPPED, ERROR or
		// UNDEFINED

		if (this.errorHandlingState != null) {
			return GridconState.ERROR;
		}

		if (this.isNextStateUndefined()) {
			return GridconState.UNDEFINED;
		}

		if (this.isNextStateStopped()) {
			return GridconState.STOPPED;
		}

		return GridconState.ERROR;
	}

	@Override
	public void act(GridconSettings settings) {
		this.log.info("Handle Errors!");

		setStringWeighting();
		setStringControlMode();
		setDateAndTime();

		if (!isBatteriesStarted()) {
			System.out.println("In error --> start batteries");
			this.keepSystemStopped(settings);
			startBatteries();
			try {
				getGridconPcs().doWriteTasks();
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		}

		if (getGridconPcs().isCommunicationBroken() && this.errorHandlingState == null) {
			System.out.println("Communication broken!");
			if (this.communicationBrokenSince == null) {
				this.communicationBrokenSince = LocalDateTime.now();
				System.out.println("Comm broken --> set timestamp!");
			}
			if (this.communicationBrokenSince.plusSeconds(COMMUNICATION_TIMEOUT).isAfter(LocalDateTime.now())) {
				System.out.println("comm broken --> in waiting time!");
				return;
			} else {
				System.out.println("comm broken --> hard reset!");
				this.communicationBrokenSince = null;
				this.errorHandlingState = ErrorHandlingState.HARD_RESTART;
			}
		}

		// handle also link voltage too low!!
		if (getGridconPcs().isRunning() && this.isLinkVoltageTooLow()) {
			System.out.println("In error --> link voltage too low");
			getGridconPcs().setStop(true);
			try {
				getGridconPcs().doWriteTasks();
			} catch (OpenemsNamedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		// first step, read number of errors, then read errors while complete error list
		// is filled

		// TODO sub state machine: start -> reading errors -> acknowledging --> waiting
		// for a certain period --> finished

		if (this.errorHandlingState == null) {
			this.errorHandlingState = ErrorHandlingState.START;
		}

		switch (this.errorHandlingState) {
		case START:
			this.doStartErrorHandling(settings);
			break;
		case READING_ERRORS:
			this.doReadErrors();
			break;
		case ACKNOWLEDGE:
			this.doAcknowledge(settings);
			break;
		case WAITING:
			this.doWait();
			break;
		case FINISHED:
			this.finishing();
			break;
		case HARD_RESTART:
			this.doHardRestart();
			break;
		}
		try {
			getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int hardRestartCnt = 0;

	private void doHardRestart() {
		System.out.println(" ---> HARD RESET <---");
		if (this.hardRestartCnt < 10) {
			System.out.println(" ---> HARD RESET counting <---");
			this.hardRestartCnt++;
			setHardRestartRelay(true);
		} else {
			System.out.println(" ---> HARD RESET get to waiting<---");
			this.hardRestartCnt = 0;
			setHardRestartRelay(false);
			this.errorHandlingState = ErrorHandlingState.WAITING;
			this.errorsAcknowledged = LocalDateTime.now();
			this.secondsToWait = WAITING_TIME_HARD_RESTART;
		}
	}

	private void keepSystemStopped(GridconSettings settings) {
		System.out.println("Keep system stopped!");
		getGridconPcs().setEnableIpu1(false);
		getGridconPcs().setEnableIpu2(false);
		getGridconPcs().setEnableIpu3(false);
		getGridconPcs().disableDcDc();

		getGridconPcs().setStop(true);
		getGridconPcs().setPlay(false);
		getGridconPcs().setAcknowledge(false);

		getGridconPcs().setMode(settings.getMode());
		getGridconPcs().setU0(settings.getU0());
		getGridconPcs().setF0(settings.getF0());
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		// getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (this.enableIpu1) {
			getGridconPcs().setPMaxChargeIpu1(maxPower);
			getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (this.enableIpu2) {
			getGridconPcs().setPMaxChargeIpu2(maxPower);
			getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (this.enableIpu3) {
			getGridconPcs().setPMaxChargeIpu3(maxPower);
			getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

	private boolean isLinkVoltageTooLow() {
		return GridconPcs.DC_LINK_VOLTAGE_SETPOINT
				- getGridconPcs().getDcLinkPositiveVoltage() > MAX_ALLOWED_DELTA_LINK_VOLTAGE;
	}

	private void doStartErrorHandling(GridconSettings settings) {
		System.out.println("doStartErrorHandling");
		// Error Count = anzahl der Fehler
		// Error code = aktueller fehler im channel
		// error code feedback -> channel fürs rückschreiben der fehler damit im error
		// code der nächste auftaucht

		this.errorCollection = new ArrayList<Integer>();
		this.errorCount = getGridconPcs().getErrorCount();
		this.errorHandlingState = ErrorHandlingState.READING_ERRORS;
		// getGridconPCS().setStop(true);
		this.keepSystemStopped(settings);
	}

	private void doReadErrors() {
		System.out.println("doReadErrors");
		int currentErrorCode = getGridconPcs().getErrorCode();
		if (!this.errorCollection.contains(currentErrorCode)) {
			this.errorCollection.add(currentErrorCode);
			getGridconPcs().setErrorCodeFeedback(currentErrorCode);
		} else {
			getGridconPcs().setErrorCodeFeedback(currentErrorCode);
		}

		if (this.errorCollection.size() >= this.errorCount) {
			this.errorHandlingState = ErrorHandlingState.ACKNOWLEDGE;
			// write errors
			this.printErrors(this.errorCollection);
		}
	}

	private void printErrors(Collection<Integer> errorCollection) {
		for (int i : errorCollection) {
			this.printError(i);
		}
	}

	private void printError(int errorCode) {
		for (ErrorCodeChannelId0 id : ErrorCodeChannelId0.values()) {
			this.printErrorIfCorresponding(errorCode, id);
		}
		for (ErrorCodeChannelId1 id : ErrorCodeChannelId1.values()) {
			this.printErrorIfCorresponding(errorCode, id);
		}
	}

	private void printErrorIfCorresponding(int errorCode, ChannelId id) {
		ErrorDoc errorDoc = (ErrorDoc) id.doc();
		if (errorDoc.getCode() == errorCode) {
			System.out.println(errorDoc.getText());
		}
	}

	private void doAcknowledge(GridconSettings settings) {
		System.out.println("doAcknowledge");
		this.errorsAcknowledged = LocalDateTime.now();
		this.errorHandlingState = ErrorHandlingState.WAITING;
		this.secondsToWait = WAITING_TIME_ERRORS;

		getGridconPcs().setEnableIpu1(false);
		getGridconPcs().setEnableIpu2(false);
		getGridconPcs().setEnableIpu3(false);
		getGridconPcs().disableDcDc();

		getGridconPcs().setStop(true);
		getGridconPcs().setPlay(false);
		getGridconPcs().setAcknowledge(true);

		getGridconPcs().setMode(settings.getMode());
		getGridconPcs().setU0(settings.getU0());
		getGridconPcs().setF0(settings.getF0());
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		// getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (this.enableIpu1) {
			getGridconPcs().setPMaxChargeIpu1(maxPower);
			getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (this.enableIpu2) {
			getGridconPcs().setPMaxChargeIpu2(maxPower);
			getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (this.enableIpu3) {
			getGridconPcs().setPMaxChargeIpu3(maxPower);
			getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

	private void finishing() {
		System.out.println("finishing");
		// reset all maps etc.

		this.errorCount = null;
		this.errorCollection = null;
		this.errorHandlingState = null;
		this.errorsAcknowledged = null;

	}

	private void doWait() {
		System.out.println("doWait");

		if (this.errorsAcknowledged.plusSeconds(this.secondsToWait).isBefore(LocalDateTime.now())) {

			if (getGridconPcs().isError()) {
				System.out.println("Gridcon has still errors.... :-(  start from the beginning");
				this.finishing(); // to reset all maps etc...
			}

			this.errorHandlingState = ErrorHandlingState.FINISHED;
		} else {
			System.out.println("we are still waiting");
		}

	}

	private Integer errorCount = null;
	private Collection<Integer> errorCollection = null;
	private ErrorHandlingState errorHandlingState = null;
	private LocalDateTime errorsAcknowledged = null;

	private enum ErrorHandlingState {
		START, READING_ERRORS, ACKNOWLEDGE, WAITING, FINISHED, HARD_RESTART
	}
}
