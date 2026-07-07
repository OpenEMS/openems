package io.openems.edge.ess.generic.common;

import static io.openems.common.utils.IntUtils.minInteger;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.util.TriConsumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.essprotection.EpRampHandler;
import io.openems.edge.ess.generic.common.essprotection.EpVoltageRegulationHandler;
import io.openems.edge.ess.generic.common.essprotection.EssProtection;
import io.openems.edge.ess.generic.common.essprotection.EssProtection.EssProtectionConfig;
import io.openems.edge.ess.generic.common.essprotection.EssProtectionHandler;
import io.openems.edge.ess.generic.common.essprotection.EssProtectionHandler.EssProtectionLimits;
import io.openems.edge.ess.generic.symmetric.ChannelManager;

/**
 * Helper class to handle calculation of Allowed-Charge-Power and
 * Allowed-Discharge-Power. This class is used by {@link ChannelManager} as a
 * callback to updates of Battery Channels.
 */
public abstract class AbstractAllowedChargeDischargeHandler<ESS extends SymmetricEss>
		implements TriConsumer<ClockProvider, Battery, SymmetricBatteryInverter> {

	public static final float DISCHARGE_EFFICIENCY_FACTOR = 0.95F;

	/**
	 * Allow a maximum increase per second.
	 *
	 * <p>
	 * 5 % of possible allowed charge/discharge power
	 */
	public static final float MAX_INCREASE_PERCENTAGE = 0.05F;

	private static final int ESS_PROTECTION_EXTREME_LIMIT_TIMEOUT = 240; // [seconds]

	protected final ESS parent;

	private final EssProtectionHandler essProtectionHandler;

	protected AbstractAllowedChargeDischargeHandler(ESS parent) {
		this(parent, EssProtectionConfig.NONE);
	}

	protected AbstractAllowedChargeDischargeHandler(ESS parent, EssProtectionConfig essProtectionConfig) {
		this.parent = parent;
		this.essProtectionHandler = switch (essProtectionConfig) {
		case NONE -> null;
		case RAMP -> new EpRampHandler();
		case VOLTAGE_REGULATION -> new EpVoltageRegulationHandler();
		};
	}

	protected float lastBatteryAllowedChargePower;
	protected float lastBatteryAllowedDischargePower;
	private Instant lastCalculate = null;
	private Instant onEntryEssProtection = null;

	private EssProtectionLimits essProtectionLimits = EssProtectionLimits.EMPTY;

	/**
	 * Calculates {@link EssProtection.ChannelId#EP_CHARGE_MAX_CURRENT} and
	 * {@link EssProtection.ChannelId#EP_DISCHARGE_MAX_CURRENT} from the given
	 * parameters. Result is stored in 'essProtectionLimits' variable.
	 * 
	 * @param battery  the {@link Battery}
	 * @param inverter the {@link SymmetricBatteryInverter}
	 */
	public void calculateEssProtectionLimits(Battery battery, SymmetricBatteryInverter inverter) {

		this.essProtectionLimits = this.essProtectionHandler != null //
				? this.essProtectionHandler.calculateEssProtectionLimits(battery, inverter) //
				: EssProtectionLimits.EMPTY;

		if (this.parent instanceof EssProtection ess) {
			ess._setEpChargeMaxCurrent(this.essProtectionLimits.chargeMaxCurrent());
			ess._setEpDischargeMaxCurrent(this.essProtectionLimits.dischargeMaxCurrent());
		}
	}

	/**
	 * Calculates {@link ManagedSymmetricEss.ChannelId#ALLOWED_CHARGE_POWER} and
	 * {@link ManagedSymmetricEss.ChannelId#ALLOWED_DISCHARGE_POWER} from the given
	 * parameters. Result is stored in 'lastBatteryAllowedChargePower' and
	 * 'lastBatteryAllowedDischargePower' variables - both as positive values!
	 *
	 * @param clockProvider the {@link ClockProvider}
	 * @param battery       the {@link Battery}
	 * @param inverter      the {@link SymmetricBatteryInverter}
	 */
	protected void calculateAllowedChargeDischargePower(ClockProvider clockProvider, Battery battery,
			SymmetricBatteryInverter inverter) {
		// From Battery
		final var batteryChargeMaxCurrent = battery.getChargeMaxCurrentChannel().getNextValue().get();
		final var batteryDischargeMaxCurrent = battery.getDischargeMaxCurrentChannel().getNextValue().get();
		// From EssProtection
		final var essChargeMaxCurrent = this.essProtectionLimits.chargeMaxCurrent();
		final var essDischargeMaxCurrent = this.essProtectionLimits.dischargeMaxCurrent();

		final var chargeMaxCurrent = minInteger(batteryChargeMaxCurrent, essChargeMaxCurrent);
		final var dischargeMaxCurrent = minInteger(batteryDischargeMaxCurrent, essDischargeMaxCurrent);

		final var current = battery.getCurrentChannel().value();
		this.checkEssProtectionExtremes(clockProvider, chargeMaxCurrent, dischargeMaxCurrent, current);

		final boolean isStarted = !(this.parent instanceof StartStoppable p) || p.isStarted();
		final var voltage = battery.getVoltageChannel().getNextValue().get();
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
	 * @param dischargeMaxCurrent the
	 *                            {@link Battery.ChannelId#DISCHARGE_MAX_CURRENT}
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
			charge = (float) chargeMaxCurrent * voltage;
			discharge = round(dischargeMaxCurrent * voltage * DISCHARGE_EFFICIENCY_FACTOR);
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
			charge = max(charge, abs(discharge));

		} else if (charge < 0) {
			// Force Discharge is active
			// Make sure AllowedDischargePower is greater-or-equals absolute
			// AllowedChargePower
			discharge = max(abs(charge), discharge);
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

	private void checkEssProtectionExtremes(ClockProvider clockProvider, Integer chargeMaxCurrent,
			Integer dischargeMaxCurrent, Value<Integer> current) {
		if (!(this.parent instanceof EssProtection ess)) {
			return;
		}

		if (dischargeMaxCurrent == null || chargeMaxCurrent == null || !current.isDefined()) {
			return;
		}

		if (dischargeMaxCurrent >= 0 || chargeMaxCurrent >= 0) {
			this.onEntryEssProtection = null;
			ess._setEpDeepDischargeProtection(false);
			ess._setEpOverChargeProtection(false);
			return;
		}

		if (this.onEntryEssProtection == null) {
			this.onEntryEssProtection = Instant.now(clockProvider.getClock());
		}

		if (current.get() >= 0 && this.isExtremeTimeoutPassed()) {
			ess._setEpDeepDischargeProtection(true);
		}

		if (current.get() <= 0 && this.isExtremeTimeoutPassed()) {
			ess._setEpOverChargeProtection(true);
		}
	}

	private boolean isExtremeTimeoutPassed() {
		return Duration.between(this.onEntryEssProtection, Instant.now())
				.getSeconds() > ESS_PROTECTION_EXTREME_LIMIT_TIMEOUT;
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
		return min(thisValue, //
				lastValue + thisValue * millis * MAX_INCREASE_PERCENTAGE / 1000.F /* convert [mW] to [W] */);
	}
}
