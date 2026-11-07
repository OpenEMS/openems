package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;
import static io.openems.edge.common.type.Phase.SinglePhase.L2;
import static io.openems.edge.common.type.Phase.SinglePhase.L3;
import static io.openems.edge.controller.io.heatingelement.Utils.getRequiredLevelFromAvgPowers;
import static io.openems.edge.controller.io.heatingelement.Utils.getRequiredLevelFromPower;
import static io.openems.edge.controller.io.heatingelement.Utils.getRequiredPower;
import static io.openems.edge.controller.io.heatingelement.Utils.getTimeToForceHeat;
import static io.openems.edge.controller.io.heatingelement.Utils.getTotalAvgPower;
import static io.openems.edge.controller.io.heatingelement.Utils.isFinal10Minutes;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.io.heatingelement.Utils.CumulatedActiveTimes;
import io.openems.edge.controller.io.heatingelement.Utils.HeatingPhases;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.IO.HeatingElement", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@GenerateTargetsFromReferences("meter")
public class ControllerIoHeatingElementImpl extends AbstractOpenemsComponent
		implements ControllerIoHeatingElement, Controller, OpenemsComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(ControllerIoHeatingElementImpl.class);

	private static final Duration LAST_PHASE_DURATION_TO_TURN_UP = Duration.ofMinutes(30);
	private static final double LAST_PHASE_PERCENTAGE_TO_TURN_UP = 0.9;
	private static final double PERCENTAGE_ENERGYLIMIT_TO_NOT_TURN_UP = 0.01;
	private static final Duration SHOWING_DONE_FOR_HALF_AN_HOUR = Duration.ofMinutes(30);
	private static final int CALIBRATION_SECONDS = 4;
	private static final double SECONDS_PER_HOUR = Duration.ofHours(1).toSeconds();
	private static final Duration FORCE_HEATING_BUFFER = Duration.ofMinutes(10);

	/*
	 * Definitions for each phase.
	 */
	private final HeatingPhases phases;

	/**
	 * Ordered list of relay-output steps. Level N switches ON the first N steps of
	 * this list. For {@link NumberOfHeatingElements#ONE} it is built from the three
	 * existing phase outputs; for {@link NumberOfHeatingElements#TWO} it
	 * additionally contains the three Steps of Heating Element B, interleaved
	 * according to the configured {@link Distribution}.
	 */
	private List<HeatingElementStep> steps;

	/**
	 * True if only Heating Element A (the legacy 3-relay setup) is configured. In
	 * that case Level 1-3 switching is still routed through {@link PhaseDef} to
	 * preserve the existing calibration / average-power logic. For
	 * {@link NumberOfHeatingElements#TWO}, switching always goes through
	 * {@link #steps} directly, since {@link PhaseDef} is hard-wired to Heating
	 * Element A's output channels and would not reflect the configured
	 * {@link Distribution}.
	 */
	private boolean usePhaseBasicLogic;

	/*
	 * Cumulated active time for each level.
	 */
	private final CumulatedActiveTimes cumulatedActiveTimes = new CumulatedActiveTimes(
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL1_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL2_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL3_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL4_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL5_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL6_CUMULATED_TIME));

	// Ordered step durations also account for TWO/PARALLEL without using PhaseDef.
	private final List<StepActiveDuration> stepDurations = List.of(new StepActiveDuration(), new StepActiveDuration(),
			new StepActiveDuration(), new StepActiveDuration(), new StepActiveDuration(), new StepActiveDuration());

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference(cardinality = OPTIONAL, policyOption = GREEDY, //
			target = "(&(id=${config.meter_id})(enabled=true))")
	private volatile ElectricityMeter meter;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	/** The minimum time the phases should be switched on the mode Energy. */
	private LocalTime timeToForceHeat;

	/** The configured endTime for the mode Energy. */
	private LocalTime endTimeWorkModeEnergy;

	/** A boolean value if in the mode Energy the heating is forced. */
	private boolean isForceHeatingActive = false;

	/** A boolean value if in the last half an hour the level is turned up. */
	private boolean isForceHeatingInTheEndActive = false;

	/** The state that is saved from the last cycle. */
	private Status lastRunState = Status.INACTIVE;

	/** The current runState. */
	private Status runState;

	/** A list of the tasks in JSCalender Format. */
	private JSCalendar.Tasks<Payload> schedule = JSCalendar.Tasks.empty();

	/** The minimum energy limit for the mode Energy. */
	private long energyLimit;

	/** A timestamp when a task is finished. */
	private ZonedDateTime timestampTaskFinished;

	/** A timestamp when the calibration should be ended. */
	private LocalDateTime timestampCalibrationEnd;

	/** A boolean value if the calibration did start. */
	private boolean gotCalibrationStarted = true;

	/** The energy the session have at the start. */
	private long sessionStartEnergy;

	/** A date the last reset of the session energy happens. */
	private LocalDate lastEnergyResetDay = null;

	/** Holds the minimum time the phases should be switched on in [Ws]. */
	private long minimumTotalPhaseTime;
	/** Tracks the currently applied Level and the symmetric SURPLUS hysteresis. */
	private final Hysteresis hysteresis = new Hysteresis();
	/** Last active task. */
	private OneTask<Payload> lastTask = null;

	/** The configured endTime in the mode time. */
	private LocalTime endTimeWorkModeTime;
	private Config config;

	public ControllerIoHeatingElementImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerIoHeatingElement.ChannelId.values() //
		);
		this.phases = new HeatingPhases(new PhaseDef(this, L1), new PhaseDef(this, L2), new PhaseDef(this, L3));
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) throws OpenemsNamedException {
		var newSteps = StepBuilder.buildSteps(config);
		if (this.steps != null) {
			for (var step : this.steps) {
				if (newSteps.stream().noneMatch(next -> next.outputChannel().equals(step.outputChannel()))) {
					// Always write OFF: a pending ON may not yet be reflected in the read value.
					try {
						WriteChannel<Boolean> output = this.componentManager.getChannel(step.outputChannel());
						output.setNextWriteValue(false);
					} catch (OpenemsNamedException | IllegalArgumentException e) {
						this.logWarn(this.log, "Unable to switch OFF removed output [" + step.outputChannel() + "]: "
								+ e.getMessage());
					}
				}
			}
		}
		this.config = config;
		this.steps = newSteps;
		this.usePhaseBasicLogic = config.numberOfHeatingElements() == NumberOfHeatingElements.ONE;
		this.minimumTotalPhaseTime = calculateMinimumTotalPhaseTime(config);
		this.schedule = JSCalendar.Tasks.fromStringOrEmpty(this.componentManager.getClock(), config.schedule(),
				Payload.serializer());
		this.resetProps();
		if (this.config.mode() == Mode.AUTOMATIC) {
			this.updateEndTime();
		}
	}

	@Override
	public void run() throws OpenemsNamedException {

		// Handle the selected operating mode.
		var runState = switch (this.config.mode()) {
		case AUTOMATIC //
			-> this.modeAutomatic();
		case MANUAL_OFF -> {
			this.modeManualOff();
			yield Status.INACTIVE;
		}
		case MANUAL_ON -> {
			this.modeManualOn();
			yield Status.ACTIVE;
		}
		case FAST_HEAT -> {
			setValue(this, ControllerIoHeatingElement.ChannelId.WAITING_FOR_CALIBRATION, false);
			this.applyLevel(Level.fromValue(this.steps.size()));
			yield Status.ACTIVE;
		}
		};

		if (this.isPowerCapturedByMeter()) {
			this.checkAndResetDailyEnergy();
			this.calculateAvgPowers();
			this.calculateSessionEnergy();
		}

		// Calculate Phase Time
		var phase1Time = (int) this.phases.phase1().getTotalDuration().getSeconds();
		var phase2Time = (int) this.phases.phase2().getTotalDuration().getSeconds();
		var phase3Time = (int) this.phases.phase3().getTotalDuration().getSeconds();
		if (!this.usePhaseBasicLogic) {
			var isParallel = this.config.distribution() == Distribution.PARALLEL;
			phase1Time = this.getStepTime(0);
			phase2Time = this.getStepTime(isParallel ? 2 : 1);
			phase3Time = this.getStepTime(isParallel ? 4 : 2);
		}
		final var totalPhaseTime = this.getTotalHeatingTime();

		// Update Channels
		setValue(this, ControllerIoHeatingElement.ChannelId.STATUS, runState);
		setValue(this, ControllerIoHeatingElement.ChannelId.PHASE1_TIME, phase1Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.PHASE2_TIME, phase2Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.PHASE3_TIME, phase3Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.TOTAL_PHASE_TIME, totalPhaseTime);

		if (this.usePhaseBasicLogic) {
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL1_TIME, phase1Time - phase2Time);
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL2_TIME, phase2Time - phase3Time);
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL3_TIME, phase3Time);
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL4_TIME, 0);
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL5_TIME, 0);
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL6_TIME, 0);
		} else {
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL1_TIME, this.getStepTime(0) - this.getStepTime(1));
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL2_TIME, this.getStepTime(1) - this.getStepTime(2));
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL3_TIME, this.getStepTime(2) - this.getStepTime(3));
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL4_TIME, this.getStepTime(3) - this.getStepTime(4));
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL5_TIME, this.getStepTime(4) - this.getStepTime(5));
			setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL6_TIME, this.getStepTime(5));
		}

		this.updateCumulatedActiveTime();

	}

	/**
	 * Handle Mode "Manual On".
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void modeManualOn() throws IllegalArgumentException, OpenemsNamedException {
		if (this.isPowerCapturedByMeter()) {
			setValue(this, ControllerIoHeatingElement.ChannelId.WAITING_FOR_CALIBRATION, false);
		}
		this.applyLevel(this.config.defaultLevel());
	}

	/**
	 * Handle Mode "Manual Off".
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void modeManualOff() throws IllegalArgumentException, OpenemsNamedException {
		this.applyLevel(Level.LEVEL_0);
	}

	/**
	 * Handle Mode "Automatic".
	 *
	 * @return run state
	 * @throws IllegalArgumentException on error.
	 * @throws OpenemsNamedException    on error.
	 */
	protected Status modeAutomatic() throws IllegalArgumentException, OpenemsNamedException {

		boolean waiting = this.getWaitingForCalibration().orElse(true);

		if (this.gotCalibrationStarted) {
			this.timestampCalibrationEnd = LocalDateTime.now(this.componentManager.getClock())
					.plusSeconds(CALIBRATION_SECONDS);
			this.gotCalibrationStarted = false;
		}

		if (waiting && this.isCalibrationSupported()) {
			return this.calibrate();
		}

		// Get the input channel addresses
		IntegerReadChannel gridActivePowerChannel = this.sum.getGridActivePowerChannel();
		int gridActivePower = gridActivePowerChannel.value().getOrError();
		IntegerReadChannel essDischargePowerChannel = this.sum.getEssDischargePowerChannel();
		int essDischargePower = essDischargePowerChannel.value().orElse(0 /* if there is no storage */);

		// TODO: use charge power if user selects heating element priority.
		if (essDischargePower < 0) { // we are only interested in discharging, not charging
			essDischargePower = 0;
		}

		long excessPower = this.calculateExcessPower(gridActivePower, essDischargePower);
		Level targetLevel = this.getRequiredLevel(excessPower);
		this.runState = this.getStateFromLevel(targetLevel);

		// Example with schedule
		if (this.isScheduleConfigured()) {
			targetLevel = this.getLevelFromSchedule(targetLevel);
		} else {
			targetLevel = switch (this.config.workMode()) {
			case WorkMode.TIME -> this.modeTime(targetLevel);
			case WorkMode.ENERGY -> this.modeEnergy(targetLevel);
			case WorkMode.NONE -> targetLevel;
			};
		}
		targetLevel = switch (this.runState) {
		case ACTIVE_FORCED_LIMIT, ACTIVE_FORCED, UNREACHABLE -> targetLevel;
		case UNDEFINED, INACTIVE, ACTIVE, DONE, CALIBRATION -> this.applyHysteresis(targetLevel);
		};

		this.applyLevel(targetLevel);
		this.lastRunState = this.runState;
		return this.runState;
	}

	private Level getLevelFromSchedule(Level targetLevel) throws IllegalArgumentException, OpenemsNamedException {
		var ot = this.schedule.getActiveOneTask();
		var now = ZonedDateTime.now(this.componentManager.getClock());

		if (ot != null) {
			this.lastTask = ot;
			this.timestampTaskFinished = null;
			this.updateEndTime();
		}

		if (ot == null && this.lastTask != null && now.isAfter(this.lastTask.end())) {
			if (this.timestampTaskFinished == null) {
				this.timestampTaskFinished = now;
				this.resetProps();
			}
		}

		boolean lastTaskJustFinished = this.timestampTaskFinished != null //
				&& now.isBefore(this.timestampTaskFinished.plusSeconds(SHOWING_DONE_FOR_HALF_AN_HOUR.getSeconds()));

		/*
		 * show the state done for half an hour, if the Workmode is Energy
		 */
		if (ot == null) {
			if (lastTaskJustFinished && this.config.workMode() == WorkMode.ENERGY) {
				this.runState = Status.DONE;
			} else {
				this.runState = Status.INACTIVE;

				if (!lastTaskJustFinished) {
					this.timestampTaskFinished = null;
					this.lastTask = null;
				}
			}
			return Level.LEVEL_0;
		}

		return switch (this.config.workMode()) {
		case WorkMode.TIME -> this.modeTime(targetLevel);
		case WorkMode.ENERGY -> this.modeEnergy(targetLevel);
		case WorkMode.NONE -> targetLevel;
		};
	}

	private Level modeTime(Level targetLevel) throws IllegalArgumentException, OpenemsNamedException {
		var now = LocalTime.now(this.componentManager.getClock());
		var latestForceChargeStartTime = this.calculateLatestForceHeatingStartTime(this.endTimeWorkModeTime);

		/*
		 * Force heat is active if the minimum time for the configured mode is not
		 * reached and no time left to heat automatically.
		 */

		final Integer forceStartAtSecondsOfDay;
		if (now.isAfter(this.endTimeWorkModeTime) || latestForceChargeStartTime == null) {
			forceStartAtSecondsOfDay = null;
		} else {

			// Force-heat with configured default level or higher
			if (now.isAfter(latestForceChargeStartTime)
					&& targetLevel.getValue() <= this.config.defaultLevel().getValue()) {
				targetLevel = this.config.defaultLevel();
				this.runState = Status.ACTIVE_FORCED;
			}

			forceStartAtSecondsOfDay = latestForceChargeStartTime.toSecondOfDay();
		}

		setValue(this, ControllerIoHeatingElement.ChannelId.FORCE_START_AT_SECONDS_OF_DAY, forceStartAtSecondsOfDay);

		return targetLevel;
	}

	private Level modeEnergy(Level excessPowerLevel) {
		this.endTimeWorkModeEnergy = this.getCalculatedEndTimeOfWorkModeEnergy();
		this.energyLimit = this.getEnergyLimit();
		final LocalTime now = LocalTime.now(this.componentManager.getClock());

		/*
		 * It will calculate the start point it should be forced to heat, if the
		 * currentDate is before the start point, and it wasn't already forced to heat.
		 */
		if (this.timeToForceHeat == null || (now.isBefore(this.timeToForceHeat) && !this.isForceHeatingActive)) {
			if (this.usePhaseBasicLogic) {
				this.timeToForceHeat = getTimeToForceHeat(this.energyLimit, this.getSessionEnergy().orElse(0L),
						this.phases, this.endTimeWorkModeEnergy, now);
			} else {
				var remainingEnergy = this.energyLimit - this.getSessionEnergy().orElse(0L);
				var nominalPower = Math.max(this.getNominalLevelPower(Level.LEVEL_1), 1);
				var heatingSeconds = (long) Math.ceil(remainingEnergy * SECONDS_PER_HOUR / nominalPower);
				var seconds = heatingSeconds + FORCE_HEATING_BUFFER.toSeconds();
				this.timeToForceHeat = this.endTimeWorkModeEnergy
						.minusSeconds(Math.min(seconds, this.endTimeWorkModeEnergy.toSecondOfDay()));
			}
		}

		if (this.checkIfEndTimeIsTommorow()) {
			return excessPowerLevel;
		}

		/*
		 * if the endTime or the minimum energy limit is reached, it will stop heating
		 * and the state will be done.
		 */
		if (this.checkIfSessionDone(now)) {
			this.runState = Status.DONE;
			return Level.LEVEL_0;
		}

		/*
		 * The level will remain if the in the last half an hour the level was forced to
		 * turn up to reach the limit except if the pv power will go up during that
		 * time.
		 */
		if (this.isForceHeatingInTheEndActive) {
			this.runState = Status.ACTIVE_FORCED_LIMIT;
			Level currentLevel = this.getLevel().asEnum();
			this.isForceHeatingInTheEndActive = !this.isExcessPowerSufficient(currentLevel, excessPowerLevel);
			return Level.values()[currentLevel.ordinal()];
		}

		Duration totalRemainingTime = Duration.between(now, this.endTimeWorkModeEnergy);

		/*
		 * if the average sum of all levels + 10% buffer is less than the required power
		 * it will determine that the minimum energy limit can't be reached and the
		 * state will be unreachable. It will heat all level to at least reach most as
		 * possible.
		 */
		if (this.checkIfSessionUnreachable(totalRemainingTime)) {
			this.runState = Status.UNREACHABLE;
			return Level.fromValue(this.steps.size());
		}

		/*
		 * if the current time is equal or after the time to force heating, it will
		 * force the heating, even when there is no surplus from the PV.
		 */
		if (this.timeToForceHeat != null && (now.isAfter(this.timeToForceHeat) || now.equals(this.timeToForceHeat))) {
			return this.forcedToHeat(totalRemainingTime, excessPowerLevel);
		}

		return excessPowerLevel;
	}

	/**
	 * A method, that is called during the time, the heating element is forced to
	 * heat.
	 *
	 * @param totalRemainingTime the total remaining time till the endTime.
	 * @param excesspowerLevel   the level to be activated depending on the
	 *                           available excess power
	 *
	 * @return the level that should be switched to.
	 */
	private Level forcedToHeat(Duration totalRemainingTime, Level excesspowerLevel) {
		this.runState = Status.ACTIVE_FORCED_LIMIT;
		this.isForceHeatingActive = true;

		long requiredPower = getRequiredPower(this.energyLimit, this.getSessionEnergy().orElse(0L), totalRemainingTime);
		Level requiredLevel = this.getRequiredLevel(requiredPower);
		if (!this.usePhaseBasicLogic && this.getNominalLevelPower(requiredLevel) < requiredPower
				&& requiredLevel.getValue() < this.steps.size()) {
			requiredLevel = Level.fromValue(requiredLevel.getValue() + 1);
		}

		/*
		 * It should always heat, except if the requiredPower is significant low, for
		 * e.g. if the PV surplus was high during the forced time heating and then
		 * suddenly it will stop. During the last 10 minutes it should always heat.
		 */
		if (requiredLevel == Level.LEVEL_0
				&& (this.isPowerAbovePhase1Threshold(requiredPower) || isFinal10Minutes(totalRemainingTime))) {
			requiredLevel = Level.LEVEL_1;
		}

		double threshold = this.getRequiredLevelThreshold(requiredLevel);

		/*
		 * if the last half an hour begins, it checks if the requiredPower is more than
		 * the average power of the required Level. When true it will turn up a level to
		 * reach the minimum limit, also if the rest energy is less than 1% the
		 * configured minimum limit, it will not turn up, so it will not turn up the
		 * level last second.
		 *
		 */
		if (totalRemainingTime.getSeconds() <= LAST_PHASE_DURATION_TO_TURN_UP.getSeconds() //
				&& requiredLevel.getValue() < this.steps.size() //
				&& threshold < requiredPower * LAST_PHASE_PERCENTAGE_TO_TURN_UP //
				&& this.energyLimit - this.getSessionEnergy().orElse(0L) > this.energyLimit
						* PERCENTAGE_ENERGYLIMIT_TO_NOT_TURN_UP) {

			requiredLevel = Level.fromValue(requiredLevel.getValue() + 1);
			this.isForceHeatingInTheEndActive = !this.isExcessPowerSufficient(requiredLevel, excesspowerLevel);
		}

		/*
		 * If the level of the surplus of the PV is still higher than the level from the
		 * requiredPower, it should heat with the PV excessPower.
		 */
		if (this.isExcessPowerSufficient(requiredLevel, excesspowerLevel)) {
			this.runState = Status.ACTIVE;
			return excesspowerLevel;
		}
		return requiredLevel;
	}

	/**
	 * Gets the power threshold to compare the required power against when deciding
	 * whether to turn up an additional Level in {@link #forcedToHeat}.
	 *
	 * <p>
	 * For a calibrated single heating element (Level 1-3), the calibrated phase
	 * average power is used, as before. Otherwise (e.g. Level 4-6 of a second
	 * heating element, or no meter), the configured nominal Step power of the given
	 * Level is used instead.
	 *
	 * @param requiredLevel the Level required to reach the energy limit
	 * @return the threshold power in [W]
	 */
	private double getRequiredLevelThreshold(Level requiredLevel) {
		if (!this.isCalibrationSupported()) {
			return this.getNominalLevelPower(requiredLevel);
		}
		if (requiredLevel == Level.LEVEL_1) {
			return this.phases.phase1().getAvgPower();
		}
		return (double) this.phases.phase1().getAvgPower() + this.phases.phase2().getAvgPower();
	}

	/**
	 * Performs an initial calibration to determine the average power of the heating
	 * element phases when they are switched on. - Forces level 3 for a short time
	 * and measures the consumption to obtain realistic starting values for the
	 * average power per phase, - Improves the accuracy of the first switching
	 * decision from level 0.
	 *
	 * @return the state Calibration.
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private Status calibrate() throws IllegalArgumentException, OpenemsNamedException {
		this.applyLevel(Level.LEVEL_3);
		if (LocalDateTime.now(this.componentManager.getClock()).isEqual(this.timestampCalibrationEnd)
				|| LocalDateTime.now(this.componentManager.getClock()).isAfter(this.timestampCalibrationEnd)) {
			setValue(this, ControllerIoHeatingElement.ChannelId.WAITING_FOR_CALIBRATION, false);
		}
		return Status.CALIBRATION;
	}

	/**
	 * Calculates the power that can be consumed by the heating element.
	 *
	 * @param gridActivePower   the current active power of the grid
	 * @param essDischargePower the current discharge power of the battery
	 *
	 * @return the excessPower
	 */
	private long calculateExcessPower(int gridActivePower, int essDischargePower) {

		var power = 0;
		if (!this.isPowerCapturedByMeter()) {
			power = this.getNominalLevelPower(this.hysteresis.getCurrentLevel());
		} else {
			power = this.meter.getActivePower().orElse(0);
		}

		return gridActivePower * -1 - essDischargePower + power;
	}

	/**
	 * Calculates the required Level.
	 *
	 * @param excessPower the power that can be used
	 * @return the level that should be switched on
	 */
	private Level getRequiredLevel(long excessPower) {
		if (this.isCalibrationSupported()) {
			return getRequiredLevelFromAvgPowers(excessPower, this.phases);
		}
		return getRequiredLevelFromPower(excessPower, this.steps);
	}

	/**
	 * Checks if the configured end time is before the current time, so it will be
	 * the next day.
	 *
	 * @return true if the end time is before the current date
	 */
	private boolean checkIfEndTimeIsTommorow() {
		LocalTime now = LocalTime.now(this.componentManager.getClock());
		LocalTime configuredEndTime = this.endTimeWorkModeEnergy;
		return now.isAfter(configuredEndTime) && !this.checkIfSessionDone(now);
	}

	/**
	 * Calculates the power of a phase including the current power of the phase.
	 */
	private void calculateAvgPowers() {
		this.phases.phase1().calculateAvgPower(this.meter.getActivePowerL1().orElse(0));
		this.phases.phase2().calculateAvgPower(this.meter.getActivePowerL2().orElse(0));
		this.phases.phase3().calculateAvgPower(this.meter.getActivePowerL3().orElse(0));
	}

	/**
	 * Reset the properties for the mode Energy.
	 */
	private void resetProps() {
		this.isForceHeatingActive = false;
		this.isForceHeatingInTheEndActive = false;
		this.timeToForceHeat = null;
	}

	/**
	 * Get the state from the level.
	 *
	 * @param targetLevel the targeted Level, that should be switched to
	 * @return the state
	 */
	private Status getStateFromLevel(Level targetLevel) {
		return targetLevel == Level.LEVEL_0 ? Status.INACTIVE : Status.ACTIVE;
	}

	/**
	 * Calculates the session energy.
	 */
	private void calculateSessionEnergy() {
		setValue(this, ControllerIoHeatingElement.ChannelId.SESSION_ENERGY, //
				this.meter.getActiveProductionEnergy().orElse(0L) - this.sessionStartEnergy);
	}

	/**
	 * Checks if it's a new day and if yes it will reset the session energy.
	 */
	private void checkAndResetDailyEnergy() {

		LocalDate today = LocalDate.now(this.componentManager.getClock());

		if (!this.meter.getActiveProductionEnergy().isDefined()) {
			return;
		}

		// If the task in the schedule lasts over multiple days, the daily energy will
		// not be reset.
		if (this.isScheduleConfigured()) {
			var highPeriod = this.schedule.getActiveOneTask();

			if (highPeriod == null) {
				return; // Session didn't start yet
			}
			var startDateTask = highPeriod.start() //
					.withZoneSameInstant(this.componentManager.getClock().getZone()).toLocalDate();
			var endDateTask = highPeriod.end() //
					.withZoneSameInstant(this.componentManager.getClock().getZone()).toLocalDate();
			if (!startDateTask.equals(endDateTask)) {
				return;
			}
		}

		// Do session start only once a day.
		if (this.lastEnergyResetDay == null || !this.lastEnergyResetDay.equals(today)) {
			this.lastEnergyResetDay = today;
			this.resetSessionEnergy();
		}
	}

	/**
	 * Resets the session energy to 0.
	 */
	private void resetSessionEnergy() {
		this.sessionStartEnergy = this.meter.getActiveProductionEnergy().get();
		setValue(this, ControllerIoHeatingElement.ChannelId.SESSION_ENERGY, 0);
	}

	/**
	 * Checks if the minimum limit or the endTime is reached.
	 *
	 * @param nowTime the current time
	 * @return a boolean value if the session is done
	 */
	private boolean checkIfSessionDone(LocalTime nowTime) {
		return nowTime.isAfter(this.endTimeWorkModeEnergy) || nowTime.equals(this.endTimeWorkModeEnergy)
				|| this.energyLimit <= this.getSessionEnergy().orElse(0L);
	}

	/**
	 * Checks if the session is unreachable.
	 *
	 * @param totalRemainingTime the total remaining time of the session
	 * @return a boolean value if the session is unreachable
	 */
	private boolean checkIfSessionUnreachable(Duration totalRemainingTime) {
		var maximumPower = this.usePhaseBasicLogic ? getTotalAvgPower(this.phases) * 1.1
				: this.getNominalLevelPower(Level.fromValue(this.steps.size()));
		return Math.ceil((this.energyLimit - this.getSessionEnergy().orElse(0L)) * 3600D
				/ Math.max(totalRemainingTime.getSeconds(), 1)) > maximumPower;
	}

	/**
	 * Gets the calculated ending time of the mode Energy.
	 *
	 * @return the calculated ending time
	 */
	private LocalTime getCalculatedEndTimeOfWorkModeEnergy() {
		if (!this.isScheduleConfigured()) {
			return LocalTime.parse(this.config.endTimeWithMeter()); // Get from config
		}
		var highPeriod = this.schedule.getActiveOneTask();
		if (highPeriod == null) {
			return LocalTime.MIN; // Task didn't start yet
		}
		return highPeriod.end().toLocalTime();
	}

	/**
	 * Gets the calculated ending time of the mode Time.
	 *
	 * @return the calculated ending time
	 */
	private LocalTime getCalculatedEndTimeOfWorkModeTime() throws OpenemsException {
		if (!this.isScheduleConfigured()) {
			return DateUtils.parseLocalTimeOrError(this.config.endTime()); // Get from config
		}
		var highPeriod = this.schedule.getActiveOneTask();
		if (highPeriod == null) {
			return LocalTime.MIN; // Task didn't start yet
		}
		return highPeriod.end().toLocalTime();
	}

	/**
	 * Gets the minimum limit of the mode Energy.
	 *
	 * @return the minimum limit
	 */
	private int getEnergyLimit() {
		var highPeriod = this.schedule.getLastActiveOneTask();
		if (this.isScheduleConfigured() && highPeriod != null) {
			return highPeriod.payload().sessionEnergy();
		}
		return this.config.minEnergylimit();

	}

	/**
	 * Checks if the required power is more than 40 percent of the average power of
	 * the first phase.
	 *
	 * @param requiredPower the required power in Watt
	 * @return a boolean value, true if it's higher than 40 percent.
	 */
	private boolean isPowerAbovePhase1Threshold(double requiredPower) {
		return requiredPower > (this.usePhaseBasicLogic ? this.phases.phase1().getAvgPower()
				: this.getNominalLevelPower(Level.LEVEL_1)) * 0.4;
	}

	/**
	 * Helper method to check if the level of the excess power is sufficient to heat
	 * than the required level.
	 *
	 * @param requiredLevel    the level it should heat to reach the energy limit
	 * @param excessPowerLevel the level it can heat from the excess power
	 * @return true if the level of the excess power is sufficient
	 */
	private boolean isExcessPowerSufficient(Level requiredLevel, Level excessPowerLevel) {
		return requiredLevel.getValue() <= excessPowerLevel.getValue();
	}

	/**
	 * Helper method to check if the heating element has a meter and the power of it
	 * can be captured.
	 * 
	 * @return a boolean value, true if the meter is present.
	 */
	private boolean isPowerCapturedByMeter() {
		return this.meter != null;
	}

	/**
	 * Helper method to check if the automatic calibration and the resulting
	 * average-power based required-Level calculation are applicable.
	 *
	 * <p>
	 * Calibration is only supported for {@link NumberOfHeatingElements#ONE},
	 * because it relies on {@link PhaseDef}, which is hard-wired to Heating Element
	 * A's three relay outputs and its calibrated average power per phase. For
	 * {@link NumberOfHeatingElements#TWO} calibration is skipped and the required
	 * Level is always derived from the configured Step powers instead (see
	 * {@link Utils#getRequiredLevelFromPower}).
	 *
	 * @return true if calibration / average-power based Level calculation should be
	 *         used.
	 */
	private boolean isCalibrationSupported() {
		return this.isPowerCapturedByMeter() //
				&& this.config.numberOfHeatingElements() == NumberOfHeatingElements.ONE;
	}

	/**
	 * Helper method to check if the schedule is configured.
	 * 
	 * @return a boolean value, true if the schedule is configured
	 */
	private boolean isScheduleConfigured() {
		return !this.config.schedule().isEmpty();
	}

	/**
	 * Updates the end time for the mode Automatic.
	 *
	 * @throws OpenemsException on error
	 */
	private void updateEndTime() throws OpenemsException {
		switch (this.config.workMode()) {
		case WorkMode.TIME -> this.endTimeWorkModeTime = this.getCalculatedEndTimeOfWorkModeTime();
		case WorkMode.ENERGY -> this.endTimeWorkModeEnergy = this.getCalculatedEndTimeOfWorkModeEnergy();
		case WorkMode.NONE -> {
			/* Do nothing */ }
		}
	}

	/**
	 * Calculates the minimum total phase time the user demands in [s].
	 *
	 * <ul>
	 * <li>in {@link WorkMode#TIME}:
	 * <ul>
	 * <li>default level {@link Level#LEVEL_0}: return 0
	 * <li>default level {@link Level#LEVEL_1}: return configured time
	 * <li>default level {@link Level#LEVEL_2}: return configured time * 2
	 * <li>default level {@link Level#LEVEL_3}: return configured time * 3
	 * <li>default level {@link Level#LEVEL_4}: return configured time * 4
	 * <li>default level {@link Level#LEVEL_5}: return configured time * 5
	 * <li>default level {@link Level#LEVEL_6}: return configured time * 6
	 * </ul>
	 * <li>in {@link WorkMode#NONE}: always return 0
	 * </ul>
	 *
	 * @param config the component {@link Config}
	 * @return the minimum total phase time [s]
	 */
	private static long calculateMinimumTotalPhaseTime(Config config) {
		return switch (config.workMode()) {
		case TIME //
			-> switch (config.defaultLevel()) {
			case LEVEL_0 -> 0;
			case LEVEL_1 -> config.minTime() * 3600L;
			case LEVEL_2 -> (long) config.minTime() * 3600 * 2;
			case LEVEL_3 -> (long) config.minTime() * 3600 * 3;
			case LEVEL_4 -> (long) config.minTime() * 3600 * 4;
			case LEVEL_5 -> (long) config.minTime() * 3600 * 5;
			case LEVEL_6 -> (long) config.minTime() * 3600 * 6;
			};
		case ENERGY -> 0;
		case NONE -> 0;
		};
	}

	/**
	 * Calculates the start time of force-heating.
	 *
	 * @param endTime the configured endTime
	 * @return the time or null, if the minimum has already been reached
	 */
	private LocalTime calculateLatestForceHeatingStartTime(LocalTime endTime) throws OpenemsException {
		var totalPhaseTime = this.getTotalHeatingTime();
		var remainingTotalPhaseTime = this.minimumTotalPhaseTime - totalPhaseTime; // [s]

		// Minimum already reached
		if (remainingTotalPhaseTime <= 0) {
			return null;
		}
		switch (this.config.defaultLevel()) {
		case LEVEL_0, LEVEL_1 -> {
			// keep value
		}
		case LEVEL_2 -> remainingTotalPhaseTime /= 2;
		case LEVEL_3 -> remainingTotalPhaseTime /= 3;
		case LEVEL_4 -> remainingTotalPhaseTime /= 4;
		case LEVEL_5 -> remainingTotalPhaseTime /= 5;
		case LEVEL_6 -> remainingTotalPhaseTime /= 6;
		}
		return endTime.minusSeconds(remainingTotalPhaseTime);
	}

	/**
	 * Switch on Phases according to selected {@link Level}.
	 *
	 * @param level the target Level
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	public void applyLevel(Level level) throws IllegalArgumentException, OpenemsNamedException {
		level = Level.fromValue(Math.min(level.getValue(), this.steps.size()));
		setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL, level);
		this.hysteresis.setCurrentLevel(level);

		var stepsOn = level.getValue();
		var phaseDefs = List.of(this.phases.phase1(), this.phases.phase2(), this.phases.phase3());
		for (var i = 0; i < this.steps.size(); i++) {
			this.applyStep(i, i < stepsOn, phaseDefs);
		}
		for (var i = 0; i < this.stepDurations.size(); i++) {
			this.stepDurations.get(i).update(i < stepsOn, this.componentManager.getClock());
		}
	}

	private int getStepTime(int index) {
		var duration = this.stepDurations.get(index);
		return (int) duration.getTotalDuration(this.componentManager.getClock()).getSeconds();
	}

	private long getTotalHeatingTime() {
		if (this.usePhaseBasicLogic) {
			return this.phases.phase1().getTotalDuration().getSeconds()
					+ this.phases.phase2().getTotalDuration().getSeconds()
					+ this.phases.phase3().getTotalDuration().getSeconds();
		}
		long total = 0;
		for (var i = 0; i < this.stepDurations.size(); i++) {
			total += this.getStepTime(i);
		}
		return total;
	}

	private void applyStep(int index, boolean on, List<PhaseDef> phaseDefs) throws OpenemsNamedException {
		if (this.usePhaseBasicLogic && index < phaseDefs.size()) {
			var phaseDef = phaseDefs.get(index);
			if (on) {
				phaseDef.switchOn();
			} else {
				phaseDef.switchOff();
			}
			return;
		}
		var step = this.steps.get(index);
		this.setOutput(step.outputChannel(), on);
	}

	/**
	 * Calculates the nominal power for a given {@link Level}, i.e. the sum of the
	 * nominal power of the first N steps, where N is the value of the Level.
	 *
	 * @param level the {@link Level}
	 * @return the nominal power in [W]
	 */
	protected int getNominalLevelPower(Level level) {
		return this.steps.stream() //
				.limit(level.getValue()) //
				.mapToInt(HeatingElementStep::nominalPower) //
				.sum();
	}

	/**
	 * Applies SURPLUS switching between the current and target {@link Level}, using
	 * symmetric switching for {@link WorkMode#NONE} and retaining immediate
	 * downward changes for the other work modes.
	 *
	 * @param targetLevel the target {@link Level}
	 * @return the Level that should actually be applied
	 */
	private Level applyHysteresis(Level targetLevel) {
		var now = LocalDateTime.now(this.componentManager.getClock());
		var minimumSwitchingTime = Duration.ofSeconds(this.config.minimumSwitchingTime());
		var result = this.hysteresis.apply(targetLevel, now, minimumSwitchingTime, this.steps.size(),
				this.config.workMode() == WorkMode.NONE);

		setValue(this, ControllerIoHeatingElement.ChannelId.AWAITING_HYSTERESIS, result.awaitingHysteresis());
		if (result.awaitingHysteresis()) {
			this.runState = this.lastRunState;
		}
		return result.level();
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param phase {@link SinglePhase}
	 * @param value The boolean value which must set on the output channel address.
	 * @throws OpenemsNamedException    on error.
	 * @throws IllegalArgumentException on error.
	 */
	protected void setOutput(SinglePhase phase, boolean value) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(this.getChannelAddressForPhase(phase), value);
	}

	/**
	 * Generic helper function to switch an output if it was not switched before.
	 * Used by {@link PhaseDef} (via {@link #setOutput(SinglePhase, boolean)}) as
	 * well as by {@link HeatingElementStep}.
	 *
	 * @param channelAddress the {@link ChannelAddress} of the output channel
	 * @param value          The boolean value which must set on the output channel
	 *                       address.
	 * @throws OpenemsNamedException    on error.
	 * @throws IllegalArgumentException on error.
	 */
	protected void setOutput(ChannelAddress channelAddress, boolean value)
			throws IllegalArgumentException, OpenemsNamedException {
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(channelAddress);
		var currentValueOpt = outputChannel.getNextWriteValue().or(() -> outputChannel.value().asOptional());
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + value + ".");
			outputChannel.setNextWriteValue(value);
		}
	}

	/**
	 * Gets the Output ChannelAddress for a given Phase.
	 *
	 * @param phase the {@link SinglePhase}
	 * @return the Output ChannelAddress
	 * @throws OpenemsNamedException on error
	 */
	private ChannelAddress getChannelAddressForPhase(SinglePhase phase) throws OpenemsNamedException {
		return ChannelAddress.fromString(//
				switch (phase) {
				case L1 -> this.config.outputChannelPhaseL1();
				case L2 -> this.config.outputChannelPhaseL2();
				case L3 -> this.config.outputChannelPhaseL3();
				});
	}

	/**
	 * Update the total time of the level depending on the current level.
	 */
	private void updateCumulatedActiveTime() {
		var currentLevel = this.hysteresis.getCurrentLevel();
		this.cumulatedActiveTimes.totalTimeLevel1().update(currentLevel == Level.LEVEL_1);
		this.cumulatedActiveTimes.totalTimeLevel2().update(currentLevel == Level.LEVEL_2);
		this.cumulatedActiveTimes.totalTimeLevel3().update(currentLevel == Level.LEVEL_3);
		this.cumulatedActiveTimes.totalTimeLevel4().update(currentLevel == Level.LEVEL_4);
		this.cumulatedActiveTimes.totalTimeLevel5().update(currentLevel == Level.LEVEL_5);
		this.cumulatedActiveTimes.totalTimeLevel6().update(currentLevel == Level.LEVEL_6);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
