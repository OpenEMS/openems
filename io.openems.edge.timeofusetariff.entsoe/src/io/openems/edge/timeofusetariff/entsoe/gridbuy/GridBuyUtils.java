package io.openems.edge.timeofusetariff.entsoe.gridbuy;

import java.time.Clock;
import java.time.Instant;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.entsoe.PriceCalculatorXY;

public class GridBuyUtils {

	/**
	 * Processes market prices by applying grid fees and exchange rate conversion
	 * for each 15-minute interval in the overlapping future timespan.
	 *
	 * @param clock           the {@link Clock} used to exclude past values
	 * @param priceCalculator the {@link PriceCalculatorXY} used to combine market
	 *                        prices and grid fees
	 * @param marketPrices    the market prices in Currency/MWh
	 * @param exchangeRate    the exchange rate multiplier applied to the calculated
	 *                        price
	 * @param gridFees        the grid fees in ct/kWh
	 * @return the calculated prices for the overlapping future timespan or
	 *         {@code null} if there is no overlap
	 */
	public static TimeRangeValues<Double> processPrices(Clock clock, PriceCalculatorXY priceCalculator,
			TimeRangeValues<Double> marketPrices, double exchangeRate, TimeOfUsePrices gridFees) {

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

			var priceWithFee = priceCalculator.calculate(//
					marketPrice, // x = EPEX Spot price
					gridFeesPerMwh); // y = Ancillary costs

			resultBuilder.setByTime(time, priceWithFee * exchangeRate);
			time = time.plus(DurationUnit.ofMinutes(15).getDuration());
		}

		return resultBuilder.build();
	}
}
