package io.openems.edge.controller.heatingelement;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;

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
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.HeatingElement", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerHeatingElement extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final Logger log = LoggerFactory.getLogger(ControllerHeatingElement.class);
	private final Clock clock;
	private final Map<Phase, PhaseDef> phases = new HashMap<>();

	private Mode mode;
	private Priority priority;
	private double minTime;
	private int minKwh;
	private int noRelaisSwitchedOn = 0;
	private ChannelAddress inputChannelAddress;
	private State state = State.UNDEFINED;
	private LocalDateTime lastStateChange = LocalDateTime.MIN;
	private LocalTime endtime;
	private double tempMinTime;
	private double tempMinKwh;

	private long totalPhaseTime = 0;
	private double totalPhasePower = 0;
	private LocalDate today = LocalDate.now();

	/**
	 * Normal mode which is before the check for minimum time or minimum kwh
	 */
	private ModeType modeType = ModeType.NORMAL_MODE;

	private Level level = Level.LEVEL_3;
	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 */
	private final TemporalAmount hysteresis = Duration.ofSeconds(5);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	public Config config;

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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
				.unit(Unit.SECONDS)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		// Setting the outputchannel for each phases
		for (Phase p : phases.keySet()) {
			if (p == Phase.ONE) {
				phases.get(p).setOutputChannelAddress(ChannelAddress.fromString(config.outputChannelAddress1()));
			} else if (p == Phase.TWO) {
				phases.get(p).setOutputChannelAddress(ChannelAddress.fromString(config.outputChannelAddress2()));
			} else {
				phases.get(p).setOutputChannelAddress(ChannelAddress.fromString(config.outputChannelAddress3()));
			}
		}

		this.mode = config.mode();
		this.priority = config.priority();
		this.level = config.level();

		this.minTime = this.getSeconds(config.minTime());
		this.minKwh = config.minkwh();

		this.endtime = LocalTime.parse(config.endTime());

		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		boolean modeChanged;

		do {
			modeChanged = false;
			switch (this.mode) {
			case AUTOMATIC:
				this.automaticMode();
				modeChanged = changeMode(Mode.AUTOMATIC);
				break;
			case MANUAL_OFF:
				for (PhaseDef p : phases.values()) {
					p.setSwitchOn(false);
				}
				this.computeTime(phases);
				noRelaisSwitchedOn = 3;
				modeChanged = changeMode(Mode.MANUAL_OFF);
				break;
			case MANUAL_ON:
				for (PhaseDef p : phases.values()) {
					p.setSwitchOn(true);
				}
				this.computeTime(phases);
				noRelaisSwitchedOn = 3;
				modeChanged = changeMode(Mode.MANUAL_ON);
				break;
			}
		} while (modeChanged);

		for (Phase p : phases.keySet()) {
			if (p == Phase.ONE) {
				this.channel(ChannelId.PHASE1_TIME).setNextValue(phases.get(p).getTotalPhaseTime());
				this.channel(ChannelId.PHASE1_POWER).setNextValue(phases.get(p).getTotalPhasePower());
			} else if (p == Phase.TWO) {
				this.channel(ChannelId.PHASE2_TIME).setNextValue(phases.get(p).getTotalPhaseTime());
				this.channel(ChannelId.PHASE2_POWER).setNextValue(phases.get(p).getTotalPhasePower());
			} else {
				this.channel(ChannelId.PHASE3_TIME).setNextValue(phases.get(p).getTotalPhaseTime());
				this.channel(ChannelId.PHASE3_POWER).setNextValue(phases.get(p).getTotalPhasePower());
			}
		}

		this.totalPhaseTime = 0;
		this.totalPhasePower = 0;
		for (PhaseDef p : phases.values()) {
			this.totalPhaseTime += p.getTotalPhaseTime();
			this.totalPhasePower += p.getTotalPhasePower();
		}
		this.channel(ChannelId.TOTAL_PHASE_TIME).setNextValue(this.totalPhaseTime);
		// Keep updating the mintime comapring it witht the total phase time
		this.tempMinTime = this.minTime - this.totalPhaseTime;

		this.channel(ChannelId.TOTAL_PHASE_POWER).setNextValue(this.totalPhasePower);
		// keep updating the minKwh comparting uit with the total phase power
		this.tempMinKwh = this.minKwh - this.totalPhasePower;

		int i1 = (int) this.channel(ChannelId.PHASE1_TIME).getNextValue().getOrError();
		int i2 = (int) this.channel(ChannelId.PHASE2_TIME).getNextValue().getOrError();
		int i3 = (int) this.channel(ChannelId.PHASE3_TIME).getNextValue().getOrError();
		this.logInfo(log, "PHASE1_TIME : " + String.valueOf(i1) + " Milisecs");
		this.logInfo(log, "PHASE2_TIME : " + String.valueOf(i2) + " Milisecs");
		this.logInfo(log, "PHASE3_TIME : " + String.valueOf(i3) + " Milisecs");

		double e1 = (double) this.channel(ChannelId.PHASE1_POWER).getNextValue().getOrError();
		double e2 = (double) this.channel(ChannelId.PHASE2_POWER).getNextValue().getOrError();
		double e3 = (double) this.channel(ChannelId.PHASE3_POWER).getNextValue().getOrError();
		this.logInfo(log, "PHASE1_Power : " + String.valueOf(e1) + " Watthours");
		this.logInfo(log, "PHASE2_Power : " + String.valueOf(e2) + " Watthours");
		this.logInfo(log, "PHASE3_Power : " + String.valueOf(e3) + " Watthours");
	}

