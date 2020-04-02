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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

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
@Component(name = "Controller.HeatingElement", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //
public class ControllerHeatingElement extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
	public static final double MILI_SEC_PER_DAY = 3600000.0;
	public static final int SEC_PER_DAY = 3600;
	public static final int TWO = 2;
	public static final int THREE = 3;

	private final Clock clock;
	private final Map<Phase, PhaseDef> phases = new HashMap<>();

	private Mode mode;
	private Priority priority;

	private int noRelaisSwitchedOn = 0;
	private ChannelAddress inputChannelAddress;
	private Level level = Level.LEVEL_0;
	private LocalDateTime lastStateChange = LocalDateTime.MIN;
	private LocalTime endtime;

	/**
	 * This variable holds the minimum time the phases should switch on
	 */
	private double minTime;
	/**
	 * This variable holds the changed mintime in every cycle, based on the total
	 * time of each phase
	 */
	private double countDownMinTime;
	/**
	 * This variable holds the minimum Kwh the phases should switch on
	 */
	private double minKwh;
	/**
	 * This variable holds the changed Kwh in every cycle, based on the total power
	 * of each phase
	 */
	private double countDownMinKwh;
	/**
	 * This variable holds the total time of level_1.
	 */
	long level1Time = 0;
	/**
	 * This variable holds the total time of level_2.
	 */
	long level2Time = 0;
	/**
	 * This variable holds the total time of level_3.
	 */
	long level3Time = 0;
	/**
	 * This variable holds the total energy of level_1.
	 */
	double level1Energy = 0;
	/**
	 * This variable holds the total energy of level_2.
	 */
	double level2Energy = 0;
	/**
	 * This variable holds the total energy of level_3.
	 */
	double level3Energy = 0;

	private long totalPhase1Time = 0;
	private long totalPhase2Time = 0;
	private long totalPhase3Time = 0;

	private double totalPhase1Energy = 0;
	private double totalPhase2Energy = 0;
	private double totalPhase3Energy = 0;

	private long totalPhaseTime = 0;
	private double totalPhaseEnergy = 0;
	private LocalDate today = LocalDate.now();

	/**
	 * This enum specify the modeType the algorithm is running.
	 */
	private ModeType modeType = ModeType.NORMAL_MODE;
	/**
	 * 
	 */
	private Level heatingLevel = Level.LEVEL_3;
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
		LEVEL(Doc.of(Level.values()) //
				.text("Current Level")),
		NO_OF_RELAIS_ON(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		AWAITING_HYSTERESIS(Doc.of(OpenemsType.INTEGER)), //
		PHASE1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		LEVEL1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		LEVEL2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		LEVEL3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		COUNT_DOWN_MIN_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		COUNT_DOWN_MIN_KWH(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT_HOURS)), //
		PHASE1_ENERGY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		PHASE2_ENERGY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		PHASE3_ENERGY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		LEVEL1_ENERGY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		LEVEL2_ENERGY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		LEVEL3_ENERGY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.WATT_HOURS)), //
		TOTAL_PHASE_ENERGY(Doc.of(OpenemsType.DOUBLE)//
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
		this.heatingLevel = config.heatingLevel();

		/**
		 * Mintime is calculated in both the priority modes and it is also important in
		 * each levels.
		 */
		this.minKwh = config.minkwh();
		this.minTime = this.getSeconds(this.heatingLevel, this.priority, config.minTime(), this.minKwh);

		this.endtime = LocalTime.parse(config.endTime());

		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Modified
	void modified(Config config) throws OpenemsNamedException {
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
		this.heatingLevel = config.heatingLevel();

		/**
		 * Mintime is calculated in both the priority modes and it is also important in
		 * each levels.
		 */
		this.minKwh = config.minkwh();
		this.minTime = this.getSeconds(this.heatingLevel, this.priority, config.minTime(), this.minKwh);

		this.endtime = LocalTime.parse(config.endTime());
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
				this.totalPhase1Time = phases.get(p).getTotalPhaseTime();
				this.totalPhase1Energy = phases.get(p).getTotalPhaseEnergy();
				this.channel(ChannelId.PHASE1_TIME).setNextValue(this.totalPhase1Time);
				this.channel(ChannelId.PHASE1_ENERGY).setNextValue(this.totalPhase1Energy);
			} else if (p == Phase.TWO) {
				this.totalPhase2Time = phases.get(p).getTotalPhaseTime();
				this.totalPhase2Energy = phases.get(p).getTotalPhaseEnergy();
				this.channel(ChannelId.PHASE2_TIME).setNextValue(this.totalPhase2Time);
				this.channel(ChannelId.PHASE2_ENERGY).setNextValue(this.totalPhase2Energy);
			} else {
				this.totalPhase3Time = phases.get(p).getTotalPhaseTime();
				this.totalPhase3Energy = phases.get(p).getTotalPhaseEnergy();
				this.channel(ChannelId.PHASE3_TIME).setNextValue(this.totalPhase3Time);
				this.channel(ChannelId.PHASE3_ENERGY).setNextValue(this.totalPhase3Energy);
			}
		}

		this.totalPhaseTime = 0;
		this.totalPhaseEnergy = 0;
		for (PhaseDef p : phases.values()) {
			this.totalPhaseTime += p.getTotalPhaseTime();
			this.totalPhaseEnergy += p.getTotalPhaseEnergy();
		}
		this.channel(ChannelId.TOTAL_PHASE_TIME).setNextValue(this.totalPhaseTime);
		// Keep updating the mintime comparing it with the total phase time
		this.countDownMinTime = this.minTime - this.totalPhaseTime;
		if (this.countDownMinTime < 0) {
			this.countDownMinTime = 0;
		}

		/**
		 * this variable holds the total time of level_1
		 * 
		 */
		this.level1Time = this.totalPhase1Time - this.totalPhase2Time;
		/**
		 * this variable holds the total time of level_2
		 * 
		 */
		this.level2Time = this.totalPhase2Time - this.totalPhase3Time;
		/**
		 * this variable holds the total time of level_3
		 * 
		 */
		this.level3Time = this.totalPhase3Time;

		this.level1Energy = (level1Time / MILI_SEC_PER_DAY) * config.powerOfPhase();
		this.level2Energy = (level2Time / MILI_SEC_PER_DAY) * (config.powerOfPhase() * TWO);
		this.level3Energy = (level3Time / MILI_SEC_PER_DAY) * (config.powerOfPhase() * THREE);

		this.channel(ChannelId.LEVEL1_TIME).setNextValue(level1Time);
		this.channel(ChannelId.LEVEL2_TIME).setNextValue(level2Time);
		this.channel(ChannelId.LEVEL3_TIME).setNextValue(level3Time);
		this.channel(ChannelId.LEVEL1_ENERGY).setNextValue(level1Energy);
		this.channel(ChannelId.LEVEL2_ENERGY).setNextValue(level2Energy);
		this.channel(ChannelId.LEVEL3_ENERGY).setNextValue(level3Energy);
		this.channel(ChannelId.TOTAL_PHASE_ENERGY).setNextValue(this.totalPhaseEnergy);

		// keep updating the minKwh comparing it with the total phase power
		this.countDownMinKwh = this.minKwh - this.totalPhaseEnergy;
	}

	/**
	 * This method is running the priority mode at the "end time", force switches on
	 * the phases based on the "Gear" configured.
	 * 
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void runPriority() throws IllegalArgumentException, OpenemsNamedException {
		switch (this.heatingLevel) {
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
	 * Function to check change in the day.
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
		long excessPower = 0;

		// Calculate the Excess power
		if (gridActivePower > 0) {
			excessPower = 0;
		} else {
			excessPower = Math.abs(gridActivePower);
			excessPower += this.noRelaisSwitchedOn * config.powerOfPhase();
		}

		// resetting the variables if there is change in the day.
		if (checkChangeInDay()) {
			for (PhaseDef p : phases.values()) {
				p.setTotalPhaseEnergy(0);
				p.setTotalPhaseTime(0);
				p.getTimeStopwatch().reset();
			}
			this.minKwh = config.minkwh();
			this.minTime = this.getSeconds(this.heatingLevel, this.priority, config.minTime(), this.minKwh);
			this.endtime = LocalTime.parse(this.config.endTime());
			this.level1Time = 0;
			this.level2Time = 0;
			this.level3Time = 0;
			this.level1Energy = 0;
			this.level2Energy = 0;
			this.level3Energy = 0;
			return;
		}

		LocalTime now = LocalTime.parse(FORMATTER.format(LocalTime.now()));
		LocalTime updateEndtime = endtime.minusSeconds((long) this.countDownMinTime);

//		System.out.println("Current time is " + now);
//		System.out.println("endtime from config : " + this.endtime);
//		System.out.println("Updated end time is : " + updateEndtime);
//		System.out.println("updated min kwh is : " + this.countDownMinKwh);
//		System.out.println("updated min time is : " + this.countDownMinTime);

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
			case KILO_WATT_HOUR:
				runPriority();
				break;
			}
			break;
		case NORMAL_MODE:
			boolean stateChanged;
			do {
				if (excessPower >= (config.powerOfPhase() * THREE)) {
					stateChanged = this.changeState(Level.LEVEL_3);
				} else if (excessPower >= (config.powerOfPhase() * TWO)) {
					stateChanged = this.changeState(Level.LEVEL_2);
				} else if (excessPower >= config.powerOfPhase()) {
					stateChanged = this.changeState(Level.LEVEL_1);
				} else {
					stateChanged = this.changeState(Level.LEVEL_0);
				}
			} while (stateChanged); // execute again if the state changed

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
			// store current state in StateMachine channel
			this.channel(ChannelId.LEVEL).setNextValue(this.level);
			this.channel(ChannelId.NO_OF_RELAIS_ON).setNextValue(this.noRelaisSwitchedOn);
			this.channel(ChannelId.COUNT_DOWN_MIN_TIME).setNextValue(this.countDownMinTime);
			this.channel(ChannelId.COUNT_DOWN_MIN_KWH).setNextValue(this.countDownMinKwh);
			break;
		}
	}

	/**
	 * This method calls the computeTime method on individual phaseDef objects, and
	 * computes the total time the Phase was switched on.
	 * 
	 * @param phases All the {@link PhaseDef} objects
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
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
	private boolean changeState(Level nextLevel) {
		if (this.level != nextLevel) {
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				this.level = nextLevel;
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
	 * A flag to maintain change in the mode.
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
	 * This method return the minTime in seconds.
	 * 
	 * @param minTime is a double, which is configured as hours in configuration
	 * @return return the no of minutes.
	 */
	private double getSeconds(Level heatingLevel, Priority priority, double configuredTime, double minKwh) {
		double calculatedTime = 0;
		switch (priority) {
		case TIME:
			switch (heatingLevel) {
			case LEVEL_0:
				// this is basically specifying the minTime is zero
				return calculatedTime;
			case LEVEL_1:
			case LEVEL_2:
			case LEVEL_3:
				return configuredTime * SEC_PER_DAY; // Converting the time configured as hours into mili seconds
			}
			break;
		case KILO_WATT_HOUR:

			switch (heatingLevel) {
			case LEVEL_0:
				// this is basically specifying the minTime equal to zero
				return calculatedTime;
			case LEVEL_1:
				calculatedTime = (minKwh / config.powerOfPhase()) * SEC_PER_DAY;
				break;
			case LEVEL_2:
				calculatedTime = (minKwh / (config.powerOfPhase() * TWO)) * SEC_PER_DAY;
				break;
			case LEVEL_3:
				calculatedTime = (minKwh / (config.powerOfPhase() * THREE)) * SEC_PER_DAY;
				break;
			}
			return calculatedTime;
		}
		// it should never reach this.
		return calculatedTime;

	}

}