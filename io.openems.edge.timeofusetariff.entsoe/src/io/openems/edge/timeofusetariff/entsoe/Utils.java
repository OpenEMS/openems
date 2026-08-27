package io.openems.edge.timeofusetariff.entsoe;

import java.time.Clock;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.edge.timeofusetariff.api.AncillaryCosts;

public class Utils {

	/**
	 * Parses the ancillary cost configuration JSON into a schedule of
	 * {@link JSCalendar.Tasks}.
	 *
	 * @param clock          The {@link Clock}
	 * @param biddingZone    the {@link EntsoeBiddingZone}
	 * @param ancillaryCosts the JSON configuration object
	 * @param logWarn        a {@link Consumer} for a warning message
	 * @return a {@link JSCalendar.Tasks} instance representing the schedule or an
	 *         empty list if no valid schedule is provided.
	 * @throws OpenemsNamedException on error.
	 */
	public static JSCalendar.Tasks<Double> parseToSchedule(Clock clock, EntsoeBiddingZone biddingZone,
			String ancillaryCosts, Consumer<String> logWarn) throws OpenemsNamedException {
		if (ancillaryCosts == null || ancillaryCosts.isBlank()) {
			return JSCalendar.Tasks.empty();
		}

		return switch (biddingZone) {
		case GERMANY //
			-> AncillaryCosts.parseForGermany(clock, ancillaryCosts);

		case AUSTRIA, SWEDEN_SE1, SWEDEN_SE2, SWEDEN_SE3, SWEDEN_SE4, BELGIUM, NETHERLANDS, CZECHIA, LITHUANIA,
				GREECE -> {
			logWarn.accept("Parser for " + biddingZone.name() + "-Scheduler is not implemented");
			throw new OpenemsException("Parser for bidding zone " + biddingZone.name() + " is not implemented");
		}
		};
	}
}
