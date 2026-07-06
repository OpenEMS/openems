package io.openems.edge.ess.generic.common.essprotection;

import static io.openems.common.utils.IntUtils.minInteger;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.generic.common.essprotection.EpForceChargeHandler.EpForceChargeParams;
import io.openems.edge.ess.generic.common.essprotection.EpForceChargeHandler.State;

public class EpRampHandler implements EssProtectionHandler {

	private static final int LIMITATION_DISTANCE_V = 10;
	private static final int RELEASE_DISTANCE_V = 13;

	private final EpForceChargeHandler forceChargeHandler = new EpForceChargeHandler(
			EpForceChargeParams.create(3, 2, 5));

	private LimitationState chargeState = LimitationState.NO_LIMIT;
	private LimitationState dischargeState = LimitationState.NO_LIMIT;
	private Double lastChargeFactor;
	private Double lastDischargeFactor;

	/**
	 * Ramp depending on the voltage difference between the battery and the limit.
	 * 
	 * <p>
	 * Distance 0V means 0% of the max current, distance 5V means 50% of the max
	 * current, distance 10V means 100% of the max current.
	 */
	private final PolyLine ramp = PolyLine.create() //
			.addPoint(0, 0) //
			.addPoint(LIMITATION_DISTANCE_V, 1) //
			.build();

	@Override
	public EssProtectionLimits calculateEssProtectionLimits(Battery battery, SymmetricBatteryInverter inverter) {
		return this.calculateLimits(battery, inverter, this.ramp);
	}

	/**
	 * Calculate the limits for the given battery and inverter using the given ramp.
	 * 
	 * <p>
	 * Technical details: Whenever the current is lowered, the voltage of the system
	 * is increasing slightly depending on the inner resistance and the battery
	 * size. To avoid fluctuations upon this technical detail, a hysteresis was
	 * used. Once ACTIVE_LIMIT is entered, a small voltage recovery (within the
	 * hysteresis band 10–13V) should NOT increase/release the limit. Only after
	 * crossing RELEASE_DISTANCE_V (≥13V distance) full current is allowed.
	 * </p>
	 * 
	 * @param battery  battery to calculate the limits for
	 * @param inverter inverter to calculate the limits for
	 * @param ramp     ramp to use for calculating the limits
	 * @return the calculated limits
	 */
	private EssProtectionLimits calculateLimits(Battery battery, SymmetricBatteryInverter inverter, PolyLine ramp) {
		var dcVoltage = battery.getVoltage().get();

		var inverterDcMinVoltage = inverter.getDcMinVoltage().get();
		var inverterDcMaxVoltage = inverter.getDcMaxVoltage().get();

		// Not ideal, because we could limit too restrictively if the chargeMaxCurrent
		// is already limited by the BatteryProtection, but it's technically OK
		// to limit more instead of running into the minVoltage.
		var chargeMaxCurrent = battery.getChargeMaxCurrent().get();
		var dischargeMaxCurrent = battery.getDischargeMaxCurrent().get();

		if (dcVoltage == null || inverterDcMinVoltage == null || inverterDcMaxVoltage == null
				|| chargeMaxCurrent == null || dischargeMaxCurrent == null) {
			return EssProtectionLimits.EMPTY;
		}

		var chargeDistance = inverterDcMaxVoltage - dcVoltage;
		var dischargeDistance = dcVoltage - inverterDcMinVoltage;

		this.chargeState = nextLimitationState(this.chargeState, chargeDistance);
		this.dischargeState = nextLimitationState(this.dischargeState, dischargeDistance);

		var chargeFactor = toFactor(this.chargeState, chargeDistance, ramp, this.lastChargeFactor);
		var dischargeFactor = toFactor(this.dischargeState, dischargeDistance, ramp, this.lastDischargeFactor);

		this.lastChargeFactor = chargeFactor;
		this.lastDischargeFactor = dischargeFactor;

		var chargeLimit = (int) (chargeMaxCurrent * chargeFactor);
		var dischargeLimit = (int) (dischargeMaxCurrent * dischargeFactor);

		// Force charge
		this.forceChargeHandler.update(dcVoltage, inverterDcMinVoltage);
		dischargeLimit = minInteger(dischargeLimit, forceChargeCurrent(this.forceChargeHandler.getState()));

		return new EssProtectionLimits(chargeLimit, dischargeLimit);
	}

	private static Integer forceChargeCurrent(State state) {
		return switch (state) {
		case IDLE, WAIT_FOR_FORCE_MODE //
			-> null;
		case FORCE_MODE //
			-> -2;
		case BLOCK_MODE //
			-> 0;
		};
	}

	private enum LimitationState {
		NO_LIMIT, ACTIVE_LIMIT
	}

	private static LimitationState nextLimitationState(LimitationState current, double distance) {
		return switch (current) {
		case NO_LIMIT -> distance <= LIMITATION_DISTANCE_V ? LimitationState.ACTIVE_LIMIT : LimitationState.NO_LIMIT;
		case ACTIVE_LIMIT -> distance >= RELEASE_DISTANCE_V ? LimitationState.NO_LIMIT : LimitationState.ACTIVE_LIMIT;
		};
	}

	private static double toFactor(LimitationState state, double distance, PolyLine ramp, Double lastLimit) {
		return switch (state) {
		case NO_LIMIT -> 1.0;
		case ACTIVE_LIMIT -> TypeUtils.min(lastLimit, ramp.getValue(distance));
		};
	}
}
