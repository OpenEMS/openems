package io.openems.edge.timeofusetariff.api.utils;

import static io.openems.common.utils.DateUtils.roundZonedDateTimeDownToMinutes;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Locale;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class TimeOfUseTariffUtils {

	/**
	 * Returns the Array of 24 hour [96 quarterly] electricity prices in EUR/MWh.
	 *
	 * @param clock           the {@link Clock}
	 * @param priceMap        {@link ImmutableSortedMap} with quarterly Time stamps
	 *                        and the price.
	 * @param updateTimeStamp time when prices are retrieved.
	 * @return the quarterly prices of next 24 hours along with the time they are
	 *         retrieved.
	 */
	public static TimeOfUsePrices getNext24HourPrices(Clock clock, ImmutableSortedMap<ZonedDateTime, Float> priceMap,
			ZonedDateTime updateTimeStamp) {

		// Returns the empty array if the map is empty.
		if (priceMap.isEmpty()) {
			return TimeOfUsePrices.empty(updateTimeStamp);
		}

		var now = roundZonedDateTimeDownToMinutes(ZonedDateTime.now(clock), 15);
		// Converts the map values to array.
		// if the map size is less than 96, rest of the values will store as null.
		final var priceList = priceMap.tailMap(now).values().toArray(new Float[TimeOfUsePrices.NUMBER_OF_VALUES]);

		return new TimeOfUsePrices(updateTimeStamp, priceList);
	}

	/**
	 * Generates a default DebugLog message for {@link TimeOfUseTariff}
	 * implementations.
	 * 
	 * @param tou      the {@link TimeOfUseTariff}
	 * @param currency the Currency (from {@link Meta} component)
	 * @return a debug log String
	 */
	public static String generateDebugLog(TimeOfUseTariff tou, Currency currency) {
		var result = new StringBuilder() //
				.append("Price:"); //
		{
			var p = tou.getPrices().getValues()[0];
			if (p != null) {
				result.append(String.format(Locale.ENGLISH, "%.4f", p / 1000));
			} else {
				result.append("-");
			}
		}
		if (!currency.isUndefined()) {
			result //
					.append(" ") //
					.append(currency.getName()) //
					.append("/kWh");
		}
		return result.toString();
	}
}
