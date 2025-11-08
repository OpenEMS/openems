package io.openems.edge.weather.openmeteo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

public class HistoricalWeatherService {

	private static final String API_SCHEME = "https";
	private static final String API_HOST = "historical-forecast-api.open-meteo.com";
	private static final String API_HOST_COMMERCIAL = "customer-historical-forecast-api.open-meteo.com";
	private static final String API_VERSION = "v1";

	private final BridgeHttp httpBridge;
	private final String apiKey;
	private final UrlBuilder baseUrl;
	private final WeatherDataParser weatherDataParser;

	public HistoricalWeatherService(//
			BridgeHttp httpBridge, //
			String apiKey, //
			WeatherDataParser weatherDataParser) {
		this.httpBridge = httpBridge;
		this.apiKey = apiKey;
		this.baseUrl = this.buildBaseUrl();
		this.weatherDataParser = weatherDataParser;
	}

	protected CompletableFuture<List<QuarterlyWeatherSnapshot>> getWeatherData(//
			Coordinates coordinates, //
			LocalDate dateFrom, //
			LocalDate dateTo, //
			ZoneId targetZone) {
		if (coordinates == null) {
			return CompletableFuture
					.failedFuture(new OpenemsException("Can't get historical weather data, coordinates are missing"));
		}

		String url = this.buildHistoricalUrl(//
				coordinates, //
				dateFrom, //
				dateTo, //
				targetZone);

		return this.httpBridge.getJson(url).thenApply(response -> {
			var json = response.data().getAsJsonObject();
			var responseZone = ZoneId.of(json.get(HistoricalQueryParams.TIMEZONE).getAsString());

			return this.weatherDataParser.parseQuarterly(//
					json.getAsJsonObject(QuarterlyWeatherVariables.JSON_KEY), //
					responseZone, //
					targetZone);
		});
	}

	private UrlBuilder buildBaseUrl() {
		var builder = UrlBuilder.create()//
				.withScheme(API_SCHEME)//
				.withHost(this.apiKey != null //
						? API_HOST_COMMERCIAL //
						: API_HOST)//
				.withPath("/" + API_VERSION + "/forecast")//
				.withQueryParam(QuarterlyWeatherVariables.JSON_KEY, String.join(",", QuarterlyWeatherVariables.ALL));

		if (this.apiKey != null) {
			builder = builder.withQueryParam(HistoricalQueryParams.API_KEY, this.apiKey);
		}

		return builder;
	}

	@VisibleForTesting
	String buildHistoricalUrl(Coordinates coordinates, LocalDate dateFrom, LocalDate dateTo, ZoneId zone) {
		return this.baseUrl//
				.withQueryParam(HistoricalQueryParams.LATITUDE, String.valueOf(coordinates.latitude()))//
				.withQueryParam(HistoricalQueryParams.LONGITUDE, String.valueOf(coordinates.longitude()))//
				.withQueryParam(HistoricalQueryParams.START_DATE, dateFrom.toString())//
				.withQueryParam(HistoricalQueryParams.END_DATE, dateTo.toString())//
				.withQueryParam(HistoricalQueryParams.TIMEZONE, zone.toString())//
				.toEncodedString();
	}
}
