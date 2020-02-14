package io.openems.edge.controller.heatingelement;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
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
	boolean isEndTime = false;
	boolean isNoEndTime = true;

	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 */
	private final TemporalAmount hysteresis = Duration.ofSeconds(5);
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress1;
	private ChannelAddress outputChannelAddress2;
	private ChannelAddress outputChannelAddress3;
	protected int powerOfPhase = 0;

	long totalPhaseTime = 0;
	double totalPhasePower = 0;

	private static enum Phase {
		ONE, TWO, THREE
	}

	private final Map<Phase, PhaseDef> phases = new HashMap<>();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(Mode.values()) //
				.initialValue(Mode.AUTOMATIC) //
				.text("Configured Mode")), //
		PRIORITY(Doc.of(Priority.values()) //
				.initialValue(Priority.TIME) //
				.text("Configured Mode")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		NO_OF_RELAIS_ON(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		AWAITING_HYSTERESIS(Doc.of(OpenemsType.INTEGER)), //
		PHASE1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE1_POWER(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		PHASE2_POWER(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		PHASE3_POWER(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		TOTAL_PHASE_POWER(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		TOTAL_PHASE_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.SECONDS)),; //

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
		for (Phase phase : Phase.values()) {
			this.phases.put(phase, new PhaseDef(this));
		}
	}

	public ControllerHeatingElement() {
		this(Clock.systemDefaultZone());
	}

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

		for (Phase p : phases.keySet()) {
			if (p == Phase.ONE) {
				this.channel(ChannelId.PHASE1_TIME).setNextValue(phases.get(p).totalPhaseTime);
				this.channel(ChannelId.PHASE1_POWER).setNextValue(phases.get(p).totalPhasePower);
			} else if (p == Phase.TWO) {
				this.channel(ChannelId.PHASE2_TIME).setNextValue(phases.get(p).totalPhaseTime);
				this.channel(ChannelId.PHASE2_POWER).setNextValue(phases.get(p).totalPhasePower);
			} else {
				this.channel(ChannelId.PHASE3_TIME).setNextValue(phases.get(p).totalPhaseTime);
				this.channel(ChannelId.PHASE3_POWER).setNextValue(phases.get(p).totalPhasePower);
			}
		}

		for (PhaseDef p : phases.values()) {
			this.totalPhaseTime += p.totalPhaseTime;
			this.totalPhasePower += p.totalPhasePower;
		}
		this.channel(ChannelId.TOTAL_PHASE_TIME).setNextValue(this.totalPhaseTime);
		this.channel(ChannelId.TOTAL_PHASE_POWER).setNextValue(this.totalPhasePower);
		
		int i1 = (int) this.channel(ChannelId.PHASE1_TIME).getNextValue().getOrError();
		int i2 = (int) this.channel(ChannelId.PHASE2_TIME).getNextValue().getOrError();
		int i3 = (int) this.channel(ChannelId.PHASE3_TIME).getNextValue().getOrError();
		this.logInfo(log, "PHASE1_TIME : " + String.valueOf(i1) + " Milisecs");
		this.logInfo(log, "PHASE2_TIME : " + String.valueOf(i2) + " Milisecs");
		this.logInfo(log, "PHASE3_TIME : " + String.valueOf(i3) + " Milisecs");
		
		double e1 = (double) this.channel(ChannelId.PHASE1_POWER).getNextValue().getOrError();
		double e2 = (double) this.channel(ChannelId.PHASE2_POWER).getNextValue().getOrError();
		double e3 = (double) this.channel(ChannelId.PHASE3_POWER).getNextValue().getOrError();
		this.logInfo(log, "PHASE1_Power : " + String.valueOf(e1)+ " Watthours");
		this.logInfo(log, "PHASE2_Power : " + String.valueOf(e2)+ " Watthours");
		this.logInfo(log, "PHASE3_Power : " + String.valueOf(e3)+ " Watthours");
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
	 * Function to check change in the day 
	 * 
	 * @return changeInDay boolean values represent a change in day
	 */
	private boolean checkChangeInDay() {
		boolean changeInDay = false;
		LocalDate nextDay = LocalDate.now();
		if (this.today.equals(nextDay)) {
			return changeInDay;
		} else {
			changeInDay = true;
			this.today = nextDay;
			return changeInDay;
		}
	}

	protected void automaticMode() throws IllegalArgumentException, OpenemsNamedException {

		// Get the input channel addresses
		Channel<?> inputChannel = this.componentManager.getChannel(this.inputChannelAddress);
		int gridActivePower = TypeUtils.getAsType(OpenemsType.INTEGER, inputChannel.value().getOrError());
		this.logInfo(this.log, "gridActivePower : "+ gridActivePower);
		long excessPower = 0;

		// Calculate the Excess power
		if (gridActivePower > 0) {
			excessPower = 0;
		} else {
			excessPower = Math.abs(gridActivePower);
			excessPower += noRelaisSwitchedOn * powerOfPhase;
			this.logInfo(this.log, "excess power is 5 : "+ excessPower);
		}
		this.logInfo(this.log, "No of relais : "+ noRelaisSwitchedOn);

		// resetting the variables if there is change in the day.
		if (checkChangeInDay()) {
			
			for (PhaseDef p : phases.values()) {
				p.phaseTimeOn = null;
				p.phaseTimeOff = null;
				p.totalPhasePower = 0;
				p.totalPhaseTime = 0;				
				p.timeStopwatch.reset();
				
			}

			minTime = this.config.minTime();
			minKwh = this.config.minkwh();
			currentEndtime = LocalTime.parse(this.config.endTime());
			return;
		}

		// Setting the outputchannel for each phases
		for (Phase p : phases.keySet()) {
			if (p == Phase.ONE) {
				phases.get(p).outputChannelAddress = this.outputChannelAddress1;
			} else if (p == Phase.TWO) {
				phases.get(p).outputChannelAddress = this.outputChannelAddress2;
			} else {
				phases.get(p).outputChannelAddress = this.outputChannelAddress3;
			}
		}

		// boolean isEndTimeCheck = false;
//		System.out.println("Local time : "+ LocalTime.parse(formatter.format(LocalTime.now())));
//		System.out.println("Current End time : " + currentEndtime);
		if (LocalTime.parse(formatter.format(LocalTime.now())).isAfter(currentEndtime.minusSeconds((long) this.minTime)) && !this.isEndTime) {
			switch (this.priority) {
			case TIME:
				this.checkMinTime(excessPower);
				break;
			case KILO_WATT_HOUR:
				this.checkMinKwh(excessPower);
				break;
			}
		} else {
			if (this.isNoEndTime) {
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
					for (PhaseDef p : phases.values()) {
						p.isSwitchOn = false;
					}
					this.computeTime(phases);
					noRelaisSwitchedOn = 0;
					break;
				case FIRST_PHASE:
					for (Phase p : phases.keySet()) {
						if (p == Phase.ONE) {
							phases.get(p).isSwitchOn = true;
						} else {
							phases.get(p).isSwitchOn = false;
						}
					}
					this.computeTime(phases);
					noRelaisSwitchedOn = 1;
					break;
				case SECOND_PHASE:
					for (Phase p : phases.keySet()) {
						if (p == Phase.THREE) {
							phases.get(p).isSwitchOn = false;
						} else {
							phases.get(p).isSwitchOn = true;
						}
					}
					this.computeTime(phases);
					noRelaisSwitchedOn = 2;
					break;
				case THIRD_PHASE:
					for (PhaseDef p : phases.values()) {
						p.isSwitchOn = true;
					}
					this.computeTime(phases);
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
	 * case 1: if the totalTimePhase is less than the minTime -> force switch-on the
	 * all three phases for the difference period, and update the totalTimePhase,
	 * case 2: if the totalTimePhase is equal to and more than minTime -> Do not
	 * force switch-on
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */

	private void checkMinTime(long excessPower) throws IllegalArgumentException, OpenemsNamedException {
		System.out.println("totalPhaseTime : " + this.totalPhaseTime);
		System.out.println("mintime : " + minTime);
		if (this.totalPhaseTime < minTime) {
			this.isNoEndTime = false;
			//long deltaTime = (long) (minTime - this.totalPhaseTime);
			// Switch-On all the 3 Phases
			for (PhaseDef p : phases.values()) {
				p.isSwitchOn = true;
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 3;
			// update the endtime
			//currentEndtime = currentEndtime.plus(deltaTime, ChronoUnit.MINUTES);
			// update the minTime
			this.minTime = 0;
		} else {
			this.isEndTime = true;
			this.isNoEndTime = true;
			for (PhaseDef p : phases.values()) {
				p.isSwitchOn = false;
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 0;
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
			this.isNoEndTime = false;
			// double deltaPower = minKwh - totalPhasePower;
			long deltaTime = (long) (minTime - this.totalPhaseTime);

			for (PhaseDef p : phases.values()) {
				p.isSwitchOn = true;
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 3;
			// update the endtime
			currentEndtime = currentEndtime.plus(deltaTime, ChronoUnit.MINUTES);
			// update the minTime
			this.minTime = 0;
			// update the minKwh
			this.minKwh = 0;
		} else {
			this.isEndTime = true;
			this.isNoEndTime = true;
			for (PhaseDef p : phases.values()) {
				p.isSwitchOn = false;
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 0;
		}
	}

	private void computeTime(Map<Phase, PhaseDef> phases) throws IllegalArgumentException, OpenemsNamedException {
		for (PhaseDef phaseDef : this.phases.values()) {
			phaseDef.computeTime();
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
		// System.out.println("From " + this.state + " to " + nextState);
		if (this.state != nextState) {
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				// System.out.println("Not Awaiting the hysterisis : "
				// +
				// this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock)));
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
