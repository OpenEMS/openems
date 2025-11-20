package io.openems.edge.timeofusetariff.manual.octopus;

import static io.openems.common.utils.StringUtils.isNullOrBlank;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.getScheduleStandardPriceForGermany;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.parseForGermany;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class Utils {

	/**
	 * Conversion factor: cent/kWh → currency/MWh.
	 * 
	 * <p>
	 * cent/kWh × (1 currency/100 cent) × (1000 kWh/1 MWh) = currency/MWh = 1000 /
	 * 100 = 10
	 * </p>
	 */
	public static final double CENT_PER_KWH_TO_CURRENCY_PER_MWH = 10.0;

	/**
	 * Retrieves the standard ancillary price for a specific date and time.
	 * 
	 * @param ancillaryCosts the ancillary costs configuration string containing DSO
	 *                       information or custom schedule in JSON format
	 * @param time           {@link ZonedDateTime} for which to retrieve the
	 *                       standard price
	 * @return the standard ancillary price in ct/kWh for the given time, or 0.0 if
	 *         the price cannot be determined due to configuration errors or missing
	 *         data
	 * 
	 * @see #getScheduleStandardPriceForGermany(String, ZonedDateTime)
	 */
	public static double getStandardPrice(String ancillaryCosts, ZonedDateTime time) {
		try {
			return getScheduleStandardPriceForGermany(ancillaryCosts, time);
		} catch (OpenemsNamedException e) {
			return 0.0;
		}
	}

	/**
	 * Combines Octopus prices with ancillary costs using the formula: Final_Price =
	 * Octopus_Price - (Ancillary_Standard_Price - Ancillary_Actual_Price).
	 * 
	 * @param octopusHelper        the Octopus price helper
	 * @param ancillaryCostsHelper the ancillary costs helper
	 * @param ancillaryCostsConfig the ancillary costs configuration
	 * @param logWarn              logger for warnings
	 * @return combined {@link TimeOfUsePrices}
	 */
	public static TimeOfUsePrices getPrices(TouManualHelper octopusHelper, TouManualHelper ancillaryCostsHelper,
			String ancillaryCostsConfig, Consumer<String> logWarn) {

		if (isNullOrBlank(ancillaryCostsConfig)) {
			return octopusHelper.getPrices();
		}

		// Get both price schedules
		final var octopusPrices = octopusHelper.getPrices();
		final var ancillaryPrices = ancillaryCostsHelper.getPrices();

		// Create a new map for the combined prices
		final var combinedPricesBuilder = ImmutableSortedMap.<ZonedDateTime, Double>naturalOrder();

		// Iterate over the base octopus price map
		for (var entry : octopusPrices.toMap().entrySet()) {
			final var time = entry.getKey();
			final var octopusPrice = entry.getValue();

			// 0.0 standard price (fallback)
			final var ancillaryActualPrice = ancillaryPrices.getAtOrElse(time, 0.0) * CENT_PER_KWH_TO_CURRENCY_PER_MWH;
			final var ancillaryStandardPrice = getStandardPrice(ancillaryCostsConfig, time)
					* CENT_PER_KWH_TO_CURRENCY_PER_MWH;
			final var finalPrice = calculateCombinedPrice(octopusPrice, ancillaryActualPrice, ancillaryStandardPrice,
					logWarn);

			combinedPricesBuilder.put(time, finalPrice);
		}

		return TimeOfUsePrices.from(combinedPricesBuilder.build());
	}

	/**
	 * Returns the schedule based on the configuration.
	 * 
	 * @param clock  The {link Clock}
	 * @param config The config.
	 * @return the {@link JSCalendar.Tasks} representing daily recurring tariff
	 *         intervals.
	 * @throws OpenemsNamedException on error.
	 */
	public static JSCalendar.Tasks<Double> parseScheduleFromConfig(Clock clock, String config)
			throws OpenemsNamedException {

		return isNullOrBlank(config) //
				? JSCalendar.Tasks.empty()
				: parseForGermany(clock, config);
	}

	private static double calculateCombinedPrice(double octopusPrice, double ancillaryActualPrice,
			double ancillaryStandardPrice, Consumer<String> logWarn) {
		if (Double.isNaN(ancillaryActualPrice) || Double.isNaN(ancillaryStandardPrice)) {
			logWarn.accept("Invalid price value detected in calculation");
			return octopusPrice;
		}

		final var delta = ancillaryStandardPrice - ancillaryActualPrice;
		return octopusPrice - delta;
	}

}
