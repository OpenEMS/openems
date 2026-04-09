package io.openems.edge.timeofusetariff.entsoe;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.timeofusetariff.api.AncillaryCosts;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	protected static TimeRangeValues<Double> processPrices(Clock clock, TimeRangeValues<Double> marketPrices,
			double exchangeRate, TimeOfUsePrices gridFees) {

		var timeSpan = marketPrices.getTimeSpan().getOverlappingTime(gridFees.getTimeSpan())
				.flatMap(x -> x.narrowDownToStartDate(Instant.now(clock))).orElse(null);
		if (timeSpan == null) {
			return null;
		}

		var resultBuilder = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class);

		Instant time = timeSpan.getStartInclusive();
		while (time.isBefore(timeSpan.getEndExclusive())) {
			var marketPrice = marketPrices.getAtOrElse(time, 0.0);
			var gridFee = gridFees.getAtOrElse(time, 0.0);

			// converting grid fees from ct/KWh -> EUR/MWh
			var gridFeesPerMwh = gridFee * 10;
			var priceWithFee = (marketPrice + gridFeesPerMwh) * exchangeRate;

			resultBuilder.setByTime(time, priceWithFee);
			time = time.plus(DurationUnit.ofMinutes(15).getDuration());
		}

		return resultBuilder.build();
	}

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
