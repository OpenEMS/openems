package io.openems.edge.weather.openmeteo.forecast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.openmeteo.WeatherOpenMeteo;
import io.openems.edge.weather.openmeteo.WeatherOpenMeteoImpl;
import io.openems.edge.weather.openmeteo.data.DailyWeatherVariables;
import io.openems.edge.weather.openmeteo.data.HourlyWeatherVariables;
import io.openems.edge.weather.openmeteo.data.QuarterlyWeatherVariables;
import io.openems.edge.weather.openmeteo.data.WeatherDataParser;

@RunWith(MockitoJUnitRunner.class)
public class WeatherForecastServiceTest {

	private static final String DUMMY_API_KEY = "dummy-api-key";

	@Mock
	private WeatherOpenMeteo parent;

	@Mock
	private HttpBridgeTimeService httpBridge;

	@Mock
	private WeatherDataParser weatherDataParser;

	private WeatherForecastService weatherForecastService;

	@Before
	public void setUp() {
		this.weatherForecastService = new WeatherForecastService(//
				this.parent, //
				this.httpBridge, //
				DUMMY_API_KEY, //
				WeatherOpenMeteoImpl.FORECAST_DAYS, //
				WeatherOpenMeteoImpl.PAST_DAYS, //
				this.weatherDataParser);
	}

