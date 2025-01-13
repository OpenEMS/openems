package io.openems.edge.ess.generic.common;

import static io.openems.edge.common.type.TypeUtils.multiply;
import static io.openems.edge.common.type.TypeUtils.subtract;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiFunction;

import org.apache.logging.log4j.util.TriConsumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.filter.Pt1filter;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.symmetric.ChannelManager;
import io.openems.edge.ess.generic.symmetric.EssProtection;

/**
 * Helper class to handle calculation of Allowed-Charge-Power and
 * Allowed-Discharge-Power. This class is used by {@link ChannelManager} as a
 * callback to updates of Battery Channels.
 */
public abstract class AbstractAllowedChargeDischargeHandler<ESS extends SymmetricEss & CycleProvider>
		implements TriConsumer<ClockProvider, Battery, SymmetricBatteryInverter> {

	public static final float DISCHARGE_EFFICIENCY_FACTOR = 0.95F;

	/**
	 * Allow a maximum increase per second.
	 *
	 * <p>
	 * 5 % of possible allowed charge/discharge power
	 */
	public static final float MAX_INCREASE_PERCENTAGE = 0.05F;

	public static final int VOLTAGE_CONTROL_FILTER_TIME_CONSTANT = 10; // [seconds]
	public static final int VOLTAGE_CONTROL_OFFSET = 4; // [V]

	protected final ESS parent;

	private static final int ESS_PROTECTION_EXTREME_LIMIT_TIMEOUT = 240; // [seconds]

	private final Pt1filter pt1FilterChargeMaxCurrentVoltLimit;
	private final Pt1filter pt1FilterDischargeMaxCurrentVoltLimit;

	public AbstractAllowedChargeDischargeHandler(ESS parent) {
		this.parent = parent;
		this.pt1FilterChargeMaxCurrentVoltLimit = new Pt1filter(VOLTAGE_CONTROL_FILTER_TIME_CONSTANT,
				this.parent.getCycleTime());
		this.pt1FilterDischargeMaxCurrentVoltLimit = new Pt1filter(VOLTAGE_CONTROL_FILTER_TIME_CONSTANT,
				this.parent.getCycleTime());
	}

	protected float lastBatteryAllowedChargePower;
	protected float lastBatteryAllowedDischargePower;
	private Instant lastCalculate = null;
	private Instant onEntryEssProtection = null;

	@Override
	public abstract void accept(ClockProvider clockProvider, Battery battery, SymmetricBatteryInverter inverter);

	/**
	 * Calculates Allowed-Charge-Power and Allowed-Discharge Power from the given
	 * parameters. Result is stored in 'lastBatteryAllowedChargePower' and
	 * 'lastBatteryAllowedDischargePower' variables - both as positive values!
	 *
	 * @param clockProvider the {@link ClockProvider}
	 * @param battery       the {@link Battery}
	 * @param inverter      the {@link SymmetricBatteryInverter}
	 */
	protected void calculateAllowedChargeDischargePower(ClockProvider clockProvider, Battery battery,
			SymmetricBatteryInverter inverter) {
		final var cycleTime = this.parent.getCycleTime();
		var chargeMaxCurrent = battery.getChargeMaxCurrentChannel().getNextValue().get();
		var dischargeMaxCurrent = battery.getDischargeMaxCurrentChannel().getNextValue().get();

		final var voltRegulationChargeMaxCurrent = calculateMaxCurrent(battery, inverter, cycleTime,
				this.pt1FilterChargeMaxCurrentVoltLimit, TypeUtils::min, TypeUtils::subtract, TypeUtils::subtract,
				true);
		final var voltRegulationDischargeMaxCurrent = calculateMaxCurrent(battery, inverter, cycleTime,
				this.pt1FilterDischargeMaxCurrentVoltLimit, TypeUtils::max, TypeUtils::sum, TypeUtils::sum, false);

		if (this.parent instanceof EssProtection ess) {
			ess._setEpChargeMaxCurrent(voltRegulationChargeMaxCurrent);
			ess._setEpDischargeMaxCurrent(voltRegulationDischargeMaxCurrent);
		}

		chargeMaxCurrent = TypeUtils.min(chargeMaxCurrent, voltRegulationChargeMaxCurrent);
		dischargeMaxCurrent = TypeUtils.min(dischargeMaxCurrent, voltRegulationDischargeMaxCurrent);

		final var current = battery.getCurrentChannel().value();
		this.checkEssProtectionExtremes(clockProvider, chargeMaxCurrent, dischargeMaxCurrent, current);

		final boolean isStarted = this.parent instanceof StartStoppable p ? p.isStarted() : true;
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
			charge = chargeMaxCurrent * voltage;
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

		if (dischargeMaxCurrent < 0//
				&& current.get() >= 0//
				&& this.isExtremeTimeoutPassed()) {
			ess._setEpDeepDischargeProtection(true);
		}

		if (chargeMaxCurrent < 0 //
				&& current.get() <= 0 //
				&& this.isExtremeTimeoutPassed()) {
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

	private record RegulationValues(//
			boolean isBatteryStarted, //
			Value<Integer> voltage, //
			Value<Integer> current, //
			Value<Integer> chargeMaxVoltage, //
			Value<Integer> dischargeMinVoltage, //
			Value<Integer> innerResistance, //
			Value<Integer> inverterDcMinVoltage, //
			Value<Integer> inverterDcMaxVoltage) {
		public RegulationValues(Battery battery, SymmetricBatteryInverter inverter) {
			this(//
					battery.isStarted(), //
					battery.getVoltage(), //
					battery.getCurrent(), //
					battery.getChargeMaxVoltage(), //
					battery.getDischargeMinVoltage(), //
					battery.getInnerResistance(), //
					inverter.getDcMinVoltage(), //
					inverter.getDcMaxVoltage());
		}
	}

	private static boolean areRegulationValuesDefined(RegulationValues regulationValues) {
		return regulationValues.isBatteryStarted()//
				&& regulationValues.voltage.isDefined()//
				&& regulationValues.current.isDefined() //
				&& regulationValues.chargeMaxVoltage.isDefined()//
				&& regulationValues.dischargeMinVoltage.isDefined()//
				&& regulationValues.innerResistance.isDefined()//
				&& regulationValues.inverterDcMinVoltage.isDefined()//
				&& regulationValues.inverterDcMaxVoltage.isDefined();//
	}

	private static Integer calculateMaxCurrent(Battery battery, SymmetricBatteryInverter inverter, int cycleTime,
			Pt1filter pt1Filter, BiFunction<Integer, Integer, Integer> dcLimit,
			BiFunction<Integer, Integer, Integer> voltageOffset, BiFunction<Double, Double, Double> typeUtilsMethod,
			boolean invert) {
		var regulationValues = new RegulationValues(battery, inverter);
		if (!areRegulationValuesDefined(regulationValues)) {
			return null;
		}

		final var batteryLimit = invert//
				? regulationValues.chargeMaxVoltage.get()
				: regulationValues.dischargeMinVoltage.get();
		final var inverterLimit = invert //
				? regulationValues.inverterDcMaxVoltage.get()
				: regulationValues.inverterDcMinVoltage.get();
		final var limitVoltage = dcLimit.apply(//
				voltageOffset.apply(//
						batteryLimit, //
						VOLTAGE_CONTROL_OFFSET),
				inverterLimit);

		var subtractLimit = subtract(regulationValues.voltage.get(), limitVoltage);
		var voltageDifference = invert ? multiply(subtractLimit, -1) : subtractLimit;

		var resistance = regulationValues.innerResistance.get() / 1000.;
		final var deltaChargeCurrent = voltageDifference / resistance;
		var maxCurrentVoltLimit = typeUtilsMethod.apply(deltaChargeCurrent, (double) regulationValues.current.get());
		pt1Filter.setCycleTime(cycleTime);
		return pt1Filter.applyPt1Filter(max(maxCurrentVoltLimit, -5.0));
	}
}