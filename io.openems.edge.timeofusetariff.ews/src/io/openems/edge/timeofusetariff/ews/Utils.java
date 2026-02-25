package io.openems.edge.timeofusetariff.ews;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsZonedDateTime;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.gson.JsonArray;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	protected static final int CLIENT_ERROR_CODE = 401;

	private Utils() {
	}

	/**
	 * Delay time provider for ENTSO-E API requests.
	 */
	public static class EwsDelayTimeProvider implements DelayTimeProvider {

		private final Clock clock;

		public EwsDelayTimeProvider(Clock clock) {
			this.clock = clock;
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(10)) //
					.plusRandomDelay(30, SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			var now = ZonedDateTime.now(this.clock);
			final ZonedDateTime nextRun = now.plusHours(1);
			// TODO plan next run for next day, if prices are already complete

			return DelayTimeProviderChain.fixedDelay(Duration.between(now, nextRun)) //
					.plusRandomDelay(10, MINUTES) // safer side not to execute exactly at 4.
					.plusRandomDelay(60, SECONDS) //
					.getDelay();
		}
	}

	protected static TimeOfUsePrices parsePrices(String jsonData) throws OpenemsNamedException {
		var result = ImmutableSortedMap.<Instant, Double>naturalOrder();
		extractPricesFromDay(result, getAsJsonArray(parseToJsonObject(jsonData), "today"));
		extractPricesFromDay(result, getAsJsonArray(parseToJsonObject(jsonData), "tomorrow"));
		return TimeOfUsePrices.from(result.build());
	}

	private static void extractPricesFromDay(Builder<Instant, Double> result, JsonArray data)
			throws OpenemsNamedException {
		for (var element : data) {
			// Cent/kWh -> Currency/MWh
			// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
			var marketPrice = getAsDouble(element, "total") * 10;
			var startTimeStamp = getAsZonedDateTime(element, "startsAt").toInstant();
			result.put(startTimeStamp, marketPrice);
		}
	}
}
