package io.openems.edge.weather.openmeteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.openmeteo.forecast.WeatherForecastService;
import io.openems.edge.weather.openmeteo.historical.HistoricalWeatherService;

@RunWith(MockitoJUnitRunner.class)
public class WeatherOpenMeteoImplTest {

	@Test
	public void testGetCurrentWeather_ShouldReturnCurrentSnapshot() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getQuarterlyWeatherForecast()).thenReturn(quarterlyWeatherSnapshots(clock));
		when(weatherForecastService.getLastUpdate()).thenReturn(clock.instant());

		var result = sut.getCurrentWeather();

		assertEquals(quarterlyWeatherSnapshots(clock).get(1), result);
	}

	@Test
	public void testGetHistoricalWeather_ShouldFetchHistoricalSnapshots_WhenServiceAvailable() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var historicalWeatherService = mock(HistoricalWeatherService.class);
		sut.setHistoricalWeatherService(historicalWeatherService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("meta", new DummyMeta("meta0"));

		var expected = quarterlyWeatherSnapshots(clock);

		when(historicalWeatherService.getWeatherData(any(), any(), any(), any()))
				.thenReturn(CompletableFuture.completedFuture(expected));

		var result = sut.getHistoricalWeather(//
				LocalDate.now(clock), //
				LocalDate.now(clock), //
				ZoneId.of("UTC"))//
				.get();

		assertEquals(expected, result);
	}

	@Test
	public void testGetHistoricalWeather_ShouldReturnFailedFuture_WhenNoServiceAvailable() throws Exception {
		var sut = new WeatherOpenMeteoImpl();

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("meta", new DummyMeta("meta0"));

		var result = sut.getHistoricalWeather(//
				LocalDate.now(clock), //
				LocalDate.now(clock), //
				ZoneId.of("UTC"));

		assertTrue(result.isCompletedExceptionally());
	}

	@Test
	public void testGetQuarterlyWeatherForecast_ShouldReturnQuarterlySnapshots() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getQuarterlyWeatherForecast()).thenReturn(quarterlyWeatherSnapshots(clock));
		when(weatherForecastService.getLastUpdate()).thenReturn(clock.instant());

		int forecastQuarters = 1;
		var result = sut.getQuarterlyWeatherForecast(forecastQuarters);

		assertEquals(2, result.size());

		assertEquals(quarterlyWeatherSnapshots(clock).get(1), result.get(0));
		assertEquals(quarterlyWeatherSnapshots(clock).get(2), result.get(1));
	}

	@Test
	public void testGetHourlyWeatherForecast_ShouldReturnHourlySnapshots() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getHourlyWeatherForecast()).thenReturn(hourlyWeatherSnapshots(clock));
		when(weatherForecastService.getLastUpdate()).thenReturn(clock.instant());

		int forecastHours = 1;
		var result = sut.getHourlyWeatherForecast(forecastHours);

		assertEquals(2, result.size());

		assertEquals(hourlyWeatherSnapshots(clock).get(1), result.get(0));
		assertEquals(hourlyWeatherSnapshots(clock).get(2), result.get(1));
	}

	@Test
	public void testGetDailyWeatherForecast_ShouldReturnDailySnapshots() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getDailyWeatherForecast()).thenReturn(dailyWeatherSnapshots(clock));
		when(weatherForecastService.getLastUpdate()).thenReturn(clock.instant());

		var result = sut.getDailyWeatherForecast();

		assertEquals(3, result.size());

		assertEquals(dailyWeatherSnapshots(clock).get(1), result.get(0));
		assertEquals(dailyWeatherSnapshots(clock).get(2), result.get(1));
		assertEquals(dailyWeatherSnapshots(clock).get(3), result.get(2));
	}

	@Test
	public void testAssertWeatherForecastServiceAvailable_ShouldThrowException_WhenNoServiceAvailable() {
		var sut = new WeatherOpenMeteoImpl();
		var exception = assertThrows(IllegalStateException.class, () -> {
			sut.getQuarterlyWeatherForecast(2);
		});
		assertEquals(
				"WeatherForecastService is not available. This may happen if the weather API component is not enabled or configured",
				exception.getMessage());
	}

	@Test
	public void testAssertWeatherForecastValid_ShouldThrowException_WhenForecastNull() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getQuarterlyWeatherForecast()).thenReturn(null);
		when(weatherForecastService.getLastUpdate()).thenReturn(clock.instant());

		var exception = assertThrows(OpenemsException.class, () -> {
			sut.getQuarterlyWeatherForecast(2);
		});
		assertEquals("Weather forecast data is unavailable or outdated", exception.getMessage());
	}

	@Test
	public void testAssertWeatherForecastValid_ShouldThrowException_WhenLastUpdateNull() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getQuarterlyWeatherForecast()).thenReturn(quarterlyWeatherSnapshots(clock));
		when(weatherForecastService.getLastUpdate()).thenReturn(null);

		var exception = assertThrows(OpenemsException.class, () -> {
			sut.getQuarterlyWeatherForecast(2);
		});
		assertEquals("Weather forecast data is unavailable or outdated", exception.getMessage());
	}

	@Test
	public void testAssertWeatherForecastValid_ShouldThrowException_WhenForecastTooOld() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));

		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		when(weatherForecastService.getQuarterlyWeatherForecast()).thenReturn(quarterlyWeatherSnapshots(clock));
		when(weatherForecastService.getLastUpdate())
				.thenReturn(clock.instant().minus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS));

		var exception = assertThrows(OpenemsException.class, () -> {
			sut.getQuarterlyWeatherForecast(2);
		});
		assertEquals("Weather forecast data is unavailable or outdated", exception.getMessage());
	}

	@Test
	public void testUpdatedMeta_ShouldSubscribeToWeatherForecast_WhenCoordinatesChange() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("meta", new DummyMeta("meta0"));

		var meta1 = mock(Meta.class);
		when(meta1.getCoordinates()).thenReturn(Coordinates.of(1.0, 2.0));
		sut.bindMeta(meta1);

		var meta2 = mock(Meta.class);
		when(meta2.getCoordinates()).thenReturn(Coordinates.of(3.0, 4.0));
		sut.updatedMeta(meta2);

		verify(weatherForecastService).subscribeToWeatherForecast(any(), eq(Coordinates.of(3, 4)), any(), any());
	}

	@Test
	public void testUpdatedMeta_ShouldNotSubscribe_WhenCoordinatesSame() throws Exception {
		var sut = new WeatherOpenMeteoImpl();
		var weatherForecastService = mock(WeatherForecastService.class);
		sut.setWeatherForecastService(weatherForecastService);

		var clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
		new ComponentTest(sut)//
				.addReference("componentManager", new DummyComponentManager(clock));

		var meta1 = mock(Meta.class);
		when(meta1.getCoordinates()).thenReturn(Coordinates.of(1.0, 2.0));
		sut.bindMeta(meta1);

		sut.updatedMeta(meta1);

		verify(weatherForecastService, never()).subscribeToWeatherForecast(any(), any(), any(), any());
	}

	private static List<QuarterlyWeatherSnapshot> quarterlyWeatherSnapshots(Clock clock) {
		return List.of(//
				new QuarterlyWeatherSnapshot(//
						ZonedDateTime.now(clock).minusMinutes(15), //
						-1.1, //
						-1.2, //
						-1.3, //
						-1.4, //
						-1.5, //
						-1.6, //
						-1.7), //
				new QuarterlyWeatherSnapshot(//
						ZonedDateTime.now(clock), //
						1.1, //
						1.2, //
						1.3, //
						1.4, //
						1.5, //
						1.6, //
						1.7), //
				new QuarterlyWeatherSnapshot(//
						ZonedDateTime.now(clock).plusMinutes(15), //
						2.1, //
						2.2, //
						2.3, //
						2.4, //
						2.5, //
						2.6, //
						2.7), //
				new QuarterlyWeatherSnapshot(//
						ZonedDateTime.now(clock).plusMinutes(30), //
						3.1, //
						3.2, //
						3.3, //
						3.4, //
						3.5, //
						3.6, //
						3.7));
	}

	private static List<HourlyWeatherSnapshot> hourlyWeatherSnapshots(Clock clock) {
		return List.of(//
				new HourlyWeatherSnapshot(//
						ZonedDateTime.now(clock).minusHours(1), //
						10, //
						100.0, //
						true), //
				new HourlyWeatherSnapshot(//
						ZonedDateTime.now(clock), //
						0, //
						10.0, //
						true), //
				new HourlyWeatherSnapshot(//
						ZonedDateTime.now(clock).plusHours(1), //
						1, //
						12.0, //
						false), //
				new HourlyWeatherSnapshot(//
						ZonedDateTime.now(clock).plusHours(2), //
						3, //
						13.0, //
						true));
	}

	private static List<DailyWeatherSnapshot> dailyWeatherSnapshots(Clock clock) {
		return List.of(//
				new DailyWeatherSnapshot(//
						LocalDate.now(clock).minusDays(1), //
						10, //
						100.1, //
						100.2, //
						100.3), //
				new DailyWeatherSnapshot(//
						LocalDate.now(clock), //
						0, //
						10.1, //
						10.2, //
						10.3), //
				new DailyWeatherSnapshot(//
						LocalDate.now(clock).plusDays(1), //
						1, //
						10.1, //
						10.2, //
						10.3), //
				new DailyWeatherSnapshot(//
						LocalDate.now(clock).plusDays(2), //
						3, //
						10.1, //
						10.2, //
						10.3));
	}
}
