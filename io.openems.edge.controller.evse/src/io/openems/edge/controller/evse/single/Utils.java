package io.openems.edge.controller.evse.single;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.calculatePowerStep;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;
import static java.lang.Math.max;
import static java.lang.Math.min;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.controller.evse.single.EnergyScheduler.Payload;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public final class Utils {

	protected static final int FORCE_CHARGE_POWER = 11000; // [W]
	protected static final int MIN_CHARGE_POWER = 4600; // [W]

	private Utils() {
	}

	protected static final ApplySetPoint.Ability.Watt combineAbilities(ChargePointAbilities chargePointAbilities,
			ElectricVehicleAbilities electricVehicleAbilities) {
		if (chargePointAbilities == null || electricVehicleAbilities == null) {
			return EMPTY_APPLY_SET_POINT_ABILITY;
		}
		final var cp = chargePointAbilities.applySetPoint();
		final var cpMin = cp.toPower(cp.min());
		final var cpMax = cp.toPower(cp.max());
		return switch (cp.phase()) {
		case SINGLE_PHASE -> {
			if (electricVehicleAbilities.singlePhaseLimit() != null) {
				var ev = electricVehicleAbilities.singlePhaseLimit();
				var step = max(calculatePowerStep(cp), calculatePowerStep(ev));
				yield new ApplySetPoint.Ability.Watt(SINGLE_PHASE, //
						max(cpMin, ev.min()), //
						min(cpMax, ev.max()), //
						step);
			} else if (electricVehicleAbilities.threePhaseLimit() != null) {
				var ev = electricVehicleAbilities.threePhaseLimit();
				var step = max(calculatePowerStep(cp), calculatePowerStep(ev));
				yield new ApplySetPoint.Ability.Watt(SINGLE_PHASE, //
						max(cpMin, ev.min()) / 3, //
						min(cpMax, ev.max()) / 3, //
						step);
			} else {
				yield EMPTY_APPLY_SET_POINT_ABILITY;
			}
		}
		case THREE_PHASE -> {
			if (electricVehicleAbilities.threePhaseLimit() != null) {
				var ev = electricVehicleAbilities.threePhaseLimit();
				var step = max(calculatePowerStep(cp), calculatePowerStep(ev));
				yield new ApplySetPoint.Ability.Watt(THREE_PHASE, //
						max(cpMin, ev.min()), //
						min(cpMax, ev.max()), //
						step);
			} else if (electricVehicleAbilities.singlePhaseLimit() != null) {
				var ev = electricVehicleAbilities.singlePhaseLimit();
				var step = max(calculatePowerStep(cp), calculatePowerStep(ev));
				yield new ApplySetPoint.Ability.Watt(SINGLE_PHASE, //
						max(cpMin, ev.min()), //
						min(cpMax, ev.max()), //
						step);
			} else {
				yield EMPTY_APPLY_SET_POINT_ABILITY;
			}
		}
		};
	}

	protected static boolean isSessionLimitReached(Mode mode, Integer energy, int limit) {
		if (mode == Mode.SMART) {
			return false;
		}
		if (energy != null && limit > 0 && energy >= limit) {
			return true;
		}
		return false;
	}

	protected static JSCalendar.Tasks<Payload> parseSmartConfig(String smartConfig) {
		try {
			return JSCalendar.Tasks.serializer(Payload.serializer()) //
					.deserialize(smartConfig);

		} catch (Exception e) {
			e.printStackTrace();
			return JSCalendar.Tasks.empty();
		}
	}
}
