package io.openems.edge.weather.openmeteo;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.HttpError.ResponseError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.meta.Coordinates;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;

public class OpenMeteoImplTest {

	private static final String WEATHER_ID = "weather0";
	private static final double DELTA = 1e-6;
	private static final long FORECAST_DAYS = 3;

	@Test
	public void testGetHistoricalWeather_SuccessfullyFetchesWeatherData() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			return HttpResponse.ok(JSON_RESPONSE);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var coordinates = new Coordinates(48.8409, 12.9607);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build()//
				);

		var result = sut.getHistoricalWeather(ZonedDateTime.now().minusDays(2), ZonedDateTime.now()).get();

		var timestamp1 = ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME);
		var snapshot1 = result.getAt(timestamp1);
		assertNotNull(snapshot1);
		assertEquals(8.6, snapshot1.temperature(), DELTA);
		assertEquals(0.0, snapshot1.globalHorizontalIrradiance(), DELTA);
		assertEquals(100.0, snapshot1.directNormalIrradiance(), DELTA);
		assertEquals(1, snapshot1.weatherCode());

		var timestamp2 = ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME);
		var snapshot2 = result.getAt(timestamp2);
		assertNotNull(snapshot2);
		assertEquals(8.4, snapshot2.temperature(), DELTA);
		assertEquals(2.0, snapshot2.globalHorizontalIrradiance(), DELTA);
		assertEquals(110.0, snapshot2.directNormalIrradiance(), DELTA);
		assertEquals(2, snapshot2.weatherCode());
	}

	@Test
	public void testGetHistoricalWeather_BuildsCorrectUrlForHistoricalData() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();

		final var coordinates = new Coordinates(48.8409, 12.9607);

		fetcher.addEndpointHandler(t -> {
			final var expectedBaseUrl = "https://historical-forecast-api.open-meteo.com/v1/forecast";
			final var expectedLatitudeParam = "latitude=" + coordinates.latitude();
			final var expectedLongitudeParam = "longitude=" + coordinates.longitude();
			final var expectedStartDateParam = "start_date=" + "2025-04-08";
			final var expectedEndDateParam = "end_date=" + "2025-04-08";
			final var expectedTimezoneParam = "timezone=UTC";
			final var expectedMinutelyParam = "minutely_15=shortwave_radiation%2Cdirect_normal_irradiance%2Ctemperature_2m%2Cweather_code";

			var url = t.url().toString();

			assertTrue("Base URL is incorrect", url.contains(expectedBaseUrl));
			assertTrue("Latitude is missing or incorrect", url.contains(expectedLatitudeParam));
			assertTrue("Longitude is missing or incorrect", url.contains(expectedLongitudeParam));
			assertTrue("Start date is missing or incorrect", url.contains(expectedStartDateParam));
			assertTrue("End date is missing or incorrect", url.contains(expectedEndDateParam));
			assertTrue("Timezone is missing or incorrect", url.contains(expectedTimezoneParam));
			assertTrue("Minutely parameter is missing or incorrect", url.contains(expectedMinutelyParam));

			return HttpResponse.ok(null);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build()//
				);

		sut.getHistoricalWeather(//
				ZonedDateTime.of(2025, 4, 8, 12, 0, 0, 0, ZoneId.of("UTC")), //
				ZonedDateTime.of(2025, 4, 8, 12, 0, 0, 0, ZoneId.of("UTC"))//
		);
	}

	@Test
	public void testGetHistoricalWeather_BuildsCorrectUrlForHistoricalData_WithApiKey() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();

		final var coordinates = new Coordinates(48.8409, 12.9607);

		fetcher.addEndpointHandler(t -> {
			final var expectedBaseUrl = "https://customer-historical-forecast-api.open-meteo.com/v1/forecast";
			final var expectedApiKeyParam = "apikey=dummyApiKey";

			var url = t.url().toString();
			assertTrue("Base URL is incorrect", url.contains(expectedBaseUrl));
			assertTrue("API Key is incorrect or missing", url.contains(expectedApiKeyParam));

			return HttpResponse.ok(null);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem() //
						.withOpenMeteoApiKey("dummyApiKey"))//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build()//
				);

		sut.getHistoricalWeather(//
				ZonedDateTime.of(2025, 4, 8, 12, 0, 0, 0, ZoneId.of("UTC")), //
				ZonedDateTime.of(2025, 4, 8, 12, 0, 0, 0, ZoneId.of("UTC"))//
		);
	}

	@Test
	public void testGetHistoricalWeather_ThrowsExceptionOnBadRequest() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			return new HttpResponse<String>(HttpStatus.BAD_REQUEST, null);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var coordinates = new Coordinates(48.8409, 12.9607);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build()//
				);

		var exception = assertThrows(ExecutionException.class, () -> {
			sut.getHistoricalWeather(ZonedDateTime.now().minusDays(2), ZonedDateTime.now()).get();
		});
		assertTrue(exception.getCause() instanceof ResponseError);
	}

	@Test
	public void testGetWeatherForecast_SuccessfullyFetchesWeatherData() throws Exception {
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			return HttpResponse.ok(JSON_RESPONSE);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var coordinates = new Coordinates(48.8409, 12.9607);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build())//
				.next(new TestCase()//
						.onBeforeProcessImage(() -> {
							executor.update();
						})//
				);

		var result = sut.getWeatherForecast();

		var timestamp1 = ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME);
		var snapshot1 = result.getAt(timestamp1);
		assertNotNull(snapshot1);
		assertEquals(8.6, snapshot1.temperature(), DELTA);
		assertEquals(0.0, snapshot1.globalHorizontalIrradiance(), DELTA);
		assertEquals(100.0, snapshot1.directNormalIrradiance(), DELTA);
		assertEquals(1, snapshot1.weatherCode());

		var timestamp2 = ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME);
		var snapshot2 = result.getAt(timestamp2);
		assertNotNull(snapshot2);
		assertEquals(8.4, snapshot2.temperature(), DELTA);
		assertEquals(2.0, snapshot2.globalHorizontalIrradiance(), DELTA);
		assertEquals(110.0, snapshot2.directNormalIrradiance(), DELTA);
		assertEquals(2, snapshot2.weatherCode());

		var httpStatusCodeChannel = sut.channels().stream()
				.filter(c -> c.address().toString().equals(WEATHER_ID + "/HttpStatusCode")).findFirst();
		assertEquals(HttpStatus.OK.code(), httpStatusCodeChannel.get().value().get());
	}

	@Test
	public void testGetWeatherForecast_ReturnsForecastOnlyWhenTimeIsAfterStartTime() throws Exception {
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			return HttpResponse.ok(JSON_RESPONSE);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var coordinates = new Coordinates(48.8409, 12.9607);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build())//
				.next(new TestCase()//
						.onBeforeProcessImage(() -> {
							executor.update();
						})//
				);

		var result = sut.getWeatherForecast();

		var timestamp1 = ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME);
		var snapshot1 = result.getAt(timestamp1);
		assertNull(snapshot1);

		var timestamp2 = ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME);
		var snapshot2 = result.getAt(timestamp2);
		assertNotNull(snapshot2);

		var httpStatusCodeChannel = sut.channels().stream()
				.filter(c -> c.address().toString().equals(WEATHER_ID + "/HttpStatusCode")).findFirst();
		assertEquals(HttpStatus.OK.code(), httpStatusCodeChannel.get().value().get());
	}

	@Test
	public void testSubscribeToWeatherForecast_HttpStatusCodeIsWrittenToChannelWhenApiCallFails() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			return new HttpResponse<String>(HttpStatus.BAD_REQUEST, null);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var coordinates = new Coordinates(48.8409, 12.9607);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build())//
				.next(new TestCase()//
						.onBeforeProcessImage(() -> {
							executor.update();
						})//
				);

		var httpStatusCodeChannel = sut.channels().stream()
				.filter(c -> c.address().toString().equals(WEATHER_ID + "/HttpStatusCode")).findFirst();
		assertEquals(HttpStatus.BAD_REQUEST.code(), httpStatusCodeChannel.get().value().get());
	}

	@Test
	public void testGetWeatherForecast_BuildsCorrectUrlForForecastedData() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();

		final var coordinates = new Coordinates(48.8409, 12.9607);

		fetcher.addEndpointHandler(t -> {
			final var expectedBaseUrl = "https://api.open-meteo.com/v1/forecast?";
			final var expectedLatitudeParam = "latitude=" + coordinates.latitude();
			final var expectedLongitudeParam = "longitude=" + coordinates.longitude();
			final var expectedTimezoneParam = "timezone=UTC";
			final var expectedMinutelyParam = "minutely_15=shortwave_radiation%2Cdirect_normal_irradiance%2Ctemperature_2m%2Cweather_code";
			final var expectedForecastDaysParam = "&forecast_days=" + FORECAST_DAYS;

			var url = t.url().toString();

			assertTrue("Base URL is incorrect", url.contains(expectedBaseUrl));
			assertTrue("Latitude is missing or incorrect", url.contains(expectedLatitudeParam));
			assertTrue("Longitude is missing or incorrect", url.contains(expectedLongitudeParam));
			assertTrue("Timezone is missing or incorrect", url.contains(expectedTimezoneParam));
			assertTrue("Minutely parameter is missing or incorrect", url.contains(expectedMinutelyParam));
			assertTrue("Forecast days parameter is missing or incorrect", url.contains(expectedForecastDaysParam));

			return HttpResponse.ok(null);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem()).activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build())//
				.next(new TestCase()//
						.onBeforeProcessImage(() -> {
							executor.update();
						})//
				);
	}

	@Test
	public void testGetWeatherForecast_BuildsCorrectUrlForForecastedData_WithApiKey() throws Exception {
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();

		final var coordinates = new Coordinates(48.8409, 12.9607);

		fetcher.addEndpointHandler(t -> {
			final var expectedBaseUrl = "https://customer-api.open-meteo.com/v1/forecast?";
			final var expectedApiKeyParam = "apikey=dummyApiKey";

			var url = t.url().toString();
			assertTrue("Base URL is incorrect", url.contains(expectedBaseUrl));
			assertTrue("API Key is incorrect or missing", url.contains(expectedApiKeyParam));

			return HttpResponse.ok(null);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut)//
				.addReference("httpBridgeFactory", factory)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates))//
				.addReference("oem", new DummyOpenemsEdgeOem() //
						.withOpenMeteoApiKey("dummyApiKey"))//
				.activate(MyConfig.create()//
						.setId(WEATHER_ID)//
						.build())//
				.next(new TestCase()//
						.onBeforeProcessImage(() -> {
							executor.update();
						})//
				);
	}

	@Test
	public void testGetCurrentWeather() throws Exception {
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-23T00:11Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(t -> {
			return HttpResponse.ok(JSON_RESPONSE);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		final var coordinates = new Coordinates(48.8409, 12.9607);

		final var sut = new WeatherOpenMeteoImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", factory) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("meta", new DummyMeta("meta0").withCoordinates(coordinates)) //
				.addReference("oem", new DummyOpenemsEdgeOem())//
				.activate(MyConfig.create() //
						.setId(WEATHER_ID) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							executor.update();
						})//
				);

		var snapshot = sut.getCurrentWeather();

		assertNotNull(snapshot);
		assertEquals(8.6, snapshot.temperature(), DELTA);
		assertEquals(0.0, snapshot.globalHorizontalIrradiance(), DELTA);
		assertEquals(100.0, snapshot.directNormalIrradiance(), DELTA);
		assertEquals(1, snapshot.weatherCode());
	}

	private static final String JSON_RESPONSE = """
			{
			  "timezone": "GMT",
			  "minutely_15": {
			    "time": [
			      "2025-03-23T00:00",
			      "2025-03-23T00:15"
			    ],
			    "temperature_2m": [
			      8.6,
			      8.4
			    ],
			    "shortwave_radiation": [
			      0.0,
			      2.0
			    ],
			    "direct_normal_irradiance": [
			      100.0,
			      110.0
			    ],
			    "weather_code": [
			      1,
			      2
			    ]
			  }
			}
			""".stripIndent();
}
