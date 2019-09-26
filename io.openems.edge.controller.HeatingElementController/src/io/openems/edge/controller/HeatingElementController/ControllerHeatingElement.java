package io.openems.edge.controller.HeatingElementController;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.HeatingElement", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerHeatingElement extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerHeatingElement.class);

	private final Clock clock;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	int noRelaisSwitchedOn = 0;
	private Mode mode;
	private Priority priority;
	private int minTime;
	private int minKwh;
	private String endTime;
	LocalTime currentEndtime;	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static final int PHASE_ONE = 1;
	public static final int PHASE_TWO = 2;
	public static final int PHASE_THREE = 3;
	
	

	
	LocalDateTime phaseTimeOn = null;
	LocalDateTime phaseTimeOff = null;
	LocalDateTime phaseOneTimeOn = null;
	LocalDateTime phaseOneTimeOff = null;
	LocalDateTime phaseTwoTimeOn = null;
	LocalDateTime phaseTwoTimeOff = null;
	LocalDateTime phaseThreeTimeOn = null;
	LocalDateTime phaseThreeTimeOff = null;

	long totalPhaseOneTime = 0;
	long totalPhaseTwoTime = 0;
	long totalPhaseThreeTime = 0;
	long totalPhaseTime = 0;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(Mode.values()) //
				.initialValue(Mode.AUTOMATIC) //
				.text("Configured Mode")), //
		PRIORITY(Doc.of(Mode.values()) //
				.initialValue(Priority.TIME) //
				.text("Configured Mode")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		AWAITING_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hystesis is active")),
		PHASE1_TIME(Doc.of(Level.INFO)
				.text("Time on Phase one")),
		PHASE2_TIME(Doc.of(Level.INFO)
				.text("Time on Phase two")),
		PHASE3_TIME(Doc.of(Level.INFO)
				.text("Time on Phase three")),
		PHASE1_POWER(Doc.of(Level.INFO)
				.text("Power on Phase one")),
		PHASE2_POWER(Doc.of(Level.INFO)
				.text("Power on Phase two")),
		PHASE3_POWER(Doc.of(Level.INFO)
				.text("Power on Phase three")),
		TOTAL_PHASE_POWER(Doc.of(Level.INFO)
				.text("Total Power on all three Phases")),
		TOTAL_PHASE_TIME(Doc.of(Level.INFO)
				.text("Total time on all three Phases")),; //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public ControllerHeatingElement(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	public ControllerHeatingElement() {
		this(Clock.systemDefaultZone());
	}

	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 */
	private final TemporalAmount hysteresis = Duration.ofSeconds(2);
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress1;
	private ChannelAddress outputChannelAddress2;
	private ChannelAddress outputChannelAddress3;
	private int powerOfPhase = 0;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.powerOfPhase = config.powerOfPhase();
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		this.outputChannelAddress1 = ChannelAddress.fromString(config.outputChannelAddress1());
		this.outputChannelAddress2 = ChannelAddress.fromString(config.outputChannelAddress2());
		this.outputChannelAddress3 = ChannelAddress.fromString(config.outputChannelAddress3());

		this.mode = config.mode();
		this.channel(ChannelId.MODE).setNextValue(config.mode());
		this.priority = config.priority();
		this.channel(ChannelId.PRIORITY).setNextValue(config.priority());

		this.minTime = this.getSeconds(config.minTime());
		this.minKwh = config.minkwh();
		this.endTime = config.endTime();

		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;

	private int getSeconds(int minTime) {
		return minTime * 60 * 60; // Converting the time configured as hours into seconds
	}

	@Override
	public void run() throws OpenemsNamedException {

		LocalDateTime now = LocalDateTime.now();
		Boolean modeChanged;

		do {
			modeChanged = false;
			switch (this.mode) {
			case MANUAL_ON:
				this.on(outputChannelAddress1);
				this.on(outputChannelAddress2);
				this.on(outputChannelAddress3);
				modeChanged = changeMode(Mode.MANUAL_ON);
				break;
			case MANUAL_OFF:
				this.off(outputChannelAddress1);
				this.off(outputChannelAddress2);
				this.off(outputChannelAddress3);
				modeChanged = changeMode(Mode.MANUAL_OFF);
				break;
			case AUTOMATIC:
				automaticMode(now);
				modeChanged = changeMode(Mode.AUTOMATIC);
				break;
			}
		} while (modeChanged);

		this.channel(ChannelId.MODE).setNextValue(this.mode);
		System.out.println("Total phase time : " + totalPhaseTime);
	}

	/**
	 * A flag to maintain change in the mode
	 * 
	 * @param nextmode the target mode
	 * @return Flag that the mode is changed or not
	 */
	private boolean changeMode(Mode nextMode) {
		if (this.mode != nextMode) {
			this.mode = nextMode;
			return true;
		} else
			return false;
	}

	protected void automaticMode(LocalDateTime now) throws IllegalArgumentException, OpenemsNamedException {

		currentEndtime = LocalTime.parse(this.endTime);
		// Get the input channel addresses
		Channel<?> inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
		int gridActivePower = TypeUtils.getAsType(OpenemsType.INTEGER, inputChannel.value().getOrError());
		long excessPower;

		// Calculate the Excess power
		if (gridActivePower > 0) {
			excessPower = 0;
		} else {
			excessPower = Math.abs(gridActivePower);
			excessPower += noRelaisSwitchedOn * 2000;
		}		

		/*
		 * boolean stateChanged; do {
		 * 
		 * if (excessPower >= (this.powerOfPhase * 3)) { stateChanged =
		 * this.changeState(State.THIRD_PHASE); } else if (excessPower >=
		 * (this.powerOfPhase * 2)) { stateChanged =
		 * this.changeState(State.SECOND_PHASE); } else if (excessPower >=
		 * this.powerOfPhase) { stateChanged = this.changeState(State.FIRST_PHASE); }
		 * else { stateChanged = this.changeState(State.UNDEFINED); }
		 * 
		 * } while (stateChanged); // execute again if the state changed
		 */
		boolean stateChanged = false;
		do {
			switch (this.state) {
			case UNDEFINED:
				if (excessPower >= this.powerOfPhase) {
					stateChanged = changeState(State.SWITCH_ON_FIRSTPHASE);//
				} else if (excessPower < this.powerOfPhase) {
					stateChanged = changeState(State.SWITCH_OFF_FIRSTPHASE);// changeState
				} else {
					stateChanged = changeState(State.UNDEFINED);// changeState
				}
				break;
			case SWITCH_ON_FIRSTPHASE:
				testLogic(true, outputChannelAddress1, PHASE_ONE);
				if (excessPower >= (this.powerOfPhase * 2)) {
					stateChanged = changeState(State.SWITCH_ON_SECONDPHASE);// changeState
					
					//this.on(outputChannelAddress1, PHASE_ONE); //--------------------------------------------------------------------
				} else if (excessPower < (this.powerOfPhase)) {
					stateChanged = changeState(State.SWITCH_OFF_FIRSTPHASE);// changeState
				} else {
					stateChanged = changeState(State.SWITCH_ON_FIRSTPHASE);// changeState
				}
				break;
			case SWITCH_OFF_FIRSTPHASE:
				if (excessPower >= (this.powerOfPhase)) {
					stateChanged = changeState(State.SWITCH_ON_FIRSTPHASE);// changeState
				} else {
					stateChanged = changeState(State.SWITCH_OFF_FIRSTPHASE);// changeState
					testLogic(false, outputChannelAddress1, PHASE_ONE);
					//this.off(outputChannelAddress1, PHASE_ONE);//--------------------------------------------------------------------
				}
				break;
			case SWITCH_ON_SECONDPHASE:
				testLogic(true, outputChannelAddress2, PHASE_TWO);
				if (excessPower >= (this.powerOfPhase * 3)) {
					stateChanged = changeState(State.SWITCH_ON_THIRDPHASE);// changeState
					
					//this.on(outputChannelAddress2, PHASE_TWO);//--------------------------------------------------------------------
				} else if (excessPower < (this.powerOfPhase * 2)) {
					stateChanged = changeState(State.SWITCH_OFF_THIRDPHASE);// changeState
				} else {
					stateChanged = changeState(State.SWITCH_ON_SECONDPHASE);// changeState
				}
				break;
			case SWITCH_OFF_SECONDPHASE:
				if (excessPower >= (this.powerOfPhase * 2)) {
					stateChanged = changeState(State.SWITCH_ON_SECONDPHASE);// changeState
				} else if (excessPower < (this.powerOfPhase * 2) && excessPower >= this.powerOfPhase) {
					stateChanged = changeState(State.SWITCH_ON_FIRSTPHASE);// changeState
				} else {
					stateChanged = changeState(State.SWITCH_OFF_SECONDPHASE);// changeState
					testLogic(false, outputChannelAddress2, PHASE_TWO);
					//this.off(outputChannelAddress2, PHASE_TWO);//--------------------------------------------------------------------
				}
				break;
			case SWITCH_ON_THIRDPHASE:
				testLogic(true, outputChannelAddress3, PHASE_THREE);
				if (excessPower < (this.powerOfPhase * 3)) {
					stateChanged = changeState(State.SWITCH_OFF_THIRDPHASE);// changeState
				} else {
					stateChanged = changeState(State.SWITCH_ON_THIRDPHASE);// changeState
					
					//this.on(outputChannelAddress3);//--------------------------------------------------------------------
				}
				break;
			case SWITCH_OFF_THIRDPHASE:
				if (excessPower >= (this.powerOfPhase)) {
					stateChanged = changeState(State.SWITCH_ON_THIRDPHASE);// changeState
				} else if (excessPower < (this.powerOfPhase * 3) && excessPower >= (this.powerOfPhase * 2)) {
					stateChanged = changeState(State.SWITCH_ON_SECONDPHASE);// changeState
				} else {
					stateChanged = changeState(State.SWITCH_OFF_FIRSTPHASE);// changeState
					testLogic(false, outputChannelAddress3, PHASE_THREE);
				//	this.off(outputChannelAddress3, PHASE_THREE);//--------------------------------------------------------------------
				}
				break;
			}
		} while (stateChanged);
		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
		this.channel(ChannelId.PRIORITY).setNextValue(this.priority);
	}
	

	
	private void testLogic(boolean onOFF, ChannelAddress outputChannelAddress, int phaseNumber)
			throws IllegalArgumentException, OpenemsNamedException {
		if (phaseNumber == 1) {
			if (!onOFF) {

				// If the Phase is not switched-On do not record the PhasetimeOff
				if (phaseOneTimeOn == null) {
					phaseOneTimeOff = null;
				} else {
					phaseOneTimeOff = LocalDateTime.now();
				}

				this.off(outputChannelAddress);

			} else {
				// Atleast one phase is running
				if (phaseOneTimeOn != null) {
					// do not take the current time
				} else {
					phaseOneTimeOn = LocalDateTime.now();
				}
				this.on(outputChannelAddress);

			}
			if (phaseOneTimeOn != null && phaseOneTimeOff != null) {
				// Atleast one cycle, any of the Phase was switch on and off
				System.out.println("delta times : " + ChronoUnit.SECONDS.between(phaseOneTimeOn, phaseOneTimeOff));
				totalPhaseOneTime += ChronoUnit.SECONDS.between(phaseOneTimeOn, phaseOneTimeOff);
				// Once the totalPhaseTime is calculated, reset the phasetimeOn to null to
				// calculate the time for the next cycle of switch On and Off
				phaseOneTimeOn = null;
			} else if (totalPhaseOneTime != 0) {
				// reserve the calculated totalPhaseTime
			} else {
				// no phases are running
				// or one of the phases is still running and not stopped
				totalPhaseOneTime = 0;
			}
		} else if (phaseNumber == 2) {
			if (!onOFF) {

				// If the Phase is not switched-On do not record the PhasetimeOff
				if (phaseTwoTimeOn == null) {
					phaseTwoTimeOff = null;
				} else {
					phaseTwoTimeOff = LocalDateTime.now();
				}

				this.off(outputChannelAddress);

			} else {
				// Atleast one phase is running
				if (phaseTwoTimeOn != null) {
					// do not take the current time
				} else {
					phaseTwoTimeOn = LocalDateTime.now();
				}
				this.on(outputChannelAddress);

			}
			if (phaseTwoTimeOn != null && phaseTwoTimeOff != null) {
				// Atleast one cycle, any of the Phase was switch on and off
				System.out.println("delta times : " + ChronoUnit.SECONDS.between(phaseTwoTimeOn, phaseTwoTimeOff));
				totalPhaseTwoTime += ChronoUnit.SECONDS.between(phaseTwoTimeOn, phaseTwoTimeOff);
				// Once the totalPhaseTime is calculated, reset the phasetimeOn to null to
				// calculate the time for the next cycle of switch On and Off
				phaseTwoTimeOn = null;
			} else if (totalPhaseTwoTime != 0) {
				// reserve the calculated totalPhaseTime
			} else {
				// no phases are running
				// or one of the phases is still running and not stopped
				totalPhaseTwoTime = 0;
			}
		} else if (phaseNumber == 3) {
			if (!onOFF) {

				// If the Phase is not switched-On do not record the PhasetimeOff
				if (phaseThreeTimeOn == null) {
					phaseThreeTimeOff = null;
				} else {
					phaseThreeTimeOff = LocalDateTime.now();
				}

				this.off(outputChannelAddress);

			} else {
				// Atleast one phase is running
				if (phaseThreeTimeOn != null) {
					// do not take the current time
				} else {
					phaseThreeTimeOn = LocalDateTime.now();
				}
				this.on(outputChannelAddress);

			}
			if (phaseThreeTimeOn != null && phaseThreeTimeOff != null) {
				// Atleast one cycle, any of the Phase was switch on and off
				System.out.println("delta times : " + ChronoUnit.SECONDS.between(phaseThreeTimeOn, phaseThreeTimeOff));
				totalPhaseThreeTime += ChronoUnit.SECONDS.between(phaseThreeTimeOn, phaseThreeTimeOff);
				// Once the totalPhaseTime is calculated, reset the phasetimeOn to null to
				// calculate the time for the next cycle of switch On and Off
				phaseThreeTimeOn = null;
			} else if (totalPhaseThreeTime != 0) {
				// reserve the calculated totalPhaseTime
			} else {
				// no phases are running
				// or one of the phases is still running and not stopped
				totalPhaseThreeTime = 0;
			}
		} else {
			throw new OpenemsException("Wrong phase number");
		}
		
		totalPhaseTime +=  totalPhaseOneTime + totalPhaseTwoTime + totalPhaseThreeTime ;
		
		System.out.println("totalPhaseOneTime : "+ totalPhaseOneTime);
		System.out.println("totalPhaseTwoTime : "+ totalPhaseTwoTime);
		System.out.println("totalPhaseThreeTime : "+ totalPhaseThreeTime);
		System.out.println("totalPhaseTime : "+ totalPhaseTime);
		
	}
	
	/**
	 * Helper function to switch the relays on and off, and also calculate the time
	 * how long the relays were switched on
	 * 
	 * @param noRelaisSwitchedOn no of relays turned on
	 */
	private void calculateTotalTime(int noRelaisSwitchedOn) throws IllegalArgumentException, OpenemsNamedException {

		if (noRelaisSwitchedOn == 0) {
			// If the Phase is not switched-On do not record the PhasetimeOff
			if (phaseTimeOn == null) {
				phaseTimeOff = null;
			} else {
				phaseTimeOff = LocalDateTime.now();
			}

			this.off(outputChannelAddress1);
			this.off(outputChannelAddress2);
			this.off(outputChannelAddress3);
		} else {
			// Atleast one phase is running
			if (phaseTimeOn != null) {
				// do not take the current time
			} else {
				phaseTimeOn = LocalDateTime.now();
			}
			if (noRelaisSwitchedOn == 1) {
				this.on(outputChannelAddress1);
				this.off(outputChannelAddress2);
				this.off(outputChannelAddress3);
			} else if (noRelaisSwitchedOn == 2) {
				this.on(outputChannelAddress1);
				this.on(outputChannelAddress2);
				this.off(outputChannelAddress3);
			} else if (noRelaisSwitchedOn == 3) {
				this.on(outputChannelAddress1);
				this.on(outputChannelAddress2);
				this.on(outputChannelAddress3);
			} else {
				throw new OpenemsException("Invalid number of relais");
			}
		}
		if (phaseTimeOn != null && phaseTimeOff != null) {
			// Atleast one cycle, any of the Phase was switch on and off
			System.out.println("delta times : " + ChronoUnit.SECONDS.between(phaseTimeOn, phaseTimeOff));
			totalPhaseTime += ChronoUnit.SECONDS.between(phaseTimeOn, phaseTimeOff);
			// Once the totalPhaseTime is calculated, reset the phasetimeOn to null to
			// calculate the time for the next cycle of switch On and Off
			phaseTimeOn = null;
		} else if (totalPhaseTime != 0) {
			// reserve the calculated totalPhaseTime
		} else {
			// no phases are running
			// or one of the phases is still running and not stopped
			totalPhaseTime = 0;
		}

	}
	
	/**
	 * Check the total time running of the heating element at the end of ENDTIME
	 * case 1: if the totalkiloWattHour is less than the minkwH -> force switch on the
	 * all three phases for the difference period, and update the totalTimePhase,
	 * case 2: if the totalTimePhase is equal to and more than minTime -> Do not
	 * force charge
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */

	private void checkMinKwh(long excessPower) throws IllegalArgumentException, OpenemsNamedException {

		if (formatter.format(LocalTime.now()).equals(formatter.format(currentEndtime))) {

			long totalkiloWattHour = calculatePower(this.totalPhaseTime, excessPower);
			if (totalkiloWattHour > minKwh) {
				long deltaPower = minKwh - totalkiloWattHour;
				long deltaTime = minTime - this.totalPhaseTime;
				calculateTotalTime(3);
				// update the endtime
				currentEndtime = currentEndtime.plus(deltaTime, ChronoUnit.SECONDS);
				// update the kilowatthour
				totalkiloWattHour += deltaPower;

				checkMinKwh(totalkiloWattHour);
			}
			// resetting the Total phase time, phasetimeon , Phasetimeoff to zero
			this.totalPhaseTime = 0;
			this.phaseTimeOff = null;
			this.phaseTimeOn = null;

		} else {
			// Switch-Off the phases
			calculateTotalTime(0);
		}
	}
	
	private long calculatePower(long time, long power) {
		long kiloWattHour = (time/3600) *  power;
		return  kiloWattHour;
	}

	/**
	 * Check the total time running of the heating element at the end of ENDTIME
	 * case 1: if the totalTimePhase is less than the minTime -> force switch on the
	 * all three phases for the difference period, and update the totalTimePhase,
	 * case 2: if the totalTimePhase is equal to and more than minTime -> Do not
	 * force charge
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void checkMinTime() throws IllegalArgumentException, OpenemsNamedException {

		if (formatter.format(LocalTime.now()).equals(formatter.format(currentEndtime))) {
			if (this.totalPhaseTime < minTime) {
				long deltaTime = minTime - this.totalPhaseTime;
				// Switch-On all the 3 Phases
				calculateTotalTime(3);
				// update the endtime
				currentEndtime = currentEndtime.plus(deltaTime, ChronoUnit.SECONDS);

				// update the totalPhaseTime
				this.totalPhaseTime += deltaTime;

				checkMinTime();
			}
			// resetting the Total phase time, phasetimeon , Phasetimeoff to zero
			this.totalPhaseTime = 0;
			this.phaseTimeOff = null;
			this.phaseTimeOn = null;
		} else {
			// Switch-Off the phases
			calculateTotalTime(0);
		}

	}
	
	




	/**
	 * Switch the output ON.
	 * 
	 * @param outputChannelAddress address of the channel which must set to ON
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void on(ChannelAddress outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(true, outputChannelAddress);
	}

	/**
	 * Switch the output OFF.
	 * 
	 * @param outputChannelAddress address of the channel which must set to OFF
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void off(ChannelAddress outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(false, outputChannelAddress);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value                The boolean value which must set on the output
	 *                             channel address
	 * @param outputChannelAddress The address of the channel
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void setOutput(boolean value, ChannelAddress outputChannelAddress)
			throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(outputChannelAddress);
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + (value) + ".");
				outputChannel.setNextWriteValue(value);
			}
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set output: [" + outputChannelAddress + "] " + e.getMessage());
		}
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 * 
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
				return true;
			} else {
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
				return false;
			}
		} else {
			this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			return false;
		}
	}
	
	
//	private void handleUnDefined() throws IllegalArgumentException, OpenemsNamedException {
//	noRelaisSwitchedOn = 0;
//	calculateTotalTime(noRelaisSwitchedOn);
//}

///**
// * Switch on the first phase. Calculate the time, how long the first phase was
// * turned on.
// */
//private void handleSinglePhase() throws IllegalArgumentException, OpenemsNamedException {
//	noRelaisSwitchedOn = 1;
//	calculateTotalTime(noRelaisSwitchedOn);
//}
//
///**
// * Switch on the first phase and second phase. Calculate the time, how long the
// * second phase was turned on.
// */
//private void handleSecondPhase() throws IllegalArgumentException, OpenemsNamedException {
//	noRelaisSwitchedOn = 2;
//	calculateTotalTime(noRelaisSwitchedOn);
//}
//
///**
// * Switch on the third phase, first phase and second phase. Calculate the time,
// * how long the third phase was turned on.
// */
//private void handleThirdPhase() throws IllegalArgumentException, OpenemsNamedException {
//	noRelaisSwitchedOn = 3;
//	calculateTotalTime(noRelaisSwitchedOn);
//}
}
