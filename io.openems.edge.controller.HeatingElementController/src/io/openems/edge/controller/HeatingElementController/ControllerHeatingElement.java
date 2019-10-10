package io.openems.edge.controller.HeatingElementController;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
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

import io.openems.common.channel.Unit;
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
	
	public Config config;

	int noRelaisSwitchedOn = 0;
	private Mode mode;
	private Priority priority;
	private double minTime;
	private int minKwh;
	private String endTime;
	LocalDate today = LocalDate.now();
	LocalTime currentEndtime;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	boolean isEndTime = true;
	
	public static final int PHASE_ONE = 1;
	public static final int PHASE_TWO = 2;
	public static final int PHASE_THREE = 3;

	boolean checkOnceFlag = true;
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
	double totalPhaseOnePower = 0;
	double totalPhaseTwoPower = 0;
	double totalPhaseThreePower = 0;
	double totalPhasePower = 0;


	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(Mode.values()) //
				.initialValue(Mode.AUTOMATIC) //
				.text("Configured Mode")), //
		PRIORITY(Doc.of(Mode.values()) //
				.initialValue(Priority.TIME) //
				.text("Configured Mode")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		NO_OF_RELAIS_ON(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)), AWAITING_HYSTERESIS(Doc.of(OpenemsType.INTEGER)),
		PHASE1_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		PHASE2_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		PHASE3_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		PHASE1_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		PHASE2_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		PHASE3_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		TOTAL_PHASE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		TOTAL_PHASE_TIME(Doc.of(OpenemsType.LONG).unit(Unit.NONE)),; //

