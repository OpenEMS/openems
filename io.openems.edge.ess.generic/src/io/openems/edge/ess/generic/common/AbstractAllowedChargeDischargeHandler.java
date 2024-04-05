package io.openems.edge.ess.generic.common;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.symmetric.ChannelManager;

/**
 * Helper class to handle calculation of Allowed-Charge-Power and
 * Allowed-Discharge-Power. This class is used by {@link ChannelManager} as a
 * callback to updates of Battery Channels.
 */
public abstract class AbstractAllowedChargeDischargeHandler<ESS extends SymmetricEss>
		implements BiConsumer<ClockProvider, Battery> {

	public static final float DISCHARGE_EFFICIENCY_FACTOR = 0.95F;

	/**
	 * Allow a maximum increase per second.
	 *
	 * <p>
	 * 5 % of possible allowed charge/discharge power
	 */
	public static final float MAX_INCREASE_PERCENTAGE = 0.05F;

	protected final ESS parent;

	public AbstractAllowedChargeDischargeHandler(ESS parent) {
		this.parent = parent;
	}

	protected float lastBatteryAllowedChargePower;
	protected float lastBatteryAllowedDischargePower;
	private Instant lastCalculate = null;

	@Override
	public abstract void accept(ClockProvider clockProvider, Battery battery);

	/**
	 * Calculates Allowed-Charge-Power and Allowed-Discharge Power from the given
	 * parameters. Result is stored in 'lastBatteryAllowedChargePower' and
	 * 'lastBatteryAllowedDischargePower' variables - both as positive values!
	 *
	 * @param clockProvider the {@link ClockProvider}
	 * @param battery       the {@link Battery}
	 */
	protected void calculateAllowedChargeDischargePower(ClockProvider clockProvider, Battery battery) {
		var chargeMaxCurrent = battery.getChargeMaxCurrentChannel().getNextValue().get();
		var dischargeMaxCurrent = battery.getDischargeMaxCurrentChannel().getNextValue().get();
		var voltage = battery.getVoltageChannel().getNextValue().get();

		// Is the ESS started?
		final boolean isStarted;
		if (this.parent instanceof StartStoppable) {
			isStarted = ((StartStoppable) this.parent).isStarted();
		} else {
			isStarted = true;
		}

		this.calculateAllowedChargeDischargePower(clockProvider, isStarted, chargeMaxCurrent, dischargeMaxCurrent,
				voltage);
	}

	/**
	 * Calculates Allowed-Charge-Power and Allowed-Discharge Power from the given
	 * parameters. Result is stored in 'allowedChargePower' and
	 * 'allowedDischargePower' variables - both as positive values!
	 *
	 * @param clockProvider       the {@link ClockProvider}
	 * @param isStarted           is the ESS started?
	 * @param chargeMaxCurrent    the {@link Battery.ChannelId#CHARGE_MAX_CURRENT}
	 * @param dischargeMaxCurrent the {@link Battery.ChannelId#DISHARGE_MAX_CURRENT}
	 * @param voltage             the {@link Battery.ChannelId#VOLTAGE}
	 */
	protected void calculateAllowedChargeDischargePower(ClockProvider clockProvider, boolean isStarted,
			Integer chargeMaxCurrent, Integer dischargeMaxCurrent, Integer voltage) {
		final var now = Instant.now(clockProvider.getClock());
		float charge;
		float discharge;

		/*
		 * Calculate initial AllowedChargePower and AllowedDischargePower
		 */
		if (!isStarted || chargeMaxCurrent == null || dischargeMaxCurrent == null || voltage == null) {
			// Block ACTIVE and REACTIVE Power if
			// - GenericEss is not in State "STARTED"
			// - any of CHARGE_MAX_CURRENT, DISHARGE_MAX_CURRENT or VOLTAGE are missing
			charge = 0;
			discharge = 0;

		} else {
			// Calculate AllowedChargePower and AllowedDischargePower from battery current
			// limits and voltage.
			// Efficiency factor is not considered in chargeMaxCurrent (DC Power > AC Power)
			charge = chargeMaxCurrent * voltage;
			discharge = Math.round(dischargeMaxCurrent * voltage * DISCHARGE_EFFICIENCY_FACTOR);
		}

		/*
		 * Handle Force Charge and Discharge
		 */
		if (charge < 0 && discharge < 0) {
			// Both Force Charge and Discharge are active -> cannot do anything
			charge = 0;
			discharge = 0;

		} else if (discharge < 0) {
			// Force Charge is active
			// Make sure AllowedChargePower is greater-or-equals absolute
			// AllowedDischargePower
			charge = Math.max(charge, Math.abs(discharge));

		} else if (charge < 0) {
			// Force Discharge is active
			// Make sure AllowedDischargePower is greater-or-equals absolute
			// AllowedChargePower
			discharge = Math.max(Math.abs(charge), discharge);
		}

		/*
		 * In Non-Force Mode: apply the max increase ramp.
		 */
		if (charge > 0) {
			charge = applyMaxIncrease(this.lastBatteryAllowedChargePower, charge, this.lastCalculate, now);
		}
		if (discharge > 0) {
			discharge = applyMaxIncrease(this.lastBatteryAllowedDischargePower, discharge, this.lastCalculate, now);
		}

		/*
		 * Apply result
		 */
		this.lastCalculate = now;
		this.lastBatteryAllowedChargePower = charge;
		this.lastBatteryAllowedDischargePower = discharge;
	}

	/**
	 * Applies the max increase ramp, built from MAX_INCREASE_PERCENTAGE.
	 *
	 * @param lastValue   the result value in [W] of previous run
	 * @param thisValue   the current value [W]
	 * @param lastInstant the timestamp of the previous run
	 * @param thisInstant the current timestamp
	 * @return the new value
	 */
	private static float applyMaxIncrease(float lastValue, float thisValue, Instant lastInstant, Instant thisInstant) {
		final long millis;
		if (lastValue < 0 || lastInstant == null) {
			// was in Force-Mode before
			lastValue = 0;
			millis = 1000;
		} else {
			millis = Duration.between(lastInstant, thisInstant).toMillis();
		}
		return Math.min(thisValue, //
				lastValue + thisValue * millis * MAX_INCREASE_PERCENTAGE / 1000.F /* convert [mW] to [W] */);
	}
}