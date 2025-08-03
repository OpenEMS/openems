package io.openems.edge.predictor.production.linearmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.FileNotFoundException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.SourceChannel;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;
import io.openems.edge.weather.test.DummyWeather;

public class PredictorLinearModelTest {

	private static final double DELTA = 0.1;
	private static final SourceChannel SOURCE = SourceChannel.UNMANAGED_PRODUCTION_ACTIVE_POWER;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testTrainModel_ShouldTrainSuccessfully() throws Exception {
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-24T00:00Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());
		final var timedata = new DummyTimedata("timedata0");

		var datetime = ZonedDateTime.of(2025, 3, 23, 0, 0, 0, 0, ZoneId.of("UTC"));

		var weatherSnapshots = new ArrayList<WeatherSnapshot>();
		var random = new Random(42);
		for (int i = 0; i < 96; i++) {
			double x1 = random.nextDouble() * 800;
			double x2 = random.nextDouble() * 400;

			weatherSnapshots.add(new WeatherSnapshot(x1, x2, 0.0, 0));
		}

		for (int i = 0; i < 96; i++) {
			var s = weatherSnapshots.get(i);
			int y = (int) Math.round(2 * s.globalHorizontalIrradiance() + 3 * s.directNormalIrradiance());

			var timestamp = datetime.plusMinutes(i * 15);
			timedata.add(timestamp, SOURCE.channelAddress, y);
		}

		var tempDirectory = this.tempFolder.newFolder("models").toPath();
		var localConfig = new DummyLocalConfig(tempDirectory, 96, 4);
		var modelSerializer = new ModelSerializer(tempDirectory);

		var componentManager = new DummyComponentManager(clock);
		var modelFitRunnable = new ModelFitRunnable(//
				componentManager, //
				timedata, //
				new DummyWeather("weather0") //
						.withHistoricalWeather(WeatherData.from(//
								datetime, weatherSnapshots.toArray(new WeatherSnapshot[0]))), //
				localConfig, //
				trainingState -> {
					// ignore SetTrainingState
				}, //
				modelSerializer, //
				SOURCE.channelAddress);
		modelFitRunnable.run();

		var modelConfigState = modelSerializer.readModelConfigState();
		var lastTrained = modelConfigState.lastTrainedDate();
		var betas = modelConfigState.betas();

