package io.openems.edge.weather.openmeteo.forecast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.openmeteo.WeatherOpenMeteo;

@RunWith(MockitoJUnitRunner.class)
public class WeatherForecastPersistenceServiceTest {

	@Mock
	private WeatherOpenMeteo parent;

	@Test
	public void testUpdateHourlyForecastChannels_ShouldSetCorrectChannelValues() throws Exception {
		final var sut = new WeatherForecastPersistenceService(//
				this.parent, //
				() -> Clock.systemDefaultZone());

		var hourlyForecast = new ArrayList<HourlyWeatherSnapshot>();
		var now = ZonedDateTime.now();
		for (int i = 0; i < 25; i++) {
			hourlyForecast.add(new HourlyWeatherSnapshot(//
					now.plusHours(i), //
					i, //
					10.0 + i, //
					false));
		}

		when(this.parent.getHourlyWeatherForecast(anyInt())).thenReturn(hourlyForecast);

		sut.updateHourlyForecastChannels();

		verify(this.parent)._setCurrentTemperature(10);
		verify(this.parent)._setTemperatureIn4h(14);
		verify(this.parent)._setTemperatureIn8h(18);
		verify(this.parent)._setTemperatureIn12h(22);
		verify(this.parent)._setTemperatureIn16h(26);
		verify(this.parent)._setTemperatureIn20h(30);
		verify(this.parent)._setTemperatureIn24h(34);

		verify(this.parent)._setCurrentWeatherCode(0);
		verify(this.parent)._setWeatherCodeIn4h(4);
		verify(this.parent)._setWeatherCodeIn8h(8);
		verify(this.parent)._setWeatherCodeIn12h(12);
		verify(this.parent)._setWeatherCodeIn16h(16);
		verify(this.parent)._setWeatherCodeIn20h(20);
		verify(this.parent)._setWeatherCodeIn24h(24);
	}

	@Test
	public void testUpdateHourlyForecastChannels_ShouldThrowException_WhenForecastNullOrTooShort() throws Exception {
		final var sut = new WeatherForecastPersistenceService(//
				this.parent, //
				() -> Clock.systemDefaultZone());

		// Case 1
		when(this.parent.getHourlyWeatherForecast(anyInt())).thenReturn(null);
		var exception1 = assertThrows(IllegalStateException.class, () -> {
			sut.updateHourlyForecastChannels();
		});
		assertEquals("Hourly weather forecast is null or too short", exception1.getMessage());

		// Case 2
		var hourlyForecast = new ArrayList<HourlyWeatherSnapshot>();
		var now = ZonedDateTime.now();
		for (int i = 0; i < 24; i++) {
			hourlyForecast.add(new HourlyWeatherSnapshot(//
					now.plusHours(i), //
					i, //
					10.0 + i, //
					false));
		}
		when(this.parent.getHourlyWeatherForecast(anyInt())).thenReturn(hourlyForecast);
		var exception2 = assertThrows(IllegalStateException.class, () -> {
			sut.updateHourlyForecastChannels();
		});
		assertEquals("Hourly weather forecast is null or too short", exception2.getMessage());
	}

	@Test
	public void testUpdateDailyForecastChannels_ShouldSetCorrectChannelValues() throws Exception {
		final var sut = new WeatherForecastPersistenceService(//
				this.parent, //
				() -> Clock.systemDefaultZone());

		var dailyForecast = new ArrayList<DailyWeatherSnapshot>();
		var now = LocalDate.now();
		for (int i = 0; i < 2; i++) {
			dailyForecast.add(new DailyWeatherSnapshot(//
					now.plusDays(i), //
					i, //
					10.0 + i, //
					20.0 + i, //
					30.0 + i));
		}

		when(this.parent.getDailyWeatherForecast()).thenReturn(dailyForecast);

		sut.updateDailyForecastChannels();

		verify(this.parent)._setTodaysMinTemperature(10);
		verify(this.parent)._setTodaysMaxTemperature(20);
		verify(this.parent)._setTodaysSunshineDuration(30);
	}

	@Test
	public void testUpdateDailyForecastChannels_ShouldThrowException_WhenForecastNullOrEmpty() throws Exception {
		final var sut = new WeatherForecastPersistenceService(//
				this.parent, //
				() -> Clock.systemDefaultZone());

		// Case 1
		when(this.parent.getDailyWeatherForecast()).thenReturn(null);
		var exception1 = assertThrows(IllegalStateException.class, () -> {
			sut.updateDailyForecastChannels();
		});
		assertEquals("Daily weather forecast is null or empty", exception1.getMessage());

		// Case 2
		var dailyForecast = new ArrayList<DailyWeatherSnapshot>();
		when(this.parent.getDailyWeatherForecast()).thenReturn(dailyForecast);
		var exception2 = assertThrows(IllegalStateException.class, () -> {
			sut.updateDailyForecastChannels();
		});
		assertEquals("Daily weather forecast is null or empty", exception2.getMessage());
	}

	@Test
	public void testResetAllForecastChannels_ShouldSetAllChannelsToNull() {
		final var sut = new WeatherForecastPersistenceService(//
				this.parent, //
				() -> Clock.systemDefaultZone());
		sut.resetAllForecastChannels();

		verify(this.parent)._setCurrentTemperature(null);
		verify(this.parent)._setTemperatureIn4h(null);
		verify(this.parent)._setTemperatureIn8h(null);
		verify(this.parent)._setTemperatureIn12h(null);
		verify(this.parent)._setTemperatureIn16h(null);
		verify(this.parent)._setTemperatureIn20h(null);
		verify(this.parent)._setTemperatureIn24h(null);

		verify(this.parent)._setCurrentWeatherCode(null);
		verify(this.parent)._setWeatherCodeIn4h(null);
		verify(this.parent)._setWeatherCodeIn8h(null);
		verify(this.parent)._setWeatherCodeIn12h(null);
		verify(this.parent)._setWeatherCodeIn16h(null);
		verify(this.parent)._setWeatherCodeIn20h(null);
		verify(this.parent)._setWeatherCodeIn24h(null);

		verify(this.parent)._setTodaysMinTemperature(null);
		verify(this.parent)._setTodaysMaxTemperature(null);
		verify(this.parent)._setTodaysSunshineDuration(null);
	}

	@Test
	public void testComputeInitialDelayToNextFullHour_ShouldReturnCorrectDelay() {
		var fixedTime = ZonedDateTime.of(2025, 9, 22, 14, 37, 0, 0, ZoneId.of("UTC"));
		var fixedClock = Clock.fixed(fixedTime.toInstant(), ZoneId.of("UTC"));

		var sut = new WeatherForecastPersistenceService(//
				null, //
				() -> fixedClock);

		long delayMinutes = sut.computeInitialDelayToNextFullHour();

		assertEquals(23, delayMinutes);
	}
}
