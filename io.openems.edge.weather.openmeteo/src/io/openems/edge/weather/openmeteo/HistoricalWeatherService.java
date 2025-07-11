package io.openems.edge.weather.openmeteo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.common.meta.Coordinates;
import io.openems.edge.weather.api.WeatherData;

public class HistoricalWeatherService {

	private static final String API_SCHEME = "https";
	private static final String API_HOST = "historical-forecast-api.open-meteo.com";
	private static final String API_HOST_COMMERCIAL = "customer-historical-forecast-api.open-meteo.com";
	private static final String API_VERSION = "v1";

	private final Logger log = LoggerFactory.getLogger(HistoricalWeatherService.class);

	private final BridgeHttp httpBridge;
	private final String[] weatherVariables;
	private final String apiKey;
	private final UrlBuilder baseUrl;

	public HistoricalWeatherService(BridgeHttp httpBridge, String[] weatherVariables, String apiKey) {
		super();
		this.httpBridge = httpBridge;
		this.weatherVariables = weatherVariables;
		this.apiKey = apiKey;
		this.baseUrl = this.buildBaseUrl();
	}

	protected CompletableFuture<WeatherData> getWeatherData(Optional<Coordinates> coordinates, ZonedDateTime dateFrom,
			ZonedDateTime dateTo, ZoneId zone) {
		if (coordinates.isEmpty()) {
			this.log.error("Can't get historical weather data, coordinates are missing");
			return CompletableFuture.completedFuture(WeatherData.EMPTY_WEATHER_DATA);
		}

		String url = this.buildHistoricalUrl(//
				coordinates.get(), //
				dateFrom.withZoneSameInstant(ZoneOffset.UTC).toLocalDate(), //
				dateTo.withZoneSameInstant(ZoneOffset.UTC).toLocalDate(), //
				ZoneId.of("UTC")//
		);

		return this.httpBridge.getJson(url).thenApply(response -> {
			return Utils.parseWeatherDataFromJson(response.data(), this.weatherVariables, zone);
		});
	}

	private UrlBuilder buildBaseUrl() {
		var builder = UrlBuilder.create()//
				.withScheme(API_SCHEME)//
				.withHost(this.apiKey != null //
						? API_HOST_COMMERCIAL //
						: API_HOST)//
				.withPath("/" + API_VERSION + "/forecast")//
				.withQueryParam("minutely_15", String.join(",", this.weatherVariables));

		if (this.apiKey != null && !this.apiKey.isBlank()) {
			builder = builder.withQueryParam("apikey", this.apiKey);
		}

		return builder;
	}

	private String buildHistoricalUrl(Coordinates coordinates, LocalDate dateFrom, LocalDate dateTo, ZoneId zone) {
		return this.baseUrl//
				.withQueryParam("latitude", String.valueOf(coordinates.latitude()))//
				.withQueryParam("longitude", String.valueOf(coordinates.longitude()))//
				.withQueryParam("start_date", dateFrom.toString())//
				.withQueryParam("end_date", dateTo.toString())//
				.withQueryParam("timezone", zone.toString())//
				.toEncodedString();
	}
}
