package io.openems.edge.predictor.production.linearmodel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;
import io.openems.edge.predictor.production.linearmodel.training.TrainingError;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

@RunWith(MockitoJUnitRunner.class)
public class TrainingDataServiceTest {

	private static final ChannelAddress PRODUCTION_CHANNEL_ADDRESS = new ChannelAddress("_sum", "ProductionChannel");

	@Mock
	private Weather weather;

	private ZonedDateTime from;
	private DummyTimedata timedata;
	private TrainingDataService sut;

	@Before
	public void setUp() {
		this.from = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));

		this.timedata = new DummyTimedata("timedata0");
		this.timedata.add(this.from.plusMinutes(15), PRODUCTION_CHANNEL_ADDRESS, 100);
		this.timedata.add(this.from.plusMinutes(30), PRODUCTION_CHANNEL_ADDRESS, 200);
		this.timedata.add(this.from.plusMinutes(45), PRODUCTION_CHANNEL_ADDRESS, -300);
		this.timedata.add(this.from.plusMinutes(60), PRODUCTION_CHANNEL_ADDRESS, 400);

		var weatherData = List.of(//
				new QuarterlyWeatherSnapshot(//
						this.from, //
						1.0, //
						1.1, //
						1.2, //
						1.3, //
						1.4, //
						1.5, //
						0.0), //
				new QuarterlyWeatherSnapshot(//
						this.from.plusMinutes(15), //
						2.0, //
						2.1, //
						2.2, //
						2.3, //
						2.4, //
						2.5, //
						0.019), // snow depth < 0.02
				new QuarterlyWeatherSnapshot(//
						this.from.plusMinutes(30), //
						3.0, //
						3.1, //
						3.2, //
						3.3, //
						3.4, //
						3.5, //
						0.2), // snow depth >= 0.02
				new QuarterlyWeatherSnapshot(//
						this.from.plusMinutes(45), //
						4.0, //
						4.1, //
						4.2, //
						4.3, //
						4.4, //
						4.5, //
						0.0), //
				new QuarterlyWeatherSnapshot(//
						this.from.plusMinutes(60), //
						5.0, //
						5.1, //
						Double.NaN, //
						5.3, //
						5.4, //
						5.5, //
						0.0)//
		);

		when(this.weather.getHistoricalWeather(any(), any(), any()))//
				.thenReturn(CompletableFuture.completedFuture(weatherData));

		this.sut = new TrainingDataService(//
				this.weather, //
				this.timedata, //
				PRODUCTION_CHANNEL_ADDRESS);
	}

	@Test
	public void testPrepareFeatureTargetMatrix_ShouldReturnExpectedMatrix() throws Exception {
		var to = this.from.plusDays(1);
		var result = this.sut.prepareFeatureTargetMatrix(this.from, to);

		verify(this.weather).getHistoricalWeather(//
				eq(this.from.toLocalDate()), //
				eq(to.toLocalDate()), //
				eq(this.from.getZone()));

		var expectedColumnNames = List.of(//
				ColumnNames.SHORTWAVE_RADIATION, //
				ColumnNames.DIRECT_RADIATION, //
				ColumnNames.DIRECT_NORMAL_IRRADIANCE, //
				ColumnNames.DIFFUSE_RADIATION, //
				ColumnNames.TEMPERATURE, //
				ColumnNames.TARGET);
		assertEquals(expectedColumnNames, result.getColumnNames());

		var expectedIndex = List.of(//
				this.from.plusMinutes(15));
		assertEquals(expectedIndex, result.getIndex());

		var expectedValues = List.of(//
				List.of(2.0, 2.1, 2.2, 2.3, 2.4, 100.0));
		assertEquals(expectedValues, result.getValues());
	}

	@Test
	public void testPrepareFeatureMatrix_ShouldThrowException_WhenNoWeatherDataAvailable() {
		var to = this.from.plusDays(1);

		// Case 1
		when(this.weather.getHistoricalWeather(any(), any(), any()))//
				.thenReturn(CompletableFuture.completedFuture(null));
		var exception1 = assertThrows(TrainingException.class, () -> {
			this.sut.prepareFeatureTargetMatrix(this.from, to);
		});
		assertEquals(TrainingError.NO_WEATHER_DATA, exception1.getError());
		assertEquals("Weather data is null or empty", exception1.getMessage());

		// Case 2
		when(this.weather.getHistoricalWeather(any(), any(), any()))//
				.thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
		var exception2 = assertThrows(TrainingException.class, () -> {
			this.sut.prepareFeatureTargetMatrix(this.from, to);
		});
		assertEquals(TrainingError.NO_WEATHER_DATA, exception2.getError());
		assertEquals("Weather data is null or empty", exception2.getMessage());
	}

	@Test
	public void testPrepareFeatureMatrix_ShouldThrowException_WhenNoProductionDataAvailable() throws Exception {
		final var to = this.from.plusDays(1);

		this.timedata = mock(DummyTimedata.class);
		this.sut = new TrainingDataService(//
				this.weather, //
				this.timedata, //
				PRODUCTION_CHANNEL_ADDRESS);

		// Case 1
		when(this.timedata.queryHistoricData(any(), any(), any(), any(), any()))//
				.thenReturn(null);
		var exception1 = assertThrows(TrainingException.class, () -> {
			this.sut.prepareFeatureTargetMatrix(this.from, to);
		});
		assertEquals(TrainingError.NO_PRODUCTION_DATA, exception1.getError());
		assertEquals("Production data is null or empty", exception1.getMessage());

		// Case 2
		when(this.timedata.queryHistoricData(any(), any(), any(), any(), any()))//
				.thenReturn(new TreeMap<>());
		var exception2 = assertThrows(TrainingException.class, () -> {
			this.sut.prepareFeatureTargetMatrix(this.from, to);
		});
		assertEquals(TrainingError.NO_PRODUCTION_DATA, exception2.getError());
		assertEquals("Production data is null or empty", exception2.getMessage());
	}
}