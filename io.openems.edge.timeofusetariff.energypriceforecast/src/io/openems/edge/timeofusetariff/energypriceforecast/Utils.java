package io.openems.edge.timeofusetariff.energypriceforecast;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.DateUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

final class Utils {
	static final String FORMAT = "openems-spot-forecast-v1";

	private Utils() {
	}

	static TariffData parseForecast(String json, BiddingZone biddingZone, Clock clock, Duration maxAge)
			throws OpenemsNamedException {
		var root = parseToJsonObject(json);
		if (!FORMAT.equals(getAsString(root, "format"))
				|| !ForecastMarket.forBiddingZone(biddingZone).equals(getAsString(root, "market"))
				|| !"EUR".equals(getAsString(root, "currency")) || !"EUR/MWh".equals(getAsString(root, "unit"))) {
			throw new IllegalArgumentException("Unexpected forecast format, market, currency or unit");
		}
		var now = clock.instant();
		var horizonEnd = DateUtils.roundDownToQuarter(now).plus(Duration.ofHours(48));
		var updated = Instant.parse(getAsString(root, "forecast_updated_at"));
		if (updated.plus(maxAge).isBefore(now) || updated.isAfter(now.plus(Duration.ofMinutes(5)))) {
			throw new IllegalArgumentException("Forecast source timestamp is outdated or in the future");
		}
		var prices = new TreeMap<Instant, Double>();
		for (var entry : getAsJsonArray(root, "entries")) {
			var start = Instant.parse(getAsString(entry, "start"));
			var end = Instant.parse(getAsString(entry, "end"));
			var value = getAsDouble(entry, "value");
			if (start.getEpochSecond() % 900 != 0 || start.getNano() != 0 || !end.equals(start.plusSeconds(900))
					|| !Double.isFinite(value)) {
				throw new IllegalArgumentException("Invalid forecast quarter-hour interval or price");
			}
			if (!end.isAfter(now) || end.isAfter(horizonEnd)) {
				continue;
			}
			if (prices.putIfAbsent(start, value) != null) {
				throw new IllegalArgumentException("Duplicate forecast interval");
			}
		}
		if (prices.isEmpty()) {
			throw new IllegalArgumentException("No future forecast prices available");
		}
		return new TariffData(new TariffType.DayAheadEpexSpot(biddingZone),
				TimeOfUsePrices.from(ImmutableSortedMap.copyOfSorted(prices)), Currency.getInstance("EUR"), now,
				updated, "Energy Price Forecast EU (energypriceforecast.eu), model forecast");
	}
}
