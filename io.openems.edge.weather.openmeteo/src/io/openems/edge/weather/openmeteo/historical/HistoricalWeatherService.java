package io.openems.edge.weather.openmeteo.historical;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.openmeteo.data.QuarterlyWeatherVariables;
import io.openems.edge.weather.openmeteo.data.WeatherDataParser;

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

	/**
	 * Fetches historical weather data for the given coordinates and date range.
	 *
	 * @param coordinates the geographical coordinates to query
	 * @param dateFrom    the start date of the historical data
	 * @param dateTo      the end date of the historical data
	 * @param targetZone  the time zone to convert the weather data to
	 * @return a CompletableFuture containing a list of quarterly weather snapshots
	 * @throws OpenemsException if coordinates are null
	 */
	public CompletableFuture<List<QuarterlyWeatherSnapshot>> getWeatherData(//
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
			var responseOffset = ZoneOffset.ofTotalSeconds(//
					json.get(HistoricalQueryParams.UTC_OFFSET_SECONDS).getAsInt());

			return this.weatherDataParser.parseQuarterly(//
					json.getAsJsonObject(QuarterlyWeatherVariables.JSON_KEY), //
					responseOffset, //
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
