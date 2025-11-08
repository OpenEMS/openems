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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.osgi.service.cm.ConfigurationAdmin;
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
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
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
public class ControllerIoHeatingElementImpl extends AbstractOpenemsComponent
		implements ControllerIoHeatingElement, Controller, OpenemsComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(ControllerIoHeatingElementImpl.class);

	private static final Duration LAST_PHASE_DURATION_TO_TURN_UP = Duration.ofMinutes(30);
	private static final double LAST_PHASE_PERCENTAGE_TO_TURN_UP = 0.9;
	private static final double PERCENTAGE_ENERGYLIMIT_TO_NOT_TURN_UP = 0.01;
	private static final Duration SHOWING_DONE_FOR_HALF_AN_HOUR = Duration.ofMinutes(30);
	private static final int CALIBRATION_SECONDS = 4;

	/*
	 * Definitions for each phase.
	 */
	private final HeatingPhases phases;

	/*
	 * Cumulated active time for each level.
	 */
	private final CumulatedActiveTimes cumulatedActiveTimes = new CumulatedActiveTimes(
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL1_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL2_CUMULATED_TIME),
			new CalculateActiveTime(this, ControllerIoHeatingElement.ChannelId.LEVEL3_CUMULATED_TIME));

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference(cardinality = OPTIONAL, policyOption = GREEDY)
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
	/** Current Level. */
	private Level currentLevel = Level.LEVEL_0;
	/** Last Level change time, used for the hysteresis. */
	private LocalDateTime lastLevelChange = LocalDateTime.MIN;

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

	private void updateConfig(Config config) throws OpenemsException {
		this.config = config;
		this.minimumTotalPhaseTime = calculateMinimumTotalPhaseTime(config);
		this.schedule = JSCalendar.Tasks.fromStringOrEmpty(this.componentManager.getClock(), config.schedule(),
				Payload.serializer());
		this.resetProps();
		if (this.config.mode() == Mode.AUTOMATIC) {
			this.updateEndTime();
		}
		if (this.config.meter_id() == null || this.config.meter_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilterRaw(this.cm, this.servicePid(), "meter", "(false=true)");
		} else {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id());
		}
	}

	@Override
	public void run() throws OpenemsNamedException {

		// Handle Mode AUTOMATIC, MANUAL_OFF or MANUAL_ON
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
		final var totalPhaseTime = phase1Time + phase2Time + phase3Time;

		// Update Channels
		setValue(this, ControllerIoHeatingElement.ChannelId.STATUS, runState);
		setValue(this, ControllerIoHeatingElement.ChannelId.PHASE1_TIME, phase1Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.PHASE2_TIME, phase2Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.PHASE3_TIME, phase3Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.TOTAL_PHASE_TIME, totalPhaseTime);
		setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL1_TIME, phase1Time - phase2Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL2_TIME, phase2Time - phase3Time);
		setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL3_TIME, phase3Time);

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

		if (waiting && this.isPowerCapturedByMeter()) {
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

		targetLevel = this.runState == Status.ACTIVE_FORCED_LIMIT || this.runState == Status.ACTIVE_FORCED ? targetLevel
				: this.applyHysteresis(targetLevel);

		this.applyLevel(targetLevel);
		this.lastRunState = this.runState;
		return this.runState;
	}

	private Level getLevelFromSchedule(Level targetLevel) throws IllegalArgumentException, OpenemsNamedException {
		var ot = this.schedule.getActiveOneTask();
		var lastOt = this.schedule.getLastActiveOneTask();
		var now = ZonedDateTime.now(this.componentManager.getClock());

		var lastTaskJustFinished = lastOt != null //
				&& now.isAfter(lastOt.end()) //
				&& this.timestampTaskFinished != null //
				&& now.isBefore(this.timestampTaskFinished.plusSeconds(SHOWING_DONE_FOR_HALF_AN_HOUR.getSeconds()));

		/*
		 * show the state done for half an hour, if the Workmode is Energy
		 */
		if (ot == null) {
			if (lastTaskJustFinished && this.config.workMode() == WorkMode.ENERGY) {
				this.runState = Status.DONE;
			} else {
				this.runState = Status.INACTIVE;
			}
			return Level.LEVEL_0;
		}

		if (lastOt != null && now.isAfter(lastOt.end())) {
			this.resetProps();
			this.updateEndTime();
			this.timestampTaskFinished = now;
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
			this.timeToForceHeat = getTimeToForceHeat(this.energyLimit, this.getSessionEnergy().get(), this.phases,
					this.endTimeWorkModeEnergy, now);
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
			return Level.LEVEL_3;
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

		long requiredPower = getRequiredPower(this.energyLimit, this.getSessionEnergy().get(), totalRemainingTime);
		Level requiredLevel = this.getRequiredLevel(requiredPower);

		/*
		 * It should always heat, except if the requiredPower is significant low, for
		 * e.g. if the PV surplus was high during the forced time heating and then
		 * suddenly it will stop. During the last 10 minutes it should always heat.
		 */
		if (requiredLevel == Level.LEVEL_0
				&& (this.isPowerAbovePhase1Threshold(requiredPower) || isFinal10Minutes(totalRemainingTime))) {
			requiredLevel = Level.LEVEL_1;
		}

		double threshold = requiredLevel == Level.LEVEL_1 ? this.phases.phase1().getAvgPower()
				: this.phases.phase1().getAvgPower() + this.phases.phase2().getAvgPower();

		/*
		 * if the last half an hour begins, it checks if the requiredPower is more than
		 * the average power of the required Level. When true it will turn up a level to
		 * reach the minimum limit, also if the rest energy is less than 1% the
		 * configured minimum limit, it will not turn up, so it will not turn up the
		 * level last second.
		 *
		 */
		if (totalRemainingTime.getSeconds() <= LAST_PHASE_DURATION_TO_TURN_UP.getSeconds() //
				&& requiredLevel != Level.LEVEL_3 //
				&& threshold < requiredPower * LAST_PHASE_PERCENTAGE_TO_TURN_UP //
				&& this.energyLimit - this.getSessionEnergy().get() > this.energyLimit
						* PERCENTAGE_ENERGYLIMIT_TO_NOT_TURN_UP) {

			requiredLevel = Level.values()[requiredLevel.ordinal() + 1];
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
	 * Gets the endTime from the config converted to LocalTime.
	 *
	 * @return the endTime in LocalTime format
	 */
	private LocalTime getConvertedEndTime() {
		return LocalTime.parse(this.config.endTimeWithMeter());
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
			power = this.currentLevel.getValue() * this.config.powerPerPhase();
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
		if (this.isPowerCapturedByMeter()) {
			return getRequiredLevelFromAvgPowers(excessPower, this.phases);
		}
		return getRequiredLevelFromPower(excessPower, this.config.powerPerPhase());
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
				|| this.energyLimit <= this.getSessionEnergy().get();
	}

	/**
	 * Checks if the session is unreachable.
	 *
	 * @param totalRemainingTime the total remaining time of the session
	 * @return a boolean value if the session is unreachable
	 */
	private boolean checkIfSessionUnreachable(Duration totalRemainingTime) {
		return Math.ceil((this.energyLimit - this.getSessionEnergy().get()) * 3600F
				/ Math.max(totalRemainingTime.getSeconds(), 1)) > getTotalAvgPower(this.phases) * 1.1;
	}

	/**
	 * Gets the calculated ending time of the mode Energy.
	 *
	 * @return the calculated ending time
	 */
	private LocalTime getCalculatedEndTimeOfWorkModeEnergy() {
		var highPeriod = this.schedule.getLastActiveOneTask();
		if (highPeriod == null) {
			return this.getConvertedEndTime();
		}
		return highPeriod.end().toLocalTime();
	}

	/**
	 * Gets the calculated ending time of the mode Time.
	 *
	 * @return the calculated ending time
	 */
	private LocalTime getCalculatedEndTimeOfWorkModeTime() throws OpenemsException {
		var highPeriod = this.schedule.getLastActiveOneTask();
		if (highPeriod == null) {
			return DateUtils.parseLocalTimeOrError(this.config.endTime());
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
		return requiredPower > this.phases.phase1().getAvgPower() * 0.4;
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
		var totalPhaseTime = this.phases.phase1().getTotalDuration().getSeconds() //
				+ this.phases.phase2().getTotalDuration().getSeconds() //
				+ this.phases.phase3().getTotalDuration().getSeconds(); // [s]
		var remainingTotalPhaseTime = this.minimumTotalPhaseTime - totalPhaseTime; // [s]

		// Minimum already reached
		if (remainingTotalPhaseTime <= 0) {
			return null;
		}
		switch (this.config.defaultLevel()) {
		case LEVEL_0:
		case LEVEL_1:
			// keep value
			break;
		case LEVEL_2:
			remainingTotalPhaseTime /= 2;
			break;
		case LEVEL_3:
			remainingTotalPhaseTime /= 3;
			break;
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
		// Update Channel
		setValue(this, ControllerIoHeatingElement.ChannelId.LEVEL, level);
		this.currentLevel = level;

		// Set phases accordingly
		switch (level) {
		case LEVEL_0 -> {
			this.phases.phase1().switchOff();
			this.phases.phase2().switchOff();
			this.phases.phase3().switchOff();
		}
		case LEVEL_1 -> {
			this.phases.phase1().switchOn();
			this.phases.phase2().switchOff();
			this.phases.phase3().switchOff();
		}
		case LEVEL_2 -> {
			this.phases.phase1().switchOn();
			this.phases.phase2().switchOn();
			this.phases.phase3().switchOff();
		}
		case LEVEL_3 -> {
			this.phases.phase1().switchOn();
			this.phases.phase2().switchOn();
			this.phases.phase3().switchOn();
		}
		}
	}

	/**
	 * Applies the {@link #HYSTERESIS} to avoid too quick changes of Levels.
	 *
	 * @param targetLevel the target {@link Level}
	 * @return boolean value if hysteresis needs to be applied;
	 */
	private Level applyHysteresis(Level targetLevel) {
		if (this.currentLevel != targetLevel) {
			var now = LocalDateTime.now(this.componentManager.getClock());
			var hysteresis = Duration.ofSeconds(this.config.minimumSwitchingTime());
			if (this.lastLevelChange.plus(hysteresis).isBefore(now)) {
				// no hysteresis applied
				this.lastLevelChange = now;
				this.currentLevel = targetLevel;
				setValue(this, ControllerIoHeatingElement.ChannelId.AWAITING_HYSTERESIS, false);

			} else {
				// wait for hysteresis
				setValue(this, ControllerIoHeatingElement.ChannelId.AWAITING_HYSTERESIS, true);
				this.runState = this.lastRunState;
				return this.currentLevel;
			}

		} else {
			// Level was not changed
			setValue(this, ControllerIoHeatingElement.ChannelId.AWAITING_HYSTERESIS, false);
		}
		return targetLevel;
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
		var channelAddress = this.getChannelAddressForPhase(phase);
		WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(channelAddress);
		var currentValueOpt = outputChannel.value().asOptional();
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
		var level1Active = false;
		var level2Active = false;
		var level3Active = false;

		switch (this.currentLevel) {
		case LEVEL_0 -> {
			// do nothing
		}
		case LEVEL_1 -> level1Active = true;
		case LEVEL_2 -> level2Active = true;
		case LEVEL_3 -> level3Active = true;
		}

		this.cumulatedActiveTimes.totalTimeLevel1().update(level1Active);
		this.cumulatedActiveTimes.totalTimeLevel2().update(level2Active);
		this.cumulatedActiveTimes.totalTimeLevel3().update(level3Active);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}