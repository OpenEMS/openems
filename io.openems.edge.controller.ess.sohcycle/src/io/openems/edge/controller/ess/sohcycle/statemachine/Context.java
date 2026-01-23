package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants.C_RATE;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.ess.sohcycle.BatteryBalanceStatus;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.LogVerbosity;
import io.openems.edge.controller.ess.sohcycle.Mode;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Context extends AbstractContext<ControllerEssSohCycleImpl> {

    protected final Config config;
    protected final Clock clock;
    protected final ManagedSymmetricEss ess;
    protected Float targetPower = 0f;
    protected float rampPower;

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    public Context(ControllerEssSohCycleImpl parent, Config config, Clock clock, ManagedSymmetricEss ess) {
        super(parent);
        this.config = config;
        this.clock = clock;
        this.ess = ess;
    }

    public Clock getClock() {
        return this.clock;
    }

    public Config getConfig() {
        return this.config;
    }

    public void setTargetPower(Float targetPower) {
        this.targetPower = targetPower;
    }

    public Float getTargetPower() {
        return this.targetPower;
    }

    public void setRampPower(float rampPower) {
        this.rampPower = rampPower;
    }

    public float getRampPower() {
        return this.rampPower;
    }

    /**
     * Applies the charging target based on the given SoC threshold.
     *
     * @param thresholdSoc the SoC threshold for charging
     * @return {@link ReferenceTargetResult} with SoC, limited power, and
     *         threshold status, or {@code null} if required values are undefined
     */
    public ReferenceTargetResult applyChargingTarget(int thresholdSoc) {
        final var ref = this.getPowerDetails();
        if (ref == null) {
            return null;
        }

        final int soc = ref.soc();
        final float limitedPower = ref.limitedPower();
        final boolean thresholdReached = soc >= thresholdSoc;
        final float signedPower = -limitedPower; // Negative for charging

        this.setTargetPower(thresholdReached ? 0f : signedPower);
        this.setRampPower(ref.rampPower());
        return new ReferenceTargetResult(soc, limitedPower, thresholdReached);
    }

    /**
     * Applies the discharging target based on the given SoC threshold.
     *
     * @param thresholdSoc the SoC threshold for discharging
     * @return {@link ReferenceTargetResult} with SoC, limited power, and
     *         threshold status, or {@code null} if required values are undefined
     */
    public ReferenceTargetResult applyDischargingTarget(int thresholdSoc) {
        final var ref = this.getPowerDetails();
        if (ref == null) {
            return null;
        }

        final int soc = ref.soc();
        final float limitedPower = ref.limitedPower();
        final boolean thresholdReached = soc <= thresholdSoc;

        this.setTargetPower(thresholdReached ? 0f : limitedPower);
        this.setRampPower(ref.rampPower());
        return new ReferenceTargetResult(soc, limitedPower, thresholdReached);
    }

    @Override
    public void logInfo(Logger logger, String message) {
        if (this.config.logVerbosity() != LogVerbosity.DEBUG_LOG) {
            return;
        }
        super.logInfo(logger, message);
    }

	@Override
	public void logDebug(Logger logger, String message) {
		if (this.config.logVerbosity() != LogVerbosity.DEBUG_LOG) {
			return;
		}
		super.logDebug(logger, message);
	}

    /**
     * Logs a standardized charging/discharging message when DEBUG_LOG is enabled.
     * @param logger   destination logger
     * @param state    current state to render in the log
     * @param result   power target calculation result
     * @param targetSoc target SoC threshold
     * @param charging true for charging, false for discharging
     */
    public void logPowerState(Logger logger, StateMachine.State state, ReferenceTargetResult result,
 			int targetSoc, boolean charging) {
 		final String mode = charging ? "charging" : "discharging";
 		this.logInfo(logger, String.format(
 				"%s: SoC=%d%%, target=%d%%, %s with %.1f W (C-rate=%.1f)",
 				state.getName(), result.soc(), targetSoc, mode, result.limitedPower(), C_RATE));
 	}

    /**
     * Calculates the reference cycle target power and ramp power.
     *
     * <p>
     * Sets {@link #targetPower} to {@code null} if any required channel is
     * undefined. Ramp power is always updated while channels are available.
     * @return {@link ReferencePowerResult} with SoC, limited power, and ramp power,
     *         or {@code null} if required values are undefined
     */
    private ReferencePowerResult getPowerDetails() {
        final var socValue = this.ess.getSoc();
        final var maxPowerValue = this.ess.getMaxApparentPower();
        final var capacityValue = this.ess.getCapacity();

        if (!socValue.isDefined() || !maxPowerValue.isDefined() || !capacityValue.isDefined()) {
            return null;
        }

        final int soc = socValue.get();
        final int maxApparentPower = maxPowerValue.get();
        final int capacityWh = capacityValue.get();
        final float capacityBasedPower = capacityWh * C_RATE;
        final float limitedPower = TypeUtils.fitWithin(0, maxApparentPower, capacityBasedPower);

        // Allow changing by 1% of max apparent power per run, similar to
        // EmergencyCapacityReserve.
        return new ReferencePowerResult(soc, limitedPower, maxApparentPower * 0.01f);
    }

    /**
     * Calculates the cell voltage delta (max - min) in millivolts.
     *
     * <p>
     * The minimal cell voltage is determined as the maximum MIN_CELL_VOLTAGE value
     * observed during the measurement charging phase, persisted on the controller.
     * If no persisted value exists, fall back to current MIN_CELL_VOLTAGE.
     *
     * @return cell voltage delta in mV, or {@code null} if values are undefined
     */
    public Integer calculateCellVoltageDelta() {
        final var maxVoltage = this.ess.getMaxCellVoltage();
        if (!maxVoltage.isDefined()) {
            return null;
        }

        final Integer minVoltage = this.getMeasurementChargingMaxMinVoltage();
        if (minVoltage == null) {
            return null;
        }

        final int delta = maxVoltage.get() - minVoltage;
        if (delta < 0) {
            log.warn("Invalid cell voltage delta (max={} mV, min={} mV). Treating as undefined.", maxVoltage.get(), minVoltage);
            return null;
        }
        return delta;
    }

    /**
     * Refresh the stored maximum min-cell-voltage using the current ESS reading.
     * It updates the stored value only if the current min-cell-voltage is higher
     * than the stored one.
     */
    public void refreshMeasurementChargingMinVoltageFromEss() {
        var minVoltage = this.ess.getMinCellVoltage();
        if (!minVoltage.isDefined()) {
            return;
        }
        var current = minVoltage.get();
        var stored = this.getMeasurementChargingMaxMinVoltage();
        if (stored == null || current > stored) {
            this.setMeasurementChargingMaxMinVoltage(current);
        }
    }

    /**
     * Checks if the battery is sufficiently balanced based on cell voltage
     * difference.
     *
     * @param thresholdMv maximum allowed cell voltage difference in millivolts
     * @return {@code true} if battery is balanced (delta ≤ threshold),
     *         {@code false} if not balanced or values are undefined
     */
    public boolean isBatteryBalanced(int thresholdMv) {
        final Integer delta = this.calculateCellVoltageDelta();
        return delta != null && delta <= thresholdMv;
    }

    public Integer getMeasurementChargingMaxMinVoltage() {
        return this.getParent().getMeasurementChargingMinVoltage();
    }

    public void setMeasurementChargingMaxMinVoltage(Integer value) {
        this.getParent().setMeasurementChargingMinVoltage(value);
    }

    /**
     * Resets the stored maximum min-cell-voltage.
     */
    public void resetMeasurementChargingMaxMinVoltage() {
        this.getParent().setMeasurementChargingMinVoltage(null);
    }

    public Long getMeasurementStartEnergyWh() {
        return this.getParent().getMeasurementStartEnergyWh();
    }

    public void setMeasurementStartEnergyWh(Long energyWh) {
        this.getParent().setMeasurementStartEnergyWh(energyWh);
    }

    /**
     * Ensures measurement baseline is set exactly once for the whole measurement cycle.
     * Sets the baseline only if it's currently null. Subsequent calls do nothing.
     * Also handles logging for capture/skip events.
     *
     * @param energyValue current energy channel value used to capture as baseline; may be undefined
     */
    public void ensureMeasurementStartEnergyWh(Value<Long> energyValue) {
        if (energyValue == null || !energyValue.isDefined()) {
            this.logDebug(log,"Measurement baseline not captured: energy undefined at this step");
            return;
        }
        final var existing = this.getParent().getMeasurementStartEnergyWh();
        if (existing == null) {
            final var currentEnergyWh = energyValue.get();
            this.getParent().setMeasurementStartEnergyWh(currentEnergyWh);
            this.logDebug(log,"Measurement baseline captured: %d Wh".formatted(currentEnergyWh));
        } else {
            // Baseline already set; do not overwrite
            this.logDebug(log, "Measurement baseline already set: %d Wh; keeping existing value".formatted(existing));
        }
    }

    /**
     * Resets the measurement baseline to null.
     */
    public void resetMeasurementStartEnergyWh() {
        this.setMeasurementStartEnergyWh(null);
    }

    /**
     * Resets all measurement-related stored values in the controller.
     */
    public void resetController() {
		this.resetMeasurementChargingMaxMinVoltage();
		this.resetMeasurementStartEnergyWh();
        this.resetMeasuredCapacity();
        this.resetVoltageDelta();
        this.resetIsBatteryBalancedChannel();
    }

    private void resetVoltageDelta() {
        ChannelUtils.setValue(this.getParent(), ControllerEssSohCycle.ChannelId.VOLTAGE_DELTA, null);
    }

    private void resetMeasuredCapacity() {
        ChannelUtils.setValue(this.getParent(), ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY, null);
    }

    private void resetIsBatteryBalancedChannel() {
        ChannelUtils.setValue(this.getParent(), ControllerEssSohCycle.ChannelId.IS_BATTERY_BALANCED,
                BatteryBalanceStatus.NOT_MEASURED);
    }

    public boolean isManualModeOff() {
        return this.config.mode().equals(Mode.MANUAL_OFF);
    }

    public void setVoltageDelta(Integer voltageDelta) {
        ChannelUtils.setValue(this.getParent(), ControllerEssSohCycle.ChannelId.VOLTAGE_DELTA, voltageDelta);
    }

    public record ReferenceTargetResult(int soc, float limitedPower, boolean thresholdReached) {
    }

    public record ReferencePowerResult(int soc, float limitedPower, float rampPower) {
    }
}
