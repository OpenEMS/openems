package io.openems.edge.controller.evse.single;

import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.edge.controller.evse.single.EnergyScheduler.Payload;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;

public final class Utils {

	protected static final int FORCE_CHARGE_POWER = 11000; // [W]
	protected static final int MIN_CHARGE_POWER = 4600; // [W]

	private Utils() {
	}

	protected static final Limit mergeLimits(EvseChargePoint.ChargeParams chargePoint,
			EvseElectricVehicle.ChargeParams electricVehicle) {
		// TODO if EV is single-phase and CP is three-phase, this should still produce a
		// non-null result
		if (chargePoint == null || electricVehicle == null) {
			return null;
		}
		var cp = chargePoint.limit();
		return electricVehicle.limits().stream() //
				.filter(ev -> ev.phase() == cp.phase()) //
				.findFirst() //
				.map(ev -> new Limit(cp.phase(), //
						max(cp.minCurrent(), ev.minCurrent()), //
						min(cp.maxCurrent(), ev.maxCurrent()))) //
				.orElse(null);
	}

	protected static ZonedDateTime getTargetDateTime(ZonedDateTime startTime, int hour) {
		var localTime = startTime.withZoneSameInstant(Clock.systemDefaultZone().getZone());
		var targetDate = localTime.getHour() > hour //
				? startTime.plusDays(1) //
				: startTime;
		return targetDate.truncatedTo(ChronoUnit.DAYS).withHour(hour);
	}

	protected static boolean getSessionLimitReached(Mode mode, Integer energy, int limit) {
		if (mode == Mode.SMART) {
			return false;
		}
		if (energy != null && limit > 0 && energy >= limit) {
			return true;
		}
		return false;
	}

	protected static ImmutableList<Task<Payload>> parseSmartConfig(String smartConfig) {
		try {
			return JSCalendar.Tasks.<Payload>fromJson(parseToJsonArray(smartConfig), Payload::fromJson);

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			return ImmutableList.of();
		}
	}
}