	@Test
	public void testSubscribeToWeatherForecast_ShouldReturn_WhenNoCoordinates() {
		this.weatherForecastService.subscribeToWeatherForecast(//
				mock(WeatherForecastDelayTimeProvider.class), //
				null, //
				Clock::systemUTC, //
				null);

		assertNull(this.weatherForecastService.getQuarterlyWeatherForecast());
		assertNull(this.weatherForecastService.getLastUpdate());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSubscribeToWeatherForecast_ShouldUpdateForecasts_WhenResponseSuccessful() throws Exception {
		var delayTimeProvider = mock(WeatherForecastDelayTimeProvider.class);
		var onResultCaptor = ArgumentCaptor.forClass(ThrowingConsumer.class);
		var clock = new TimeLeapClock();
		var callback = mock(Runnable.class);

		this.weatherForecastService.subscribeToWeatherForecast(//
				delayTimeProvider, //
				new Coordinates(48.8409, 12.9607), //
				() -> clock, //
				callback);

		// Capture success callback
		verify(this.httpBridge).subscribeJsonTime(//
				eq(delayTimeProvider), //
				any(Supplier.class), //
				onResultCaptor.capture(), //
				any(Consumer.class));

		// Prepare dummy JSON response
		var responseJson = new JsonObject();
		responseJson.add(ForecastQueryParams.UTC_OFFSET_SECONDS, new JsonPrimitive(0));
		responseJson.add(QuarterlyWeatherVariables.JSON_KEY, new JsonObject());
		responseJson.add(HourlyWeatherVariables.JSON_KEY, new JsonObject());
		responseJson.add(DailyWeatherVariables.JSON_KEY, new JsonObject());

		// Stub parser results
		var quarterlyWeatherForecast = List
				.of(new QuarterlyWeatherSnapshot(ZonedDateTime.now(), 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0));
		when(this.weatherDataParser.parseQuarterly(any(), any(), any())).thenReturn(quarterlyWeatherForecast);

		var hourlyWeatherForecast = List.of(new HourlyWeatherSnapshot(ZonedDateTime.now(), 0, 2.0, true));
		when(this.weatherDataParser.parseHourly(any(), any(), any())).thenReturn(hourlyWeatherForecast);

		var dailyWeatherForecast = List.of(new DailyWeatherSnapshot(LocalDate.now(), 0, 1.0, 2.0, 3.0));
		when(this.weatherDataParser.parseDaily(any())).thenReturn(dailyWeatherForecast);

		// Simulate successful HTTP response
		var httpResponse = HttpResponse.ok(responseJson);
		onResultCaptor.getValue().accept(httpResponse);

		// Assert HTTP status code
		var httpStatusCodeCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(this.parent)._setHttpStatusCode(httpStatusCodeCaptor.capture());
		assertEquals(HttpStatus.OK.code(), (int) httpStatusCodeCaptor.getValue());

		// Verify parser invocations
		verify(this.weatherDataParser).parseQuarterly(//
				any(JsonObject.class), //
				eq(ZoneOffset.ofTotalSeconds(0)), //
				eq(clock.getZone()));
		verify(this.weatherDataParser).parseHourly(//
				any(JsonObject.class), //
				eq(ZoneOffset.ofTotalSeconds(0)), //
				eq(clock.getZone()));
		verify(this.weatherDataParser).parseDaily(//
				any(JsonObject.class));

		// Assert forecasts
		assertEquals(quarterlyWeatherForecast, this.weatherForecastService.getQuarterlyWeatherForecast());
		assertEquals(hourlyWeatherForecast, this.weatherForecastService.getHourlyWeatherForecast());
		assertEquals(dailyWeatherForecast, this.weatherForecastService.getDailyWeatherForecast());

		// Assert last update
		assertEquals(clock.instant(), this.weatherForecastService.getLastUpdate());
		
		// Assert callback
		verify(callback).run();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSubscribeToWeatherForecast_ShouldSetHttpStatusCode_WhenErrorOccurs() throws Exception {
		var delayTimeProvider = mock(WeatherForecastDelayTimeProvider.class);
		var onErrorCaptor = ArgumentCaptor.forClass(Consumer.class);
		var clock = new TimeLeapClock();

		this.weatherForecastService.subscribeToWeatherForecast(//
				delayTimeProvider, //
				new Coordinates(48.8409, 12.9607), //
				() -> clock, //
				mock(Runnable.class));

		// Capture error callback
		verify(this.httpBridge).subscribeJsonTime(//
				eq(delayTimeProvider), //
				any(Supplier.class), //
				any(ThrowingConsumer.class), //
				onErrorCaptor.capture());

		// Trigger error response
		onErrorCaptor.getValue().accept(new HttpError.ResponseError(HttpStatus.BAD_GATEWAY, "Dummy error message"));

		// Assert HTTP status code
		var httpStatusCodeCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(this.parent)._setHttpStatusCode(httpStatusCodeCaptor.capture());
		assertEquals(HttpStatus.BAD_GATEWAY.code(), (int) httpStatusCodeCaptor.getValue());

		// Assert forecasts unset
		assertNull(this.weatherForecastService.getQuarterlyWeatherForecast());
		assertNull(this.weatherForecastService.getHourlyWeatherForecast());
		assertNull(this.weatherForecastService.getDailyWeatherForecast());

		// Assert last update unset
		assertNull(this.weatherForecastService.getLastUpdate());
	}

	@Test
	public void testBuildForecastUrl_ShouldBuildCorrectUrl_WhenApiKey() {
		var coordinates = new Coordinates(52.52, 13.405);
		var zone = ZoneId.of("Europe/Berlin");
		String url = this.weatherForecastService.buildForecastUrl(coordinates, zone);

		assertTrue(url.contains("https://customer-api.open-meteo.com/v1/forecast?"));
		assertTrue(url.contains("apikey=" + DUMMY_API_KEY));
		assertTrue(url.contains("latitude=" + coordinates.latitude()));
		assertTrue(url.contains("longitude=" + coordinates.longitude()));
		assertTrue(url.contains("timezone=" + zone.toString().replace("/", "%2F")));
		assertTrue(url.contains("forecast_days=" + WeatherOpenMeteoImpl.FORECAST_DAYS));
		assertTrue(url.contains("past_days=" + 1));
		assertTrue(url.contains("minutely_15=" + String.join("%2C", QuarterlyWeatherVariables.ALL)));
		assertTrue(url.contains("hourly=" + String.join("%2C", HourlyWeatherVariables.ALL)));
		assertTrue(url.contains("daily=" + String.join("%2C", DailyWeatherVariables.ALL)));
	}

	@Test
	public void testBuildForecastUrl_ShouldBuildCorrectUrl_WhenNoApiKey() {
		this.weatherForecastService = new WeatherForecastService(//
				this.parent, //
				this.httpBridge, //
				null, //
				WeatherOpenMeteoImpl.FORECAST_DAYS, //
				WeatherOpenMeteoImpl.PAST_DAYS, //
				this.weatherDataParser);

		var coordinates = new Coordinates(52.52, 13.405);
		var zone = ZoneId.of("Europe/Berlin");
		String url = this.weatherForecastService.buildForecastUrl(coordinates, zone);

		assertTrue(url.contains("https://api.open-meteo.com/v1/forecast?"));
		assertFalse(url.contains("apikey="));
		assertTrue(url.contains("latitude=" + coordinates.latitude()));
		assertTrue(url.contains("longitude=" + coordinates.longitude()));
		assertTrue(url.contains("timezone=" + zone.toString().replace("/", "%2F")));
		assertTrue(url.contains("forecast_days=" + WeatherOpenMeteoImpl.FORECAST_DAYS));
		assertTrue(url.contains("past_days=" + 1));
		assertTrue(url.contains("minutely_15=" + String.join("%2C", QuarterlyWeatherVariables.ALL)));
		assertTrue(url.contains("hourly=" + String.join("%2C", HourlyWeatherVariables.ALL)));
		assertTrue(url.contains("daily=" + String.join("%2C", DailyWeatherVariables.ALL)));
	}
}
