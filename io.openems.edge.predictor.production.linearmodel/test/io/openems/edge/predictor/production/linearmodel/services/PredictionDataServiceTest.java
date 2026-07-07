package io.openems.edge.predictor.production.linearmodel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionError;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

@RunWith(MockitoJUnitRunner.class)
public class PredictionDataServiceTest {

	@Mock
	private Weather weather;

	private ZonedDateTime now;
	private PredictionDataService sut;

	@Before
	public void setUp() throws Exception {
		this.now = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));

		var weatherData = List.of(//
				new QuarterlyWeatherSnapshot(//
						this.now, //
						1.0, //
						1.1, //
						1.2, //
						1.3, //
						1.4, //
						1.5, //
						1.6), //
				new QuarterlyWeatherSnapshot(//
						this.now.plusMinutes(15), //
						2.0, //
						2.1, //
						2.2, //
						2.3, //
						2.4, //
						2.5, //
						2.6)//
		);

		when(this.weather.getQuarterlyWeatherForecast(anyInt()))//
				.thenReturn(weatherData);

		this.sut = new PredictionDataService(//
				this.weather, //
				Clock.fixed(this.now.toInstant(), ZoneId.of("Europe/Berlin"))//
		);
	}

	@Test
	public void testPrepareFeatureMatrix_ShouldReturnExpectedMatrix() throws Exception {
		var result = this.sut.prepareFeatureMatrix(1);

		verify(this.weather).getQuarterlyWeatherForecast(eq(1));

		var expectedColumnNames = List.of(//
				ColumnNames.SHORTWAVE_RADIATION, //
				ColumnNames.DIRECT_RADIATION, //
				ColumnNames.DIRECT_NORMAL_IRRADIANCE, //
				ColumnNames.DIFFUSE_RADIATION, //
				ColumnNames.TEMPERATURE);
		assertEquals(expectedColumnNames, result.getColumnNames());

		var expectedIndex = List.of(//
				this.now, //
				this.now.plusMinutes(15));
		assertEquals(expectedIndex, result.getIndex());

		var expectedValues = List.of(//
				List.of(1.0, 1.1, 1.2, 1.3, 1.4), //
				List.of(2.0, 2.1, 2.2, 2.3, 2.4));
		assertEquals(expectedValues, result.getValues());
	}

	@Test
	public void testPrepareFeatureMatrix_ShouldThrowException_WhenNoWeatherDataAvailable() throws Exception {
		// Case 1
		when(this.weather.getQuarterlyWeatherForecast(anyInt()))//
				.thenReturn(null);
		var exception1 = assertThrows(PredictionException.class, () -> {
			this.sut.prepareFeatureMatrix(1);
		});
		assertEquals(PredictionError.NO_WEATHER_DATA, exception1.getError());
		assertEquals("Weather data is null or empty", exception1.getMessage());

		// Case 2
		when(this.weather.getQuarterlyWeatherForecast(anyInt()))//
				.thenReturn(new ArrayList<>());
		var exception2 = assertThrows(PredictionException.class, () -> {
			this.sut.prepareFeatureMatrix(1);
		});
		assertEquals(PredictionError.NO_WEATHER_DATA, exception2.getError());
		assertEquals("Weather data is null or empty", exception2.getMessage());
	}

	@Test
	public void testPrepareFeatureMatrix_ShouldThrowException_WhenWeatherDataHasGapsOrMissingIntervals()
			throws Exception {
		// Case 1
		when(this.weather.getQuarterlyWeatherForecast(anyInt()))//
				.thenReturn(List.of(//
						new QuarterlyWeatherSnapshot(//
								this.now.plusMinutes(1), //
								1.0, //
								1.1, //
								1.2, //
								1.3, //
								1.4, //
								1.5, //
								1.6), //
						new QuarterlyWeatherSnapshot(//
								this.now.plusMinutes(15), //
								2.0, //
								2.1, //
								2.2, //
								2.3, //
								2.4, //
								2.5, //
								2.6)//
				));
		var exception1 = assertThrows(PredictionException.class, () -> {
			this.sut.prepareFeatureMatrix(1);
		});
		assertEquals(PredictionError.INVALID_WEATHER_DATA, exception1.getError());
		assertEquals("Weather data has gaps or missing intervals", exception1.getMessage());

		// Case 2
		when(this.weather.getQuarterlyWeatherForecast(anyInt()))//
				.thenReturn(List.of(//
						new QuarterlyWeatherSnapshot(//
								this.now, //
								1.0, //
								1.1, //
								1.2, //
								1.3, //
								1.4, //
								1.5, //
								1.6), //
						new QuarterlyWeatherSnapshot(//
								this.now.plusMinutes(30), //
								2.0, //
								2.1, //
								2.2, //
								2.3, //
								2.4, //
								2.5, //
								2.6)//
				));

		var exception2 = assertThrows(PredictionException.class, () -> {
			this.sut.prepareFeatureMatrix(1);
		});
		assertEquals(PredictionError.INVALID_WEATHER_DATA, exception2.getError());
		assertEquals("Weather data has gaps or missing intervals", exception2.getMessage());
	}
}