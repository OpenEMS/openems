package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc;
import io.openems.edge.ess.mr.gridcon.enums.ErrorStateMachine;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.StateMachine;

/**
 * An instance of this class takes care of handling all Error cases of the
 * Gridcon PCS.
 */
public class ErrorHandler {

	private static final long SWITCH_OFF_TIME_SECONDS = 10;
	private static final long RELOAD_TIME_SECONDS = 45; // Time for Mr Gridcon to boot and come back
	private static final int DELAY_TO_WAIT_FOR_NEW_ERROR_SECONDS = 30;

	private final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
	private final GridconPCS parent;
	private final Map<Integer, io.openems.edge.common.channel.ChannelId> errorChannelIds = new HashMap<>();

	protected LocalDateTime timeWhenErrorsHasBeenAcknowledged = null;

	private Map<io.openems.edge.common.channel.ChannelId, LocalDateTime> readErrorMap = new HashMap<>();
	private ErrorStateMachine state = ErrorStateMachine.READ_ERRORS;
	private int tryToAcknowledgeErrorsCounter = 0;
	private LocalDateTime lastHardReset = null;
	private int MAX_TIMES_FOR_TRYING_TO_ACKNOWLEDGE_ERRORS = 5;
	private int hardResetCounter = 0;
	private int MAX_TIMES_FOR_TRYING_TO_HARD_RESET = 5;

	public ErrorHandler(GridconPCS parent) {
		this.parent = parent;
		this.fillErrorChannelMap();
	}

	protected void handleStateMachine() throws IllegalArgumentException, OpenemsNamedException {
		switch (this.state) {
		case READ_ERRORS:
			this.doReadErrors();
			break;
		case HANDLE_ERRORS:
			this.doHandleErrors();
			break;
		case ACKNOWLEDGE_ERRORS:
			this.doAcknowledgeErrors();
			break;
		case HARD_RESET:
			this.doHardReset();
			break;
		case ERROR_HANDLING_NOT_POSSIBLE:
			this.doErrorHandlingNotPossible();
			break;
		case FINISH_ERROR_HANDLING:
			this.doFinishErrorHandling();
		}
	}

	/**
	 * Reads all active Errors into 'readErrorMap'.
	 */
	private void doReadErrors() {
		if (this.readErrorMap == null) {
			this.readErrorMap = new HashMap<>();
		}

		io.openems.edge.common.channel.ChannelId errorId = this.readCurrentError();
		if (errorId != null) {
			if (!this.readErrorMap.containsKey(errorId)) {
				this.readErrorMap.put(errorId, LocalDateTime.now());
			}
		}
		if (!this.isNewErrorPresent()) {
			// If no new error appeared within the last DELAY_TO_WAIT_FOR_NEW_ERROR_SECONDS
			// -> switch to HANDLE_ERRORS state
			this.state = ErrorStateMachine.HANDLE_ERRORS;
		}
	}

