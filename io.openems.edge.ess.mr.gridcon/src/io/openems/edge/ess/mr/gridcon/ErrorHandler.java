package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId0;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCodeChannelId1;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.writeutils.CommandControlRegisters;

public class ErrorHandler {

	private final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
	private final StateMachine parent;
	private final Map<Integer, io.openems.edge.common.channel.ChannelId> errorChannelIds;

	private State state = State.UNDEFINED;

	// READ_ERRORS
	private static final int DELAY_TO_WAIT_FOR_NEW_ERROR_SECONDS = 30;
	private Map<ChannelId, LocalDateTime> readErrorMap = new HashMap<>();

	// HANDLE_ERRORS
	private static final int MAX_TIMES_FOR_TRYING_TO_ACKNOWLEDGE_ERRORS = 5;
	private int tryToAcknowledgeErrorsCounter = 0;

	protected LocalDateTime timeWhenErrorsHasBeenAcknowledged = null;

	// HARD_RESET
	private LocalDateTime lastHardReset = null;
	private static final int SWITCH_OFF_TIME_SECONDS = 10;
	private static final long RELOAD_TIME_SECONDS = 60; // Time for Mr Gridcon to boot and come back
	private static final int MAX_TIMES_FOR_TRYING_TO_HARD_RESET = 5;
	private static final long DELAY_AFTER_FINISHING_SECONDS = 5;
	private int hardResetCounter = 0;
	private boolean sendSecondAcknowledge = false;
	private LocalDateTime delayAfterFinishing;

	public ErrorHandler(StateMachine parent) {
		this.parent = parent;
		this.errorChannelIds = this.fillErrorChannelMap();
	}

	public void initialize() {
		this.state = State.UNDEFINED;
		this.tryToAcknowledgeErrorsCounter = 0;
		this.readErrorMap = null;
		this.delayAfterFinishing = null;
	}
	
	protected StateMachine.State run() throws IllegalArgumentException, OpenemsNamedException {
		switch (this.state) {
		case UNDEFINED:
			if (parent.getCcuState() == CCUState.ERROR) {
				this.state =  State.READ_ERRORS;
			} else if (this.parent.parent.isLinkVoltageTooLow()) {
				this.state = State.LINK_VOLTAGE_TOO_LOW;
			} else {
				this.state =  State.READ_ERRORS;
			}
			break;

		case LINK_VOLTAGE_TOO_LOW:
			this.state = this.doLinkVoltageTooLow();
			break;
			
		case READ_ERRORS:
			this.state = this.doReadErrors();
			break;

		case HANDLE_ERRORS:
			this.state = this.doHandleErrors();
			break;

		case ACKNOWLEDGE_ERRORS:
			this.state = this.doAcknowledgeErrors();
			break;

		case HARD_RESET:
			this.state = this.doHardReset();
			break;

		case ERROR_HANDLING_NOT_POSSIBLE:
			this.state = this.doErrorHandlingNotPossible();
			break;

		case FINISH_ERROR_HANDLING:
			//
			if (this.delayAfterFinishing == null) {
				this.delayAfterFinishing = LocalDateTime.now();
			}
			
			if (this.delayAfterFinishing.plusSeconds(DELAY_AFTER_FINISHING_SECONDS).isAfter(LocalDateTime.now())) {
				// do nothing
				//this.state = State.FINISH_ERROR_HANDLING;
			} else {
				this.initialize();
				return StateMachine.State.UNDEFINED;
			}
		}

		return StateMachine.State.ERROR;
	}

	private State doLinkVoltageTooLow() throws IllegalArgumentException, OpenemsNamedException {
		
		new CommandControlRegisters() //
		// Stop the system
		.stop(true) // 
		.syncApproval(true) //
		.blackstartApproval(false) //
		.shortCircuitHandling(true) //
		.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
		.parameterSet1(true) //
		.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
		.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
		.enableIpus(this.parent.parent.config.inverterCount()) //
		.writeToChannels(this.parent.parent);
		
		return State.FINISH_ERROR_HANDLING;
	}

