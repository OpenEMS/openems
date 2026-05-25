package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Phase;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.sum.Sum;

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "Controller.IO.HeatingElement", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerIoHeatingElementwithTempImpl extends AbstractOpenemsComponent
        implements ControllerIoHeatingElementwithTemp, Controller, OpenemsComponent, TimedataProvider {

    private final Logger log = LoggerFactory.getLogger(ControllerIoHeatingElementwithTempImpl.class);
    private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
            ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

    /*
     * Definitions for each phase.
     */
    private final PhaseDef phase1;
    private final PhaseDef phase2;
    private final PhaseDef phase3;

    /*
     * Cumulated active time for each level.
     */
    private final CalculateActiveTime totalTimeLevel1 = new CalculateActiveTime(this,
            ControllerIoHeatingElementwithTemp.ChannelId.LEVEL1_CUMULATED_TIME);
    private final CalculateActiveTime totalTimeLevel2 = new CalculateActiveTime(this,
            ControllerIoHeatingElementwithTemp.ChannelId.LEVEL2_CUMULATED_TIME);
    private final CalculateActiveTime totalTimeLevel3 = new CalculateActiveTime(this,
            ControllerIoHeatingElementwithTemp.ChannelId.LEVEL3_CUMULATED_TIME);
    
    @Reference
    private ConfigurationAdmin cm;

    @Reference(
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.OPTIONAL
    )
    private volatile Thermometer waterThermometer = null;
    @Reference
    protected ComponentManager componentManager;

    @Reference
    private Sum sum;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile Timedata timedata = null;

    /** Holds the minimum time the phases should be switch on in [Ws]. */
    private long minimumTotalPhaseTime;
    /** Current Level. */
    private Level currentLevel = Level.LEVEL_0;
    /** Last Level change time, used for the hysteresis. */
    private LocalDateTime lastLevelChange = LocalDateTime.MIN;
    private LocalDateTime lastLegionellenCheck = LocalDateTime.MIN;
    private int legionellenCountdownHours = 0;
    private String configurationFault = null;
    private boolean sensorErrorLogged = false;
    private Config config;

    public ControllerIoHeatingElementwithTempImpl() throws OpenemsNamedException {
        super(//
                OpenemsComponent.ChannelId.values(), //
                Controller.ChannelId.values(), //
                ElectricityMeter.ChannelId.values(), //
                ControllerIoHeatingElementwithTemp.ChannelId.values() //
        );
        this.phase1 = new PhaseDef(this, Phase.L1);
        this.phase2 = new PhaseDef(this, Phase.L2);
        this.phase3 = new PhaseDef(this, Phase.L3);
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.updateConfig(config);
    }

    @Modified
    private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.updateConfig(config);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    private void updateConfig(Config config) {
        this.config = config;
        this.configurationFault = validateConfig(config);
        this.minimumTotalPhaseTime = calculateMinimumTotalPhaseTime(config);
        this.sensorErrorLogged = false;

        if (config.hasWaterThermometer()) {
            OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "waterThermometer",
                    config.waterThermometer_id());
        }

        if (this.configurationFault != null) {
            this.log.warn("Invalid HeatingElement configuration: " + this.configurationFault);
            return;
        }

        if (!config.hasWaterThermometer() || !config.enableLegionellenProtection()) {
            this.legionellenCountdownHours = 0;
            this.lastLegionellenCheck = LocalDateTime.MIN;
            return;
        }
        if (this.legionellenCountdownHours == 0) {
            this.legionellenCountdownHours = config.legionellenInterval() * 24; // Convert days to hours
            this.lastLegionellenCheck = LocalDateTime.now(this.componentManager.getClock()); // Set initial check time
            log.info("Initialized legionella countdown to " + this.legionellenCountdownHours + " hours (" + config.legionellenInterval() + " days)");
        }
    }

    @Override
    public void run() throws OpenemsNamedException {
        if (this.configurationFault != null) {
            this._setWaterActual(null);
            this._setWaterTarget(null);
            this.applyLevel(Level.LEVEL_0);
            this.channel(ControllerIoHeatingElementwithTemp.ChannelId.STATUS).setNextValue(Status.INACTIVE);
            return;
        }

        // Handle Mode AUTOMATIC, MANUAL_OFF or MANUAL_ON
        var runState = switch (this.config.mode()) {
        case AUTOMATIC //
            -> this.modeAutomatic();
        case MANUAL_OFF -> this.modeManualOff();
        case MANUAL_ON -> this.modeManualOn();
        };

        // Calculate Phase Time
        var phase1Time = (int) this.phase1.getTotalDuration().getSeconds();
        var phase2Time = (int) this.phase2.getTotalDuration().getSeconds();
        var phase3Time = (int) this.phase3.getTotalDuration().getSeconds();
        var totalPhaseTime = phase1Time + phase2Time + phase3Time;

        // Update Channels
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.STATUS).setNextValue(runState);
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.PHASE1_TIME).setNextValue(phase1Time);
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.PHASE2_TIME).setNextValue(phase2Time);
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.PHASE3_TIME).setNextValue(phase3Time);
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.TOTAL_PHASE_TIME).setNextValue(totalPhaseTime);

        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.LEVEL1_TIME).setNextValue(phase1Time - phase2Time);
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.LEVEL2_TIME).setNextValue(phase2Time - phase3Time);
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.LEVEL3_TIME).setNextValue(phase3Time);

        this.updateCumulatedActiveTime();

    }

    private Optional<Integer> getCurrentTemperature() throws OpenemsException {
        if (this.waterThermometer == null) {
            this.log.warn("Water thermometer reference is not bound. Configured waterThermometer_id=["
                    + this.config.waterThermometer_id() + "].");
            return Optional.empty();
        }
        
        Optional<?> tempValueOpt = this.waterThermometer.channel(Thermometer.ChannelId.TEMPERATURE).value().asOptional();
        if (tempValueOpt.isPresent()) {
            Object tempValue = tempValueOpt.get();
            if (tempValue instanceof Integer) {
                return Optional.of((Integer) tempValue);
            } else if (tempValue instanceof Number) {
                return Optional.of(((Number) tempValue).intValue());
            } else {
                throw new OpenemsException("Temperature value is not a valid number: " + tempValue.getClass().getSimpleName());
            }
        } else {
            return Optional.empty();
        }
    }
    
    private Status modeManualOn() throws IllegalArgumentException, OpenemsNamedException {
        if (!this.config.hasWaterThermometer()) {
            this._setWaterActual(null);
            this._setWaterTarget(null);
            this.applyLevel(this.config.defaultLevel());
            return this.config.defaultLevel().equals(Level.LEVEL_0) ? Status.INACTIVE : Status.ACTIVE;
        }

        var currentTemperatureOpt = this.readCurrentTemperatureOrFallback();
        if (currentTemperatureOpt.isEmpty()) {
            var sensorErrorLevel = this.config.sensorErrorLevel();
            this.applyLevel(sensorErrorLevel);
            return sensorErrorLevel.equals(Level.LEVEL_0) ? Status.INACTIVE : Status.ACTIVE;
        }

        int currentTemperature = currentTemperatureOpt.get();
        this._setWaterActual(toDegreesCelsius(currentTemperature));
        this._setWaterTarget(this.config.highWaterTemperature());
        this.log.info("Current water temperature: " + toDegreesCelsius(currentTemperature) + "°C.");

        if (currentTemperature >= this.maxWaterTemperature()
                || currentTemperature >= this.highWaterTemperature()) {
            this.applyLevel(Level.LEVEL_0);
            this.logTemperatureLimit("Manual mode", currentTemperature);
            return Status.INACTIVE;
        }

        this.applyLevel(this.config.defaultLevel());
        return this.config.defaultLevel().equals(Level.LEVEL_0) ? Status.INACTIVE : Status.ACTIVE;
    }

    private Status modeManualOff() throws IllegalArgumentException, OpenemsNamedException {
        if (this.config.hasWaterThermometer()) {
            var currentTemperatureOpt = this.readCurrentTemperatureOrFallback();
            this._setWaterActual(currentTemperatureOpt
                    .map(ControllerIoHeatingElementwithTempImpl::toDegreesCelsius)
                    .orElse(null));
            this._setWaterTarget(this.config.highWaterTemperature());
            currentTemperatureOpt.ifPresent(currentTemperature ->
                    this.log.info("Current water temperature: " + toDegreesCelsius(currentTemperature) + "°C."));
        } else {
            this._setWaterActual(null);
            this._setWaterTarget(null);
        }
        this.applyLevel(Level.LEVEL_0);
        return Status.INACTIVE;
    }

    protected Status modeAutomatic() throws IllegalArgumentException, OpenemsNamedException {
        // Get the input channel addresses
        IntegerReadChannel gridActivePowerChannel = this.sum.channel(Sum.ChannelId.GRID_ACTIVE_POWER);
        int gridActivePower = gridActivePowerChannel.value().getOrError();
        IntegerReadChannel essDischargePowerChannel = this.sum.getEssDischargePowerChannel();

        int essDischargePower = essDischargePowerChannel.value().orElse(0 /* if there is no storage */);
        if (essDischargePower < 0) { // we are only interested in discharging, not charging
            essDischargePower = 0;
        }

        long excessPower;
        if (gridActivePower > 0) {
            excessPower = 0;
        } else {
            excessPower = gridActivePower * -1 - essDischargePower
                    + this.currentLevel.getValue() * this.config.powerPerPhase();
        }

        Level targetLevel = Level.LEVEL_0;

        if (!this.config.hasWaterThermometer()) {
            this._setWaterActual(null);
            this._setWaterTarget(null);
            targetLevel = this.calculateTargetLevelFromExcessPower(excessPower);
            this.log.debug("No water thermometer configured, using surplus power control only.");

        } else {
            // Add temperature control logic
            var currentTemperatureOpt = this.readCurrentTemperatureOrFallback();
            if (currentTemperatureOpt.isEmpty()) {
                targetLevel = this.config.sensorErrorLevel();
                this.applyLevel(targetLevel);
                return targetLevel.equals(Level.LEVEL_0) ? Status.INACTIVE : Status.ACTIVE;
            }

            int currentTemperature = currentTemperatureOpt.get();
            this._setWaterActual(toDegreesCelsius(currentTemperature));
            this._setWaterTarget(this.config.highWaterTemperature());
            this.logWaterStatus(currentTemperature);

            if (currentTemperature >= this.maxWaterTemperature()) {
                this.applyLevel(Level.LEVEL_0);
                this.log.warn("Temperature safety limit reached: current="
                        + toDegreesCelsius(currentTemperature) + "°C, max="
                        + this.config.maxWaterTemperature() + "°C. Turning off heating.");
                return Status.INACTIVE;
            }

            if (this.config.enableLegionellenProtection()) {
                this.updateLegionellenCountdown(currentTemperature);
                this.logLegionellenStatus();
                if (this.legionellenCountdownHours <= 0) {
                    this._setWaterTarget(this.config.legionellenWaterTemperature());
                    targetLevel = Level.LEVEL_3;
                    this.log.debug("Legionellen countdown reached zero, forcing heating to reach " +
                            this.config.legionellenWaterTemperature() + "°C.");
                }
            }

            if (targetLevel.equals(Level.LEVEL_0) && currentTemperature >= this.highWaterTemperature()) {
                this.applyLevel(Level.LEVEL_0);
                this.log.info("High water temperature limit reached: current="
                        + toDegreesCelsius(currentTemperature) + "°C, high="
                        + this.config.highWaterTemperature() + "°C. Turning off heating.");
                return Status.INACTIVE;
            }

            if (targetLevel.equals(Level.LEVEL_0) && currentTemperature < this.lowWaterTemperature()) {
                targetLevel = Level.LEVEL_3;
                this.log.debug("Temperature is below low water temperature, forcing level 3 heating.");
            }

            // Temperature is in normal range - use surplus power control
            if (targetLevel.equals(Level.LEVEL_0)
                    && currentTemperature >= this.lowWaterTemperature() &&
                    currentTemperature < this.highWaterTemperature()) {
                this.log.debug("Temperature is in normal range, using surplus power control.");
                targetLevel = this.calculateTargetLevelFromExcessPower(excessPower);
                this.log.debug("Calculated target level based on excess power (" + excessPower + "W): " + targetLevel);
            }
        }

        var now = LocalTime.now(this.componentManager.getClock());
        var configuredEndTime = DateUtils.parseLocalTimeOrError(this.config.endTime());
        var latestForceChargeStartTime = this.calculateLatestForceHeatingStartTime();
        var forceActive = false;

        // Force heat is active if the minimum time for the configured mode is not
        // reached and no time left to heat automatically
        if (this.config.workMode().equals(WorkMode.TIME)) {
            if (now.isAfter(configuredEndTime) || latestForceChargeStartTime == null) {
                this.channel(ControllerIoHeatingElementwithTemp.ChannelId.FORCE_START_AT_SECONDS_OF_DAY).setNextValue(null);
            } else {
                // Force-heat with configured default level or higher
                if (now.isAfter(latestForceChargeStartTime)
                        && targetLevel.getValue() <= this.config.defaultLevel().getValue()) {
                    targetLevel = this.config.defaultLevel();
                    forceActive = true;
                }

                this.channel(ControllerIoHeatingElementwithTemp.ChannelId.FORCE_START_AT_SECONDS_OF_DAY)
                        .setNextValue(latestForceChargeStartTime.toSecondOfDay());
            }
        }

        // Apply hysteresis after all target-level decisions, including force heating.
        targetLevel = this.applyHysteresis(targetLevel);

        Status runState = targetLevel.equals(Level.LEVEL_0)
                ? Status.INACTIVE
                : forceActive
                        ? Status.ACTIVE_FORCED
                        : Status.ACTIVE;

        // Apply Level
        this.applyLevel(targetLevel);
        this.log.debug("Final target level applied: " + targetLevel);
        return runState;
    }

    private static String validateConfig(Config config) {
        if (config.hasWaterThermometer()) {
            if (config.lowWaterTemperature() >= config.highWaterTemperature()) {
                return "Low water temperature must be below high water temperature.";
            }
            if (config.highWaterTemperature() > config.maxWaterTemperature()) {
                return "High water temperature must not be above maximum water temperature.";
            }
            if (config.enableLegionellenProtection()) {
                if (config.legionellenInterval() <= 0) {
                    return "Legionellen interval must be greater than zero if legionellen protection is enabled.";
                }
                if (config.legionellenWaterTemperature() > config.maxWaterTemperature()) {
                    return "Legionellen water temperature must not be above maximum water temperature.";
                }
            }
        }
        return null;
    }

    private int lowWaterTemperature() {
        return toDeciCelsius(this.config.lowWaterTemperature());
    }

    private int highWaterTemperature() {
        return toDeciCelsius(this.config.highWaterTemperature());
    }

    private int maxWaterTemperature() {
        return toDeciCelsius(this.config.maxWaterTemperature());
    }

    private int legionellenWaterTemperature() {
        return toDeciCelsius(this.config.legionellenWaterTemperature());
    }

    private static int toDeciCelsius(int degreesCelsius) {
        return degreesCelsius * 10;
    }

    private static int toDegreesCelsius(int deciCelsius) {
        return Math.round(deciCelsius / 10F);
    }

    private void logWaterStatus(int currentTemperature) {
        this.log.info("Current water temperature: " + toDegreesCelsius(currentTemperature) + "°C.");
    }

    private void logLegionellenStatus() {
        if (!this.config.enableLegionellenProtection()) {
            this.log.info("Legionellen protection is disabled.");
            return;
        }

        int remainingDays = this.legionellenCountdownHours / 24;
        int remainingHoursInDay = this.legionellenCountdownHours % 24;
        this.log.info("Legionellen countdown: " + this.legionellenCountdownHours
                + " hours remaining (" + remainingDays + " days, "
                + remainingHoursInDay + " hours).");
    }

    private void logTemperatureLimit(String source, int currentTemperature) {
        if (currentTemperature >= this.maxWaterTemperature()) {
            this.log.warn(source + ": temperature safety limit reached: current="
                    + toDegreesCelsius(currentTemperature) + "°C, max="
                    + this.config.maxWaterTemperature() + "°C. Turning off heating.");
        } else {
            this.log.info(source + ": high water temperature limit reached: current="
                    + toDegreesCelsius(currentTemperature) + "°C, high="
                    + this.config.highWaterTemperature() + "°C. Turning off heating.");
        }
    }

    private Optional<Integer> readCurrentTemperatureOrFallback() throws OpenemsNamedException {
        try {
            var currentTemperatureOpt = this.getCurrentTemperature();
            if (currentTemperatureOpt.isPresent()) {
                this.sensorErrorLogged = false;
                return currentTemperatureOpt;
            }
        } catch (OpenemsException e) {
            if (!this.sensorErrorLogged) {
                this.log.warn("Failed to read water temperature. Using sensor error fallback level ["
                        + this.config.sensorErrorLevel() + "].", e);
                this.sensorErrorLogged = true;
            }
            this._setWaterActual(null);
            this._setWaterTarget(this.config.highWaterTemperature());
            return Optional.empty();
        }

        if (!this.sensorErrorLogged) {
            this.log.warn("Water temperature is not available. Using sensor error fallback level ["
                    + this.config.sensorErrorLevel() + "].");
            this.sensorErrorLogged = true;
        }
        this._setWaterActual(null);
        this._setWaterTarget(this.config.highWaterTemperature());
        return Optional.empty();
    }

    private void updateLegionellenCountdown(int currentTemperature) {
        if (currentTemperature >= this.legionellenWaterTemperature()) {
            this.lastLegionellenCheck = LocalDateTime.now(this.componentManager.getClock());
            this.legionellenCountdownHours = this.config.legionellenInterval() * 24;
            this.log.debug("Temperature reached legionellen threshold (" + this.config.legionellenWaterTemperature()
                    + "°C), resetting legionellen countdown timer to " + this.legionellenCountdownHours
                    + " hours (" + this.config.legionellenInterval() + " days).");
            return;
        }

        long hoursSinceLastCheck = ChronoUnit.HOURS.between(
                this.lastLegionellenCheck,
                LocalDateTime.now(this.componentManager.getClock()));
        if (hoursSinceLastCheck >= 1) {
            this.legionellenCountdownHours =
                    Math.max(0, this.legionellenCountdownHours - (int) hoursSinceLastCheck);
            this.lastLegionellenCheck = LocalDateTime.now(this.componentManager.getClock());
            int remainingDays = this.legionellenCountdownHours / 24;
            int remainingHoursInDay = this.legionellenCountdownHours % 24;
            this.log.debug("Legionella countdown: " + this.legionellenCountdownHours
                    + " hours remaining (" + remainingDays + " days, " + remainingHoursInDay + " hours).");
        }
    }

    private Level calculateTargetLevelFromExcessPower(long excessPower) {
        if (excessPower >= this.config.powerPerPhase() * 3) {
            return Level.LEVEL_3;
        } else if (excessPower >= this.config.powerPerPhase() * 2) {
            return Level.LEVEL_2;
        } else if (excessPower >= this.config.powerPerPhase()) {
            return Level.LEVEL_1;
        } else {
            return Level.LEVEL_0;
        }
    }

    private static long calculateMinimumTotalPhaseTime(Config config) {
        return switch (config.workMode()) {
        case TIME //
            -> switch (config.defaultLevel()) {
            case LEVEL_0 -> 0;
            case LEVEL_1 -> config.minTime() * 3600;
            case LEVEL_2 -> config.minTime() * 3600 * 2;
            case LEVEL_3 -> config.minTime() * 3600 * 3;
            };
        case NONE -> 0;
        };
    }

    private LocalTime calculateLatestForceHeatingStartTime() throws OpenemsException {
        var totalPhaseTime = this.phase1.getTotalDuration().getSeconds() //
                + this.phase2.getTotalDuration().getSeconds() //
                + this.phase3.getTotalDuration().getSeconds(); // [s]
        var remainingTotalPhaseTime = this.minimumTotalPhaseTime - totalPhaseTime; // [s]

        // Minimum already reached
        if (remainingTotalPhaseTime <= 0) {
            return null;
        }
        var endTime = DateUtils.parseLocalTimeOrError(this.config.endTime());
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

    public void applyLevel(Level level) throws IllegalArgumentException, OpenemsNamedException {
        // Update Channel
        this.channel(ControllerIoHeatingElementwithTemp.ChannelId.LEVEL).setNextValue(level);
        this.currentLevel = level;

        // Set phases accordingly
        switch (level) {
        case LEVEL_0 -> {
            this.phase1.switchOff();
            this.phase2.switchOff();
            this.phase3.switchOff();
        }
        case LEVEL_1 -> {
            this.phase1.switchOn();
            this.phase2.switchOff();
            this.phase3.switchOff();
        }
        case LEVEL_2 -> {
            this.phase1.switchOn();
            this.phase2.switchOn();
            this.phase3.switchOff();
        }
        case LEVEL_3 -> {
            this.phase1.switchOn();
            this.phase2.switchOn();
            this.phase3.switchOn();
        }
        }
    }

    private Level applyHysteresis(Level targetLevel) {
        if (this.currentLevel != targetLevel) {
            var now = LocalDateTime.now(this.componentManager.getClock());
            var hysteresis = Duration.ofSeconds(this.config.minimumSwitchingTime());
            if (this.lastLevelChange.plus(hysteresis).isBefore(now)) {
                // no hysteresis applied
                this.currentLevel = targetLevel;
                this.lastLevelChange = now;
                this.channel(ControllerIoHeatingElementwithTemp.ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
            } else {
                // wait for hysteresis
                this.channel(ControllerIoHeatingElementwithTemp.ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
            }
        } else {
            // Level was not changed
            this.channel(ControllerIoHeatingElementwithTemp.ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
        }
        return this.currentLevel;
    }

    protected int getPowerPerPhase() {
        return this.config.powerPerPhase();
    }

    protected void setOutput(Phase phase, boolean value) throws IllegalArgumentException, OpenemsNamedException {
        var channelAddress = this.getChannelAddressForPhase(phase);
        WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(channelAddress);
        var currentValueOpt = outputChannel.value().asOptional();
        if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
            this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + value + ".");
            outputChannel.setNextWriteValue(value);
        }
    }

    private ChannelAddress getChannelAddressForPhase(Phase phase) throws OpenemsNamedException {
        return ChannelAddress.fromString(//
                switch (phase) {
                case L1 -> this.config.outputChannelPhaseL1();
                case L2 -> this.config.outputChannelPhaseL2();
                case L3 -> this.config.outputChannelPhaseL3();
                });
    }

    private void updateCumulatedActiveTime() {
        var level1Active = false;
        var level2Active = false;
        var level3Active = false;

        switch (this.currentLevel) {
        case LEVEL_0:
            break;
        case LEVEL_1:
            level1Active = true;
            break;
        case LEVEL_2:
            level2Active = true;
            break;
        case LEVEL_3:
            level3Active = true;
            break;
        }

        this.totalTimeLevel1.update(level1Active);
        this.totalTimeLevel2.update(level2Active);
        this.totalTimeLevel3.update(level3Active);
    }

    @Override
    public MeterType getMeterType() {
        return MeterType.CONSUMPTION_METERED;
    }

    @Override
    public Timedata getTimedata() {
        return this.timedata;
    }
}