	/**
	 * Is any new error present?.
	 * 
	 * @return true if a a new error was found recently; otherwise false
	 */
	private boolean isNewErrorPresent() {
		for (LocalDateTime time : this.readErrorMap.values()) {
			if (time.plusSeconds(DELAY_TO_WAIT_FOR_NEW_ERROR_SECONDS).isAfter(LocalDateTime.now())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handle Errors.
	 * 
	 * <ul>
	 * <li>Errors are acknowledgeable and we did not try too often -> switch to
	 * ACKNOWLEDGE_ERRORS state
	 * <li>Otherwise -> switch to HARD_RESET state
	 * <li>If Hard-Reset did not work for MAX_TIMES_FOR_TRYING_TO_HARD_RESET times
	 * -> switch to ERROR_HANDLING_NOT_POSSIBLE
	 * </ul>
	 */
	private void doHandleErrors() {
		if (this.isAcknowledgeable()
				&& this.tryToAcknowledgeErrorsCounter < MAX_TIMES_FOR_TRYING_TO_ACKNOWLEDGE_ERRORS) {
			// All errors are acknowledgeable and we did not try too often to acknowledge
			// them -> switch to ACKNOWLEDGE_ERRORS state
			this.state = ErrorStateMachine.ACKNOWLEDGE_ERRORS;

		} else if (this.hardResetCounter < MAX_TIMES_FOR_TRYING_TO_HARD_RESET) {
			this.state = ErrorStateMachine.HARD_RESET;
			this.tryToAcknowledgeErrorsCounter = 0;

		} else {
			this.state = ErrorStateMachine.ERROR_HANDLING_NOT_POSSIBLE;
		}
	}

	/**
	 * Are all errors acknowledgeable, i.e. none of them requires a Hard-Reset.
	 * 
	 * @return true if all errors are acknowledgeable; false otherwise
	 */
	private boolean isAcknowledgeable() {
		for (io.openems.edge.common.channel.ChannelId id : readErrorMap.keySet()) {
			for (io.openems.edge.common.channel.ChannelId id2 : this.errorChannelIds.values()) {
				if (id.equals(id2)) {
					if (id instanceof ErrorCodeChannelId) {
						if (((ErrorDoc) ((ErrorCodeChannelId) id).doc()).isNeedsHardReset()) {
							return false;
						}
					} else if (id instanceof ErrorCodeChannelId1) {
						if (((ErrorDoc) ((ErrorCodeChannelId1) id).doc()).isNeedsHardReset()) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Execute a Hard-Reset, i.e. switch the Gridcon PCS off and on.
	 *
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	private void doHardReset() throws IllegalArgumentException, OpenemsNamedException {
		if (this.lastHardReset == null) {
			this.hardResetCounter = this.hardResetCounter + 1;
			this.lastHardReset = LocalDateTime.now();
			this.parent.setHardResetContactor(true);

		} else {

			if (this.lastHardReset.plusSeconds(SWITCH_OFF_TIME_SECONDS).isAfter(LocalDateTime.now())) {
				// just wait and keep the contactor closed
				this.parent.setHardResetContactor(true);
			}

			if ( //
			this.lastHardReset.plusSeconds(SWITCH_OFF_TIME_SECONDS + RELOAD_TIME_SECONDS).isAfter(LocalDateTime.now()) //
					&& this.lastHardReset.plusSeconds(SWITCH_OFF_TIME_SECONDS).isBefore(LocalDateTime.now()) //
			) {
				this.parent.setHardResetContactor(false); // Open the contactor
			}

			if ( //
			this.lastHardReset.plusSeconds(SWITCH_OFF_TIME_SECONDS + RELOAD_TIME_SECONDS).isBefore(LocalDateTime.now()) //
			) {
				this.parent.setHardResetContactor(false); // Keep contactor open
				// Mr Gridcon should be back, so reset everything to start conditions
				this.lastHardReset = null;
				this.tryToAcknowledgeErrorsCounter = 0;
				this.state = ErrorStateMachine.FINISH_ERROR_HANDLING;
			}
		}
	}

	/**
	 * Acknowledges errors.
	 * 
	 * <p>
	 * Writes read errors to error code feedback. If all errors are written to error
	 * code feedback continue normal operation.
	 * 
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	private void doAcknowledgeErrors() throws IllegalArgumentException, OpenemsNamedException {
		this.tryToAcknowledgeErrorsCounter = this.tryToAcknowledgeErrorsCounter + 1;

		int currentErrorCodeFeedBack = 0;

		if (!readErrorMap.isEmpty()) {
			io.openems.edge.common.channel.ChannelId currentId = null;
			for (io.openems.edge.common.channel.ChannelId id : this.readErrorMap.keySet()) {
				currentId = id;
				break;
			}
			currentErrorCodeFeedBack = ((ErrorDoc) currentId.doc()).getCode();
			this.readErrorMap.remove(currentId);
		}

		new CommandControlRegisters() //
				// Acknowledge error
				.acknowledge(true) //
				.syncApproval(true) //
				.blackstartApproval(false).errorCodeFeedback(currentErrorCodeFeedBack) //
				.shortCircuitHandling(true) //
				.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
				.parameterSet1(true) //
				.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
				.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
				.enableIpus(this.parent.config.inverterCount()) //
				.writeToChannels(this.parent);

		if (this.readErrorMap.isEmpty()) {
			this.timeWhenErrorsHasBeenAcknowledged = LocalDateTime.now();
			this.state = ErrorStateMachine.FINISH_ERROR_HANDLING;
		}
	}

	/**
	 * It is impossible to handle the error. Just set an error state and wait for
	 * human help.
	 */
	private void doErrorHandlingNotPossible() {
		// switch off system
		// TODO switch off
		this.parent.channel(GridConChannelId.STATE_CYCLE_ERROR).setNextValue(true);
	}

	private io.openems.edge.common.channel.ChannelId readCurrentError() {
		StateChannel errorChannel = this.getErrorChannel();
		if (errorChannel != null) {
			return errorChannel.channelId();
		}
		return null;
	}

	private void fillErrorChannelMap() {
		// TODO move to static map in Enum
		for (io.openems.edge.common.channel.ChannelId id : ErrorCodeChannelId.values()) {
			this.errorChannelIds.put(((ErrorDoc) id.doc()).getCode(), id);
		}
		for (io.openems.edge.common.channel.ChannelId id : ErrorCodeChannelId1.values()) {
			this.errorChannelIds.put(((ErrorDoc) id.doc()).getCode(), id);
		}
	}

	/**
	 * Gets the (first) active Error-Channel; or null if no Error is present.
	 * 
	 * @return the Error-Channel or null
	 */
	protected StateChannel getErrorChannel() {
		IntegerReadChannel errorCodeChannel = this.parent.channel(GridConChannelId.CCU_ERROR_CODE);
		Optional<Integer> errorCodeOpt = errorCodeChannel.value().asOptional();
		if (errorCodeOpt.isPresent() && errorCodeOpt.get() != 0) {
			int code = errorCodeOpt.get();
			System.out.println("Code read: " + code + " ==> hex: " + Integer.toHexString(code));
			code = code >> 8;
			System.out.println("Code >> 8 read: " + code + " ==> hex: " + Integer.toHexString(code));
			io.openems.edge.common.channel.ChannelId id = errorChannelIds.get(code);
			log.info("Error code is present --> " + code + " --> " + ((ErrorDoc) id.doc()).getText());
			return this.parent.channel(id);
		}
		return null;
	}

	/**
	 * Finishes the error handling. This is always the last state of the
	 * ErrorHandler.
	 * 
	 * <ul>
	 * <li>initializes the read-errors
	 * <li>sets the state to READ_ERRORS
	 * <li>sets the parent state to UNDEFINED
	 * </ul>
	 */
	private void doFinishErrorHandling() {
		this.parent.state = StateMachine.UNDEFINED;
		this.state = ErrorStateMachine.READ_ERRORS;
		this.readErrorMap = null;
	}

	protected void setState(ErrorStateMachine state) {
		this.state = state;
	}
}