//	/**
//	 * This method calculates the continuously calculate the minimum time the
//	 * controller shud activate the priority modes
//	 * 
//	 * @param endTime
//	 * @param totalPhaseTime
//	 * @return
//	 */
//	private LocalTime calculateEndTime(LocalTime endTime, long totalPhaseTime) {
//		return endtime.minusSeconds((long) this.tempMinTime);
//	}

	/**
	 * 
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */

	private void runMinimumPriority() throws IllegalArgumentException, OpenemsNamedException {
		switch (this.level) {
		case LEVEL_0:
			for (PhaseDef p : phases.values()) {
				p.setSwitchOn(false);
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 0;
			break;
		case LEVEL_1:
			for (Phase p : phases.keySet()) {
				if (p == Phase.ONE) {
					phases.get(p).setSwitchOn(true);
				} else {
					phases.get(p).setSwitchOn(false);
				}
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 1;
			break;
		case LEVEL_2:
			for (Phase p : phases.keySet()) {
				if (p == Phase.THREE) {
					phases.get(p).setSwitchOn(false);
				} else {
					phases.get(p).setSwitchOn(true);
				}
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 2;
			break;
		case LEVEL_3:
			for (PhaseDef p : phases.values()) {
				p.setSwitchOn(true);
			}
			this.computeTime(phases);
			noRelaisSwitchedOn = 3;
			break;
		}

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
		int gridActivePower = TypeUtils.getAsType(OpenemsType.INTEGER, (inputChannel).value().getOrError());
		this.logInfo(this.log, "gridActivePower : " + gridActivePower);
		long excessPower = 0;

		// Calculate the Excess power
		if (gridActivePower > 0) {
			excessPower = 0;
		} else {
			excessPower = Math.abs(gridActivePower);
			excessPower += this.noRelaisSwitchedOn * config.powerOfPhase();
			this.logInfo(this.log, "excess power is : " + excessPower);
		}
		this.logInfo(this.log, "No of relais : " + this.noRelaisSwitchedOn);

		// resetting the variables if there is change in the day.
		if (checkChangeInDay()) {

			for (PhaseDef p : phases.values()) {
				p.setTotalPhasePower(0);
				p.setTotalPhaseTime(0);
				p.getTimeStopwatch().reset();

			}

			this.minTime = this.config.minTime();
			this.minKwh = this.config.minkwh();
			this.endtime = LocalTime.parse(this.config.endTime());
			return;
		}

		LocalTime now = LocalTime.parse(formatter.format(LocalTime.now()));
		LocalTime updateEndtime = endtime.minusSeconds((long) this.tempMinTime);

		System.out.println("updated min kwh is : " + this.tempMinKwh);

		if (now.isAfter(updateEndtime) && now.isBefore(this.endtime)) {
			this.modeType = ModeType.PRIORITY_MODE;
		} else if (updateEndtime.isAfter(this.endtime)) {
			this.modeType = ModeType.NORMAL_MODE;
		} else {
			this.modeType = ModeType.NORMAL_MODE;
		}

		switch (this.modeType) {
		case PRIORITY_MODE:
			switch (this.priority) {
			case TIME:
				runMinimumPriority();
				System.out.println("should call the checkmintime method");
				break;
			case KILO_WATT_HOUR:
				runMinimumPriority();
				System.out.println("should call the checkminKwh method");
				break;
			}
			break;
		case NORMAL_MODE:

			boolean stateChanged;
			do {

				if (excessPower >= (config.powerOfPhase() * 3)) {
					stateChanged = this.changeState(State.THIRD_PHASE);
				} else if (excessPower >= (config.powerOfPhase() * 2)) {
					stateChanged = this.changeState(State.SECOND_PHASE);
				} else if (excessPower >= config.powerOfPhase()) {
					stateChanged = this.changeState(State.FIRST_PHASE);
				} else {
					stateChanged = this.changeState(State.UNDEFINED);
				}

			} while (stateChanged); // execute again if the state changed

			switch (this.state) {
			case UNDEFINED:
				for (PhaseDef p : phases.values()) {
					p.setSwitchOn(false);
				}
				this.computeTime(phases);
				noRelaisSwitchedOn = 0;
				break;
			case FIRST_PHASE:
				for (Phase p : phases.keySet()) {
					if (p == Phase.ONE) {
						phases.get(p).setSwitchOn(true);
					} else {
						phases.get(p).setSwitchOn(false);
					}
				}
				this.computeTime(phases);
				noRelaisSwitchedOn = 1;
				break;
			case SECOND_PHASE:
				for (Phase p : phases.keySet()) {
					if (p == Phase.THREE) {
						phases.get(p).setSwitchOn(false);
					} else {
						phases.get(p).setSwitchOn(true);
					}
				}
				this.computeTime(phases);
				noRelaisSwitchedOn = 2;
				break;
			case THIRD_PHASE:
				for (PhaseDef p : phases.values()) {
					p.setSwitchOn(true);
				}
				this.computeTime(phases);
				noRelaisSwitchedOn = 3;
				break;
			}
			// store current state in StateMachine channel
			this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
			this.channel(ChannelId.NO_OF_RELAIS_ON).setNextValue(noRelaisSwitchedOn);

			break;
		}

	}

	/**
	 * This method calls the computeTime method on individual phaseDef objects, and
	 * computes the total time the Phase was switched on
	 * 
	 * @param phases
	 * @throws IllegalArgumentException
	 * @throws OpenemsNamedException
	 */
	private void computeTime(Map<Phase, PhaseDef> phases) throws IllegalArgumentException, OpenemsNamedException {
		for (PhaseDef phaseDef : this.phases.values()) {
			phaseDef.computeTime();
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
	 * This method return the minTime in seconds
	 * 
	 * @param minTime is a double, which is configured as hours in configuration
	 * @return return the no of minutes.
	 */
	private double getSeconds(double minTime) {
		return minTime * 60 * 60; // Converting the time configured as hours into seconds
	}

}