		assertEquals(ZonedDateTime.now(clock), lastTrained);
		assertEquals(0, betas[0], DELTA);
		assertEquals(2, betas[1], DELTA);
		assertEquals(3, betas[2], DELTA);
	}

	@Test
	public void testTrainModel_ShouldThrowException_WhenInsufficientDataPoints() throws Exception {
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-24T00:00Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());
		final var timedata = new DummyTimedata("timedata0");

		var datetime = ZonedDateTime.of(2025, 3, 23, 0, 0, 0, 0, ZoneId.of("UTC"));

		var weatherSnapshots = new ArrayList<WeatherSnapshot>();
		var random = new Random(42);
		for (int i = 0; i < 96; i++) {
			double x1 = random.nextDouble() * 800;
			double x2 = random.nextDouble() * 400;

			weatherSnapshots.add(new WeatherSnapshot(x1, x2, 0.0, 0));
		}

		for (int i = 0; i < 96; i++) {
			var s = weatherSnapshots.get(i);
			int y = (int) Math.round(2 * s.globalHorizontalIrradiance() + 3 * s.directNormalIrradiance());

			var timestamp = datetime.plusMinutes(i * 15);
			timedata.add(timestamp, SOURCE.channelAddress, y);
		}

		var tempDirectory = this.tempFolder.newFolder("models").toPath();
		var localConfig = new DummyLocalConfig(tempDirectory, 107, 4);
		var modelSerializer = new ModelSerializer(tempDirectory);
		var componentManager = new DummyComponentManager(clock);
		// Set the training window to be too large for the available data
		var modelFitRunnable = new ModelFitRunnable(//
				componentManager, //
				timedata, //
				new DummyWeather("weather0") //
						.withHistoricalWeather(WeatherData.from(//
								datetime, weatherSnapshots.toArray(new WeatherSnapshot[0]))), //
				localConfig, //
				trainingState -> {
					// ignore SetTrainingState
				}, //
				modelSerializer, //
				SOURCE.channelAddress);
		modelFitRunnable.run();

		assertThrows(FileNotFoundException.class, () -> {
			modelSerializer.readModelConfigState();
		});
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnCorrectResult() throws Exception {
		final var tempDirectory = this.tempFolder.newFolder("test").toPath().resolve("models");
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-24T00:00Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());

		var betas = new double[] { 0., 1., 1., 1., 1., 1., 1., 1. };
		var modelConfigState = new ModelConfigState(ZonedDateTime.now(clock), betas);
		var serializer = new ModelSerializer(tempDirectory);
		serializer.saveModelConfigState(modelConfigState);

		var weatherData = WeatherData.from(//
				ZonedDateTime.now(clock), //
				new WeatherSnapshot(3., 8., 0., 0), //
				new WeatherSnapshot(4., 9., 0., 0) //
		);

		var sut = new PredictorProductionLinearModelImpl();

		new ComponentTest(sut) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("weather", new DummyWeather("weather0") //
						.withWeatherForecast(weatherData)) //
				.addReference("localConfig", new DummyLocalConfig(tempDirectory)) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setSourceChannel(SOURCE) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		var predictions = sut.getPrediction(SOURCE.channelAddress).asArray();

		assertEquals(11, (int) predictions[0]);
		assertEquals(13, (int) predictions[1]);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnNoNegativePrediction() throws Exception {
		final var tempDirectory = this.tempFolder.newFolder("test").toPath().resolve("models");
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-24T00:00Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());

		var betas = new double[] { 0., 1., 1., 1., 1., 1., 1., 1. };
		var modelConfigState = new ModelConfigState(ZonedDateTime.now(clock), betas);
		var serializer = new ModelSerializer(tempDirectory);
		serializer.saveModelConfigState(modelConfigState);

		var sut = new PredictorProductionLinearModelImpl();

		new ComponentTest(sut) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("weather", new DummyWeather("weather0") //
						.withWeatherForecast(WeatherData.from(//
								ZonedDateTime.now(clock), //
								// Set negative values, so the prediction would be negative
								new WeatherSnapshot(-3., -8., 0., 0)))) //
				.addReference("localConfig", new DummyLocalConfig(tempDirectory)) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setSourceChannel(SOURCE) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		var predictions = sut.getPrediction(SOURCE.channelAddress).asArray();

		assertEquals(0, (int) predictions[0]);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnEmptyPrediction_WhenModelIsTooOld() throws Exception {
		final var tempDirectory = this.tempFolder.newFolder("test").toPath().resolve("models");
		final var clock = new TimeLeapClock(
				ZonedDateTime.parse("2025-03-24T00:00Z", DateTimeFormatter.ISO_DATE_TIME).toInstant());

		var betas = new double[] { 0., 1., 1., 1., 1., 1., 1., 1. };
		// Set last trained date before threshold
		var lastTrainedDate = ZonedDateTime.now(clock).minusDays(15);
		var modelConfigState = new ModelConfigState(lastTrainedDate, betas);
		var serializer = new ModelSerializer(tempDirectory);
		serializer.saveModelConfigState(modelConfigState);

		var sut = new PredictorProductionLinearModelImpl();

		new ComponentTest(sut) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("weather", new DummyWeather("weather0").withWeatherForecast(WeatherData.from(//
						ZonedDateTime.now(clock), //
						new WeatherSnapshot(3., 8., 0., 0)))) //
				.addReference("localConfig", new DummyLocalConfig(tempDirectory)) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setSourceChannel(SOURCE) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		var prediction = sut.getPrediction(SOURCE.channelAddress);

		assertEquals(Prediction.EMPTY_PREDICTION, prediction);
	}
}