	/**
	 * Reads all active Errors into 'readErrorMap'.
	 * 
	 * @return the next state
	 */
	private State doReadErrors() {
		if (this.readErrorMap == null) {
			this.readErrorMap = new HashMap<>();
		}

		ChannelId errorId = this.readCurrentError();
		if (errorId != null) {
			if (!this.readErrorMap.containsKey(errorId)) {
				this.readErrorMap.put(errorId, LocalDateTime.now());
			}
		}

		// Did errors appear within the last DELAY_TO_WAIT_FOR_NEW_ERROR_SECONDS?
		if (this.isNewErrorPresent()) {
			// yes -> keep reading errors
			return State.READ_ERRORS;
		} else {
			// no -> start handling errors
			return State.HANDLE_ERRORS;
		}
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
	 * 
	 * @return the next state
	 */
	private State doHandleErrors() {
		if (this.isAcknowledgeable()
				&& this.tryToAcknowledgeErrorsCounter < MAX_TIMES_FOR_TRYING_TO_ACKNOWLEDGE_ERRORS) {
			// All errors are acknowledgeable and we did not try too often to acknowledge
			// them -> switch to ACKNOWLEDGE_ERRORS state
			return State.ACKNOWLEDGE_ERRORS;

		} else if (this.hardResetCounter < MAX_TIMES_FOR_TRYING_TO_HARD_RESET) {
			// At least one error is not acknowledgeable -> hard reset
			this.tryToAcknowledgeErrorsCounter = 0; // reset acknowledge-counter
			return State.HARD_RESET;

		} else {
			return State.ERROR_HANDLING_NOT_POSSIBLE;
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
	private State doAcknowledgeErrors() throws IllegalArgumentException, OpenemsNamedException {
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
				.blackstartApproval(false) //
				.errorCodeFeedback(currentErrorCodeFeedBack) //
				.shortCircuitHandling(true) //
				.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
				.parameterSet1(true) //
				.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
				.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
				.enableIpus(this.parent.parent.config.inverterCount()) //
				.writeToChannels(this.parent.parent);

		if (this.readErrorMap.isEmpty()) {
			
			if (!this.sendSecondAcknowledge ) {
				new CommandControlRegisters() //
				// Set acknowledge error bit to false and then to true again, because gridcon acts on rising edge of signal
				.acknowledge(false) //
				.syncApproval(true) //
				.blackstartApproval(false) //
				.errorCodeFeedback(currentErrorCodeFeedBack) //
				.shortCircuitHandling(true) //
				.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
				.parameterSet1(true) //
				.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
				.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
				.enableIpus(this.parent.parent.config.inverterCount()) //
				.writeToChannels(this.parent.parent);
				
				this.sendSecondAcknowledge = true;
				
			} else {
			
				new CommandControlRegisters() //
				// Acknowledge error for a 2nd time (in tests gridcon needs sometime two times)
				.acknowledge(true) //
				.syncApproval(true) //
				.blackstartApproval(false) //
				.errorCodeFeedback(currentErrorCodeFeedBack) //
				.shortCircuitHandling(true) //
				.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
				.parameterSet1(true) //
				.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
				.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
				.enableIpus(this.parent.parent.config.inverterCount()) //
				.writeToChannels(this.parent.parent);
				
				this.sendSecondAcknowledge = false;
				this.timeWhenErrorsHasBeenAcknowledged = LocalDateTime.now();
				return State.FINISH_ERROR_HANDLING;
			}
		}
		return State.ACKNOWLEDGE_ERRORS;
	}

	/**
	 * Execute a Hard-Reset, i.e. switch the Gridcon PCS off and on.
	 * 
	 * @return the next state
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	private State doHardReset() throws IllegalArgumentException, OpenemsNamedException {
		if (this.lastHardReset == null) {
			// Start Hard-Reset -> close the contactor
			this.hardResetCounter = this.hardResetCounter + 1;
			this.lastHardReset = LocalDateTime.now();
			this.parent.parent.setHardResetContactor(true);
			return State.HARD_RESET;
		}

		if (this.lastHardReset.plusSeconds(SWITCH_OFF_TIME_SECONDS).isAfter(LocalDateTime.now())) {
			// just wait and keep the contactor closed
			this.parent.parent.setHardResetContactor(true);
			return State.HARD_RESET;
		}

		if (this.lastHardReset.plusSeconds(SWITCH_OFF_TIME_SECONDS + RELOAD_TIME_SECONDS)
				.isAfter(LocalDateTime.now())) {
			// switch-off-time passed -> Open the contactor
			this.parent.parent.setHardResetContactor(false);
			return State.HARD_RESET;
		}

		// switch-off-time and reload-time passed
		this.parent.parent.setHardResetContactor(false); // Keep contactor open
		// Mr Gridcon should be back, so reset everything to start conditions
		this.lastHardReset = null;
		return State.FINISH_ERROR_HANDLING;
	}

	/**
	 * Impossible do manually handle this error. Wait for human help.
	 * 
	 * @return
	 */
	private State doErrorHandlingNotPossible() {
		// TODO switch off system
		this.parent.parent.channel(GridConChannelId.STATE_CYCLE_ERROR).setNextValue(true);
		return State.ERROR_HANDLING_NOT_POSSIBLE;
	}

	private io.openems.edge.common.channel.ChannelId readCurrentError() {
		StateChannel errorChannel = this.getErrorChannel();
		if (errorChannel != null) {
			return errorChannel.channelId();
		}
		return null;
	}

	/**
	 * Gets the (first) active Error-Channel; or null if no Error is present.
	 * 
	 * @return the Error-Channel or null
	 */
	protected StateChannel getErrorChannel() {
		IntegerReadChannel errorCodeChannel = this.parent.parent.channel(GridConChannelId.CCU_ERROR_CODE);
		Optional<Integer> errorCodeOpt = errorCodeChannel.value().asOptional();
		if (errorCodeOpt.isPresent() && errorCodeOpt.get() != 0) {
			int code = errorCodeOpt.get();
			code = code >> 8;
			ChannelId id = this.errorChannelIds.get(code);
			this.log.info("Error code is present --> " + code + " --> " + ((ErrorDoc) id.doc()).getText());
			return this.parent.parent.channel(id);
		}
		return null;
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
	 * Are all errors acknowledgeable, i.e. none of them requires a Hard-Reset.
	 * 
	 * @return true if all errors are acknowledgeable; false otherwise
	 */
	private boolean isAcknowledgeable() {
		for (ChannelId id : readErrorMap.keySet()) {
			for (ChannelId id2 : this.errorChannelIds.values()) {
				if (id.equals(id2)) {
					if (id instanceof ErrorCodeChannelId0) {
						if (((ErrorDoc) ((ErrorCodeChannelId0) id).doc()).isNeedsHardReset()) {
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

	private Map<Integer, ChannelId> fillErrorChannelMap() {
		Map<Integer, ChannelId> result = new HashMap<>();
		for (ChannelId id : ErrorCodeChannelId0.values()) {
			result.put(((ErrorDoc) id.doc()).getCode(), id);
		}
		for (ChannelId id : ErrorCodeChannelId1.values()) {
			result.put(((ErrorDoc) id.doc()).getCode(), id);
		}
		return result;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		READ_ERRORS(1, "Read Errors"), //
		ACKNOWLEDGE_ERRORS(2, "Acknowledge Errors"), //
		HANDLE_ERRORS(3, "Handle Errors"), //
		HARD_RESET(4, "Hard Reset"), //
		FINISH_ERROR_HANDLING(5, "Finish Error Handling"), //
		ERROR_HANDLING_NOT_POSSIBLE(6, "Error handling not possible"),
		LINK_VOLTAGE_TOO_LOW(7, "Link voltage too low");

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