//		CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
//				.unit(Unit.AMPERE)), //

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

		this.minTime =  this.getSeconds(config.minTime());
		
		this.minKwh = config.minkwh();
		this.endTime = config.endTime();
		currentEndtime = LocalTime.parse(this.endTime);
		
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;

	private double getSeconds(double minTime) {
		return minTime * 60 * 60; // Converting the time configured as hours into seconds
	}

	@Override
	public void run() throws OpenemsNamedException {

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
				automaticMode();
				modeChanged = changeMode(Mode.AUTOMATIC);
				break;
			}
		} while (modeChanged);

		this.channel(ChannelId.MODE).setNextValue(this.mode);
		this.channel(ChannelId.PHASE1_TIME).setNextValue(this.totalPhaseOneTime);
		this.channel(ChannelId.PHASE2_TIME).setNextValue(this.totalPhaseTwoTime);
		this.channel(ChannelId.PHASE3_TIME).setNextValue(this.totalPhaseThreeTime);
		this.channel(ChannelId.PHASE1_POWER).setNextValue(this.totalPhaseOnePower);
		this.channel(ChannelId.PHASE2_POWER).setNextValue(this.totalPhaseTwoPower);
		this.channel(ChannelId.PHASE3_POWER).setNextValue(this.totalPhaseThreePower);
		
		
		this.totalPhaseTime = this.totalPhaseOneTime + this.totalPhaseThreeTime + this.totalPhaseTwoTime;
		this.totalPhasePower = this.totalPhaseOnePower + this.totalPhaseThreePower + this.totalPhaseTwoPower;
		this.channel(ChannelId.TOTAL_PHASE_TIME).setNextValue(this.totalPhaseTime);
		this.channel(ChannelId.TOTAL_PHASE_POWER).setNextValue(this.totalPhasePower);
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

	/**
	 * Function to check change in the day and reset all the Time and power on each phases if the day changes to a new day
	 * 
	 * @return changeInDay    boolean values represent a change in day
	 */
	private boolean checkChangeInDay() {
		boolean changeInDay = false;
		LocalDate nextDay = LocalDate.now();
		if (this.today.equals(nextDay)) {
			//no change in the day
			System.out.println("no change in the day");
			return changeInDay;
		}else {
			// change in the day
			System.out.println("In change in the day");
			changeInDay = true;
			this.today = nextDay;
			return changeInDay;
		}		
	}
	protected void automaticMode() throws IllegalArgumentException, OpenemsNamedException {

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
		System.out.println("Grid active power :"+ gridActivePower);
		System.out.println("Excess power : " + excessPower);

		// resetting the variables if there is change in the day.
		if(checkChangeInDay()) {
			
			 phaseTimeOn = null;
			 phaseTimeOff = null;
			 phaseOneTimeOn = null;
			 phaseOneTimeOff = null;
			 phaseTwoTimeOn = null;
			 phaseTwoTimeOff = null;
			 phaseThreeTimeOn = null;
			 phaseThreeTimeOff = null;

			 totalPhaseOneTime = 0;
			 totalPhaseTwoTime = 0;
			 totalPhaseThreeTime = 0;
			 totalPhaseTime = 0;
			 totalPhaseOnePower = 0;
			 totalPhaseTwoPower = 0;
			 totalPhaseThreePower = 0;
			 totalPhasePower = 0;
			 
			 minTime = this.config.minTime();
			 minKwh = this.config.minkwh();
			 currentEndtime = LocalTime.parse(this.config.endTime());
		}
		
		
		// checking the end time, during checking of the endtime, state machine is stopped
		if (LocalTime.parse(formatter.format(LocalTime.now())).isAfter(currentEndtime)) {
			if (!this.isEndTime) {
			switch (this.priority) {
			case TIME:
				this.checkMinTime(excessPower);
				break;
			case KILO_WATT_HOUR:
				this.checkMinKwh(excessPower);
				break;
			}}
			
		} else {
			if (this.isEndTime) {

				boolean stateChanged;
				do {

					if (excessPower >= (this.powerOfPhase * 3)) {
						stateChanged = this.changeState(State.THIRD_PHASE);
					} else if (excessPower >= (this.powerOfPhase * 2)) {
						stateChanged = this.changeState(State.SECOND_PHASE);
					} else if (excessPower >= this.powerOfPhase) {
						stateChanged = this.changeState(State.FIRST_PHASE);
					} else {
						stateChanged = this.changeState(State.UNDEFINED);
					}

				} while (stateChanged); // execute again if the state changed

				switch (this.state) {
				case UNDEFINED:
					computeTime(false, outputChannelAddress1, PHASE_ONE, excessPower);
					computeTime(false, outputChannelAddress2, PHASE_TWO, excessPower);
					computeTime(false, outputChannelAddress3, PHASE_THREE, excessPower);
					noRelaisSwitchedOn = 0;
					break;
				case FIRST_PHASE:
					computeTime(true, outputChannelAddress1, PHASE_ONE, excessPower);
					computeTime(false, outputChannelAddress2, PHASE_TWO, excessPower);
					computeTime(false, outputChannelAddress3, PHASE_THREE, excessPower);
					noRelaisSwitchedOn = 1;
					break;
				case SECOND_PHASE:
					computeTime(true, outputChannelAddress1, PHASE_ONE, excessPower);
					computeTime(true, outputChannelAddress2, PHASE_TWO, excessPower);
					computeTime(false, outputChannelAddress3, PHASE_THREE, excessPower);
					noRelaisSwitchedOn = 2;
					break;
				case THIRD_PHASE:
					computeTime(true, outputChannelAddress1, PHASE_ONE, excessPower);
					computeTime(true, outputChannelAddress2, PHASE_TWO, excessPower);
					computeTime(true, outputChannelAddress3, PHASE_THREE, excessPower);
					noRelaisSwitchedOn = 3;
					break;
				}
				// store current state in StateMachine channel
				this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
				this.channel(ChannelId.PRIORITY).setNextValue(this.priority);
				this.channel(ChannelId.NO_OF_RELAIS_ON).setNextValue(noRelaisSwitchedOn);
			}
		}

	}
	

	/**
	 * Check the total time running of the heating element at the end of ENDTIME
	 * case 1: if the totalkiloWattHour is less than the minkwH -> force switch on
	 * the all three phases for the difference period, and update the
	 * totalTimePhase, case 2: if the totalTimePhase is equal to and more than
	 * minTime -> Do not force charge
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */

	private void checkMinKwh(long excessPower) throws IllegalArgumentException, OpenemsNamedException {

		if (this.totalPhasePower < minKwh) {
			this.isEndTime = false;
			//double deltaPower = minKwh - totalPhasePower;
			long deltaTime = (long) (minTime - this.totalPhaseTime);
			computeTime(true, outputChannelAddress1, PHASE_ONE, excessPower);
			computeTime(true, outputChannelAddress2, PHASE_TWO, excessPower);
			computeTime(true, outputChannelAddress3, PHASE_THREE, excessPower);
			noRelaisSwitchedOn = 3;
			// update the endtime
			currentEndtime = currentEndtime.plus(deltaTime, ChronoUnit.SECONDS);
			// update the minTime
			this.minTime = 0;
			// update the minKwh
			this.minKwh = 0;

		} else {
			this.isEndTime = true;
			computeTime(false, outputChannelAddress1, PHASE_ONE, excessPower);
			computeTime(false, outputChannelAddress2, PHASE_TWO, excessPower);
			computeTime(false, outputChannelAddress3, PHASE_THREE, excessPower);
		}
	}

	/**
	 * function to calculates the Kilowatthour, using the power of each phase 
	 * 
	 * @param time   Long values of time in seconds
	 * 
	 */
	private float calculatePower(long time) {		
		float kiloWattHour = ((float)(time)/3600) * this.powerOfPhase;
		return kiloWattHour;
	}

	/**
	 * Check the total time running of the heating element at the end of ENDTIME
	 * case 1: if the totalTimePhase is less than the minTime -> force switch-on the
	 * all three phases for the difference period, and update the totalTimePhase,
	 * case 2: if the totalTimePhase is equal to and more than minTime -> Do not
	 * force switch-on
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */

	private void checkMinTime(long excessPower) throws IllegalArgumentException, OpenemsNamedException {

	
			if (this.totalPhaseTime < minTime) {
				this.isEndTime = false;
				System.out.println("inside if in chkmintime, isEndtime is set to false");
				long deltaTime = (long) (minTime - this.totalPhaseTime);
				// Switch-On all the 3 Phases
				computeTime(true, outputChannelAddress1, PHASE_ONE, excessPower);
				computeTime(true, outputChannelAddress2, PHASE_TWO, excessPower);
				computeTime(true, outputChannelAddress3, PHASE_THREE, excessPower);
				noRelaisSwitchedOn = 3;
				
				// update the endtime
				currentEndtime = currentEndtime.plus(deltaTime, ChronoUnit.SECONDS);
				// update the minTime
				this.minTime = 0;
			}else
			 {
				this.isEndTime = true;
				computeTime(false, outputChannelAddress1, PHASE_ONE, excessPower);
				computeTime(false, outputChannelAddress2, PHASE_TWO, excessPower);
				computeTime(false, outputChannelAddress3, PHASE_THREE, excessPower);
				noRelaisSwitchedOn = 0;
			 }			
		
	}


	private void computeTime(boolean isSwitchOn, ChannelAddress outputChannelAddress, int phaseNumber, long excessPower)
			throws IllegalArgumentException, OpenemsNamedException {
		if (phaseNumber == 1) {
			if (!isSwitchOn) {
				// If the Phase one is not switched-On do not record the PhasetimeOff
				if (phaseOneTimeOn == null) {
					phaseOneTimeOff = null;
				} else {
					phaseOneTimeOff = LocalDateTime.now();
				}

				this.off(outputChannelAddress);
			} else {
				// phase one is running
				if (phaseOneTimeOn != null) {
					// do not take the current time
				} else {
					phaseOneTimeOn = LocalDateTime.now();
				}
				this.on(outputChannelAddress);
			}
			if (phaseOneTimeOn != null && phaseOneTimeOff != null) {
				// cycle of turning phase one On and off is complete
				totalPhaseOneTime += ChronoUnit.SECONDS.between(phaseOneTimeOn, phaseOneTimeOff);
				totalPhaseOnePower += calculatePower(this.totalPhaseOneTime);
				// Once the totalPhaseTime is calculated, reset the phasetimeOn to null to
				// calculate the time for the next cycle of switch On and Off
				phaseOneTimeOn = null;
			} else if (totalPhaseOneTime != 0) {
				// reserve the calculated totalPhaseTime
			} else {
				// phase one is not started, or still running
				totalPhaseOneTime = 0;
			}
		} else if (phaseNumber == 2) {
			if (!isSwitchOn) {

				// If the Phase two is not switched-On do not record the PhasetimeOff
				if (phaseTwoTimeOn == null) {
					phaseTwoTimeOff = null;
				} else {
					phaseTwoTimeOff = LocalDateTime.now();
				}
				this.off(outputChannelAddress);
			} else {
				// phase two is running
				if (phaseTwoTimeOn != null) {
					// do not take the current time
				} else {
					phaseTwoTimeOn = LocalDateTime.now();
				}
				this.on(outputChannelAddress);
			}
			if (phaseTwoTimeOn != null && phaseTwoTimeOff != null) {
				// cycle of turning phase two On and off is complete
				totalPhaseTwoTime += ChronoUnit.SECONDS.between(phaseTwoTimeOn, phaseTwoTimeOff);
				totalPhaseTwoPower += calculatePower(this.totalPhaseTwoTime);
				phaseTwoTimeOn = null;
			} else if (totalPhaseTwoTime != 0) {
				// reserve the calculated totalPhaseTime
			} else {
				// phase two is not started, or still running
				totalPhaseTwoTime = 0;
			}
		} else if (phaseNumber == 3) {
			if (!isSwitchOn) {

				// If the Phase three is not switched-On do not record the PhasetimeOff
				if (phaseThreeTimeOn == null) {
					phaseThreeTimeOff = null;
				} else {
					phaseThreeTimeOff = LocalDateTime.now();
				}
				this.off(outputChannelAddress);
			} else {
				// phase three is running
				if (phaseThreeTimeOn != null) {
					// do not take the current time
				} else {
					phaseThreeTimeOn = LocalDateTime.now();
				}
				this.on(outputChannelAddress);
			}
			if (phaseThreeTimeOn != null && phaseThreeTimeOff != null) {
				// cycle of turning phase three On and off is complete
				totalPhaseThreeTime += ChronoUnit.SECONDS.between(phaseThreeTimeOn, phaseThreeTimeOff);
				totalPhaseThreePower += calculatePower(this.totalPhaseThreeTime);
				phaseThreeTimeOn = null;
			} else if (totalPhaseThreeTime != 0) {
				// reserve the calculated totalPhaseTime
			} else {
				// phase two is not started, or still running
				totalPhaseThreeTime = 0;
			}
		} else {
			throw new OpenemsException("Wrong phase number");
		}

//		System.out.println("totalPhaseOneTime : " + totalPhaseOneTime);
//		System.out.println("totalPhaseOnePower : " + totalPhaseOnePower);
//		System.out.println("totalPhaseTwoTime : " + totalPhaseTwoTime);
//		System.out.println("totalPhaseTwoPower : " + totalPhaseTwoPower);
//		System.out.println("totalPhaseThreeTime : " + totalPhaseThreeTime);
//		System.out.println("totalPhaseThreePower : " + totalPhaseThreePower);

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
		System.out.println("From " + this.state + " to " + nextState);
		if (this.state != nextState) {
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				System.out.println("Not Awaiting the hysterisis : "
						+ this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock)));
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
}
