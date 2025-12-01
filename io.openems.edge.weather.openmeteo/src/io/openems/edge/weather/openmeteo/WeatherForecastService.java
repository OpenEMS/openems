package io.openems.edge.weather.openmeteo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

public class WeatherForecastService {

	private static final String API_SCHEME = "https";
	private static final String API_HOST = "api.open-meteo.com";
	private static final String API_HOST_COMMERCIAL = "customer-api.open-meteo.com";
	private static final String API_VERSION = "v1";

	private static final int INTERNAL_ERROR = -1;

	private final Logger log = LoggerFactory.getLogger(WeatherForecastService.class);

	private final WeatherOpenMeteo parent;
	private final HttpBridgeTimeService timeService;
	private final String apiKey;
	private final int forecastDays;
	private final int pastDays;
	private final UrlBuilder baseUrl;
	private final WeatherDataParser weatherDataParser;

	private HttpBridgeTimeService.TimeEndpoint subscription;
	private Instant lastUpdate;
	private List<QuarterlyWeatherSnapshot> quarterlyWeatherForecast;
	private List<HourlyWeatherSnapshot> hourlyWeatherForecast;
	private List<DailyWeatherSnapshot> dailyWeatherForecast;

	public WeatherForecastService(//
			WeatherOpenMeteo parent, //
			HttpBridgeTimeService timeService, //
			String apiKey, //
			int forecastDays, //
			int pastDays, //
			WeatherDataParser weatherDataParser) {
		this.parent = parent;
		this.timeService = timeService;
		this.apiKey = apiKey;
		this.forecastDays = forecastDays;
		this.pastDays = pastDays;
		this.baseUrl = this.buildBaseUrl();
		this.weatherDataParser = weatherDataParser;
	}

	protected void subscribeToWeatherForecast(//
			OpenMeteoDelayTimeProvider delayTimeProvider, //
			Coordinates coordinates, //
			Supplier<Clock> clockSupplier, //
			Runnable onFetchWeatherForecastSuccess) {
		if (this.subscription != null) {
			this.timeService.removeTimeEndpoint(this.subscription);
			this.subscription = null;
		}

		if (coordinates == null) {
			this.log.error("Can't subscribe to weather forecast, coordinates are missing");
			return;
		}

		this.subscription = this.timeService.subscribeJsonTime(//
				delayTimeProvider, //
				() -> this.createForecastEndpoint(//
						coordinates, //
						clockSupplier), //
				response -> this.handleEndpointResponse(//
						response, //
						clockSupplier, //
						onFetchWeatherForecastSuccess), //
				error -> this.handleEndpointError(//
						error));
	}

	/**
	 * Deactivates the weather forecast subscription, if it exists. This stops
	 * receiving forecast updates from the HTTP bridge.
	 */
	public void deactivateForecastSubscription() {
		if (this.subscription != null) {
			this.timeService.removeTimeEndpoint(this.subscription);
			this.subscription = null;
		}
	}

	public List<QuarterlyWeatherSnapshot> getQuarterlyWeatherForecast() {
		return this.quarterlyWeatherForecast == null //
				? null //
				: new ArrayList<>(this.quarterlyWeatherForecast);
	}

	public List<HourlyWeatherSnapshot> getHourlyWeatherForecast() {
		return this.hourlyWeatherForecast == null //
				? null //
				: new ArrayList<>(this.hourlyWeatherForecast);
	}

	public List<DailyWeatherSnapshot> getDailyWeatherForecast() {
		return this.dailyWeatherForecast == null //
				? null //
				: new ArrayList<>(this.dailyWeatherForecast);
	}

	public Instant getLastUpdate() {
		return this.lastUpdate;
	}

	@VisibleForTesting
	String buildForecastUrl(Coordinates coordinates, ZoneId zone) {
		return this.baseUrl//
				.withQueryParam(ForecastQueryParams.LATITUDE, String.valueOf(coordinates.latitude()))//
				.withQueryParam(ForecastQueryParams.LONGITUDE, String.valueOf(coordinates.longitude()))//
				.withQueryParam(ForecastQueryParams.TIMEZONE, zone.toString())//
				.toEncodedString();
	}

	private Endpoint createForecastEndpoint(Coordinates coordinates, Supplier<Clock> clockSupplier) {
		String url = this.buildForecastUrl(coordinates, clockSupplier.get().getZone());

		return new Endpoint(//
				url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				Collections.emptyMap());
	}

	private void handleEndpointResponse(//
			HttpResponse<JsonElement> response, //
			Supplier<Clock> clockSupplier, //
			Runnable onFetchWeatherForecastSuccess) {
		this.parent._setHttpStatusCode(response.status().code());

		var responseJson = response.data().getAsJsonObject();
		var responseOffset = ZoneOffset.ofTotalSeconds(//
				responseJson.get(ForecastQueryParams.UTC_OFFSET_SECONDS).getAsInt());

		var clock = clockSupplier.get();
		var targetZone = clock.getZone();

		this.quarterlyWeatherForecast = this.weatherDataParser.parseQuarterly(//
				responseJson.getAsJsonObject(QuarterlyWeatherVariables.JSON_KEY), //
				responseOffset, //
				targetZone);

		this.hourlyWeatherForecast = this.weatherDataParser.parseHourly(//
				responseJson.getAsJsonObject(HourlyWeatherVariables.JSON_KEY), //
				responseOffset, //
				targetZone);

		this.dailyWeatherForecast = this.weatherDataParser.parseDaily(//
				responseJson.getAsJsonObject(DailyWeatherVariables.JSON_KEY));

		this.lastUpdate = Instant.now(clock);
		onFetchWeatherForecastSuccess.run();
	}

	private void handleEndpointError(HttpError error) {
		var httpStatusCode = switch (error) {
		case HttpError.ResponseError re -> re.status.code();
		default -> INTERNAL_ERROR;
		};

		this.parent._setHttpStatusCode(httpStatusCode);
		this.log.error(error.getMessage(), error);
	}

	private UrlBuilder buildBaseUrl() {
		var builder = UrlBuilder.create()//
				.withScheme(API_SCHEME)//
				.withHost(this.apiKey != null //
						? API_HOST_COMMERCIAL //
						: API_HOST)//
				.withPath("/" + API_VERSION + "/forecast")//
				.withQueryParam(ForecastQueryParams.FORECAST_DAYS, String.valueOf(this.forecastDays))//
				.withQueryParam(ForecastQueryParams.PAST_DAYS, String.valueOf(this.pastDays))//
				.withQueryParam(QuarterlyWeatherVariables.JSON_KEY, String.join(",", QuarterlyWeatherVariables.ALL))//
				.withQueryParam(HourlyWeatherVariables.JSON_KEY, String.join(",", HourlyWeatherVariables.ALL))//
				.withQueryParam(DailyWeatherVariables.JSON_KEY, String.join(",", DailyWeatherVariables.ALL));

		if (this.apiKey != null) {
			builder = builder.withQueryParam(ForecastQueryParams.API_KEY, this.apiKey);
		}

		return builder;
	}
}
