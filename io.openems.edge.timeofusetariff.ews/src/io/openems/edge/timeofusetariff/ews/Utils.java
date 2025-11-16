package io.openems.edge.timeofusetariff.ews;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsZonedDateTime;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.timeofusetariff.ews.TimeOfUseTariffEwsImpl.CLIENT_ERROR_CODE;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static final int RETRY_AFTER_UNABLE_TO_UPDATE_PRICES_MINUTES = 5;
	
	private Utils() {
	}
	
	protected static TimeOfUsePrices parsePrices(String jsonData) throws OpenemsNamedException {
		var result = ImmutableSortedMap.<ZonedDateTime, Double>naturalOrder();
		extractPricesFromDay(result, getAsJsonArray(parseToJsonObject(jsonData), "today"));
		extractPricesFromDay(result, getAsJsonArray(parseToJsonObject(jsonData), "tomorrow"));
		return TimeOfUsePrices.from(result.build());
	}

	private static void extractPricesFromDay(Builder<ZonedDateTime, Double> result, JsonArray data)
			throws OpenemsNamedException {
		for (var element : data) {
			// Cent/kWh -> Currency/MWh
			// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
			var marketPrice = getAsDouble(element, "total") * 10;
			ZonedDateTime startTimeStamp = getAsZonedDateTime(element, "startsAt");
			result.put(startTimeStamp, marketPrice);
		}
	}
	
	protected static long calculateDelay(int httpStatusCode, boolean unableToUpdatePrices) {
		
		final var now = ZonedDateTime.now();
		final ZonedDateTime nextRun;

		if (!unableToUpdatePrices) {
			// next price update at next hour for successful response
			nextRun = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
		} else if (httpStatusCode == CLIENT_ERROR_CODE) {
			return 0;
		} else {
			nextRun = now.plusMinutes(RETRY_AFTER_UNABLE_TO_UPDATE_PRICES_MINUTES).truncatedTo(ChronoUnit.MINUTES);
			LOG.warn("Unable to Update the prices, Retrying again at: " + nextRun);
		}

		return Duration.between(now, nextRun.plusSeconds(new Random().nextInt(60))) // randomly add a few seconds
				.getSeconds();
	}
}
