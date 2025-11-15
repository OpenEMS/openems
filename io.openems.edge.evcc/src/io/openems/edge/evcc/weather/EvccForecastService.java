
package io.openems.edge.evcc.weather;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeService.TimeEndpoint;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

public class EvccForecastService {

	private static final Logger log = LoggerFactory.getLogger(EvccForecastService.class);

	private final HttpBridgeTimeService timeService;
	private final Clock clock;
	private final String apiUrl;
	private final Config config;

	private List<QuarterlyWeatherSnapshot> weatherForecast = new ArrayList<>();
	private Instant lastUpdate = null;
	private TimeEndpoint subscription = null;

	public EvccForecastService(String apiUrl, HttpBridgeTimeService timeService, Clock clock, Config config) {
		this.apiUrl = apiUrl;
		this.timeService = timeService;
		this.clock = clock;
		this.config = config;

		this.subscription = this.timeService.subscribeTime(new EvccDelayTimeProvider(this.clock), this::createEndpoint,
				this::handleResponse, this::handleError);
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
				this.lastUpdate = Instant.now(this.clock);
			} catch (Exception e) {
				log.error("Error parsing EVCC response: {}", e.getMessage(), e);
			}
		} else {
			log.warn("EVCC API returned status {}", response.status().code());
		}
		log.debug("Received {} forecast entries", this.weatherForecast.size());
	}

	private void handleError(HttpError error) {
		log.error("HTTP error: {}", error.getMessage());
	}

	private void parseJson(JsonObject json) {
		List<QuarterlyWeatherSnapshot> result = new ArrayList<>();
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
			Integer powerValue = e.has("value") ? e.get("value").getAsInt() : null;
			if (powerValue == null) {
				continue;
			}

			ZonedDateTime startTime = ZonedDateTime.parse(e.get("start").getAsString())
					.withZoneSameInstant(ZoneId.of("UTC"));
			ZonedDateTime endTime = ZonedDateTime.parse(e.get("end").getAsString())
					.withZoneSameInstant(ZoneId.of("UTC"));

			long duration = ChronoUnit.MINUTES.between(startTime, endTime);
			int steps = (int) (duration / interval);

			for (int i = 0; i < steps; i++) {
				ZonedDateTime datetime = startTime.plusMinutes(i * interval);
				result.add(this.createSnapshot(datetime, powerValue));
			}
		}

		this.weatherForecast = result;
	}

	private QuarterlyWeatherSnapshot createSnapshot(ZonedDateTime datetime, int power) {
		double factor = 1;
		if (this.config != null) {
			factor = this.config.factor();
		}
		double ghi = power / factor;

		// DNI is set to 0.0 since EVCC only provides power forecasts, not actual irradiance data
		return new QuarterlyWeatherSnapshot(datetime, ghi, 0.0);
	}

	/**
	 * Returns the currently cached weather forecast.
	 *
	 * <p>
	 * The forecast is periodically updated by the subscribed HTTP bridge from the
	 * EVCC API. If no valid forecast has been received yet, this method returns an
	 * empty list.
	 *
	 * @return the latest list of {@link QuarterlyWeatherSnapshot}, or an empty
	 *         list if no forecast is available
	 */
	public List<QuarterlyWeatherSnapshot> getWeatherForecast() {
		return this.weatherForecast;
	}

	/**
	 * Returns the timestamp of the last successful forecast update.
	 *
	 * @return the {@link Instant} of the last update, or null if no update has
	 *         occurred yet
	 */
	public Instant getLastUpdate() {
		return this.lastUpdate;
	}

	/**
	 * Removes the subscription and cleans up resources.
	 */
	public void cleanup() {
		if (this.subscription != null && this.timeService != null) {
			this.timeService.removeTimeEndpointIf(t -> t.equals(this.subscription));
			this.subscription = null;
		}
	}
}
