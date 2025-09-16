
package io.openems.edge.evcc.weather;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public class EvccForecastService {

	private static final Logger log = LoggerFactory.getLogger(EvccForecastService.class);

	private final BridgeHttp httpBridge;
	private final Clock clock;
	private final String apiUrl;
	private final Config config;

	private WeatherData weatherForecast = WeatherData.EMPTY_WEATHER_DATA;

	public EvccForecastService(String apiUrl, BridgeHttp httpBridge, Clock clock, Config config) {
		this.apiUrl = apiUrl;
		this.httpBridge = httpBridge;
		this.clock = clock;
		this.config = config;

		this.httpBridge.subscribeTime(new EvccDelayTimeProvider(this.clock), this::createEndpoint, this::handleResponse,
				this::handleError);
	}

	private Endpoint createEndpoint() {
		var url = UrlBuilder.parse(this.apiUrl);
		return new Endpoint(url.toEncodedString(), HttpMethod.GET, 5000, 5000, null,
				Map.of("Accept", "application/json"));
	}

	private void handleResponse(HttpResponse<String> response) {
		if (response.status().isSuccessful()) {
			try {
				JsonObject json = com.google.gson.JsonParser.parseString(response.data()).getAsJsonObject();
				this.parseJson(json);
			} catch (Exception e) {
				log.error("Error parsing EVCC response: {}", e.getMessage(), e);
			}
		} else {
			log.warn("EVCC API returned status {}", response.status().code());
		}
		log.debug("Received {} forecast entries", this.weatherForecast.asArray().length);
	}

	private void handleError(HttpError error) {
		log.error("HTTP error: {}", error.getMessage());
	}

	private void parseJson(JsonObject json) {
		TreeMap<ZonedDateTime, WeatherSnapshot> result = new TreeMap<>();
		final int interval = 15;

		JsonArray rates;
		if (json.has("rates")) {
			rates = json.getAsJsonArray("rates");
		} else if (json.has("result") && json.getAsJsonObject("result").has("rates")) {
			rates = json.getAsJsonObject("result").getAsJsonArray("rates");
		} else {
			log.warn("No rates found in EVCC JSON");
			return;
		}

		for (var element : rates) {
			JsonObject e = element.getAsJsonObject();
			Integer ghiValue = e.has("value") ? e.get("value").getAsInt() : null;
			if (ghiValue == null) {
				continue;
			}

			ZonedDateTime startTime = ZonedDateTime.parse(e.get("start").getAsString())
					.withZoneSameInstant(ZoneId.of("UTC"));
			ZonedDateTime endTime = ZonedDateTime.parse(e.get("end").getAsString())
					.withZoneSameInstant(ZoneId.of("UTC"));

			long duration = ChronoUnit.MINUTES.between(startTime, endTime);
			int steps = (int) (duration / interval); // fixed interval defined above and reused here and below

			for (int i = 0; i < steps; i++) {
				result.put(startTime.plusMinutes(i * interval), this.createSnapshot(ghiValue));
			}
		}

		this.weatherForecast = WeatherData.from(ImmutableSortedMap.copyOf(result));
	}

	private WeatherSnapshot createSnapshot(int power) {
		double factor = 1;
		if (this.config != null) {
			factor = this.config.factor();
		}
		int ghi = (int) (power / factor);

		return new WeatherSnapshot(ghi, 0.0, 0.0, 0);
	}

	/**
	 * Returns the currently cached weather forecast.
	 *
	 * <p>
	 * The forecast is periodically updated by the subscribed HTTP bridge from the
	 * EVCC API. If no valid forecast has been received yet, this method returns
	 * {@link WeatherData#EMPTY_WEATHER_DATA}.
	 *
	 * @return the latest {@link WeatherData}, or
	 *         {@link WeatherData#EMPTY_WEATHER_DATA} if no forecast is available
	 */
	public WeatherData getWeatherForecast() {
		return this.weatherForecast;
	}
}
