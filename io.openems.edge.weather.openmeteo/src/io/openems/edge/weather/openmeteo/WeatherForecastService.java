package io.openems.edge.weather.openmeteo;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpTime.TimeEndpoint;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.meta.Coordinates;
import io.openems.edge.weather.api.WeatherData;

public class WeatherForecastService {

	private static final String API_SCHEME = "https";
	private static final String API_HOST = "api.open-meteo.com";
	private static final String API_HOST_COMMERCIAL = "customer-api.open-meteo.com";
	private static final String API_VERSION = "v1";
	private static final String PAST_DAYS = "1";

	private static final int INTERNAL_ERROR = -1;

	private final Logger log = LoggerFactory.getLogger(WeatherForecastService.class);

	private final BridgeHttp httpBridge;
	private final String[] weatherVariables;
	private final long forecastDays;
	private final String apiKey;
	private final UrlBuilder baseUrl;

	private TimeEndpoint subscription;
	private WeatherData weatherForecast = WeatherData.EMPTY_WEATHER_DATA;

	public WeatherForecastService(BridgeHttp httpBridge, String[] weatherVariables, long forecastDays, String apiKey) {
		super();
		this.httpBridge = httpBridge;
		this.weatherVariables = weatherVariables;
		this.forecastDays = forecastDays;
		this.apiKey = apiKey;
		this.baseUrl = this.buildBaseUrl();
	}

	protected void subscribeToWeatherForecast(OpenMeteoDelayTimeProvider delayTimeProvider,
			Channel<Integer> httpStatusCodeChannel, Optional<Coordinates> coordinates, ZoneId zone) {
		if (this.subscription != null) {
			this.httpBridge.removeTimeEndpoint(this.subscription);
			this.subscription = null;
		}

		if (coordinates.isEmpty()) {
			this.log.error("Can't subscribe to weather forecast, coordinates are missing");
			return;
		}

		this.subscription = this.httpBridge.subscribeJsonTime(//
				delayTimeProvider, //
				this.createForecastEndpoint(//
						coordinates.get()//
				), //
				response -> this.handleEndpointResponse(//
						response, //
						httpStatusCodeChannel, //
						zone//
				), //
				error -> this.handleEndpointError(//
						error, //
						httpStatusCodeChannel//
				)//
		);
	}

	public WeatherData getWeatherForecast() {
		return this.weatherForecast;
	}

	private void handleEndpointResponse(HttpResponse<JsonElement> response, Channel<Integer> httpStatusCodeChannel,
			ZoneId zone) {
		httpStatusCodeChannel.setNextValue(response.status().code());

		this.weatherForecast = Utils.parseWeatherDataFromJson(response.data(), this.weatherVariables, zone);
	}

	private void handleEndpointError(HttpError error, Channel<Integer> httpStatusCodeChannel) {
		var httpStatusCode = switch (error) {
		case HttpError.ResponseError re -> re.status.code();
		default -> INTERNAL_ERROR;
		};

		httpStatusCodeChannel.setNextValue(httpStatusCode);
		this.log.error(error.getMessage(), error);
	}

	private Endpoint createForecastEndpoint(Coordinates coordinates) {
		String url = this.buildForecastUrl(coordinates, ZoneId.of("UTC"));

		return new Endpoint(url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				Collections.emptyMap());
	}

	private UrlBuilder buildBaseUrl() {
		return UrlBuilder.create()//
				.withScheme(API_SCHEME)//
				.withHost(this.apiKey != null //
						? API_HOST_COMMERCIAL //
						: API_HOST)//
				.withPath("/" + API_VERSION + "/forecast")//
				.withQueryParam("apikey", this.apiKey != null //
						? this.apiKey //
						: "")//
				.withQueryParam("forecast_days", String.valueOf(this.forecastDays))//
				.withQueryParam("past_days", PAST_DAYS)//
				.withQueryParam("minutely_15", String.join(",", this.weatherVariables));
	}

	private String buildForecastUrl(Coordinates coordinates, ZoneId zone) {
		return this.baseUrl//
				.withQueryParam("latitude", String.valueOf(coordinates.latitude()))//
				.withQueryParam("longitude", String.valueOf(coordinates.longitude()))//
				.withQueryParam("timezone", zone.toString())//
				.toEncodedString();
	}
}
