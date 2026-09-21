package io.openems.edge.timeofusetariff.entsoe.gridsell;

import java.time.Instant;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.timeofusetariff.entsoe.PriceCalculatorX;

public class GridSellUtils {

	/**
	 * Processes market prices for each 15-minute interval in the overlapping future
	 * timespan.
	 *
	 * @param priceCalculator the {@link PriceCalculatorX} used to combine market
	 *                        prices and grid fees
	 * @param marketPrices    the market prices in Currency/MWh
	 * @return the calculated prices
	 */
	public static TimeRangeValues<Double> processPrices(PriceCalculatorX priceCalculator,
			TimeRangeValues<Double> marketPrices) {
		final var timeSpan = marketPrices.getTimeSpan();
		var resultBuilder = TimeRangeValues.builder(timeSpan, marketPrices.getResolution(), Double.class);
		Instant time = timeSpan.getStartInclusive();
		while (time.isBefore(timeSpan.getEndExclusive())) {
			var marketPrice = marketPrices.getAtOrElse(time, 0.0);

			var priceWithFee = priceCalculator.calculate(//
					marketPrice); // x = EPEX Spot price

			resultBuilder.setByTime(time, priceWithFee);
			time = time.plus(DurationUnit.ofMinutes(15).getDuration());
		}

		return resultBuilder.build();
	}
}
