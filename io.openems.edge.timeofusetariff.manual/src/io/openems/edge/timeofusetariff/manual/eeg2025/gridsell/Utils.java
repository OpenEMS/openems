package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import io.openems.common.utils.TimeRangeValues;

public final class Utils {

	private Utils() {
	}

	/**
	 * Builds a series of prices. If the market price is negative, the value is set
	 * to 0. Otherwise, the fixed price is used.
	 *
	 * @param fixedPrice   the fixed price
	 * @param marketPrices the market prices
	 * @return the processed series of prices
	 */
	public static TimeRangeValues<Double> processPrices(double fixedPrice, TimeRangeValues<Double> marketPrices) {
		final var timeSpan = marketPrices.getTimeSpan();
		final var interval = marketPrices.getResolution();
		final var builder = TimeRangeValues.builder(timeSpan, interval, Double.class);

		for (var time = timeSpan.getStartInclusive(); //
				time.isBefore(timeSpan.getEndExclusive()); //
				time = time.plus(interval.getDuration())) {
			final var marketPrice = marketPrices.getAt(time);
			builder.setByTime(time, (marketPrice != null && marketPrice < 0) ? 0.0 : fixedPrice);
		}

		return builder.build();
	}
}
