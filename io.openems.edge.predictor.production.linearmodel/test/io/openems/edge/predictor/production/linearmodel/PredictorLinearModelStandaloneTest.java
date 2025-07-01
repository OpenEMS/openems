package io.openems.edge.predictor.production.linearmodel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.OpenemsConstants;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.predictor.api.prediction.SourceChannel;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;
import io.openems.edge.weather.test.DummyWeather;

/**
 * Test class for running the model with custom local data.
 * 
 * <p>
 * To use this test, create a folder named {@code data} inside the test
 * directory. Place the following CSV files in the {@code data} folder:
 * <ul>
 * <li>{@code X_train.csv}</li>
 * <li>{@code X_test.csv}</li>
 * <li>{@code y_train.csv}</li>
 * <li>{@code y_test.csv}</li>
 * </ul>
 * 
 * <p>
 * The {@code X_*.csv} files must contain the columns:
 * <ul>
 * <li>{@code time} – in the format {@code yyyy-MM-dd HH:mm:ssXXX} (e.g.,
 * {@code 2025-01-01 12:30:00+00:00})</li>
 * <li>{@code direct_normal_irradiance}</li>
 * <li>{@code global_horizontal_irradiance}</li>
 * </ul>
 * 
 * <p>
 * The {@code y_*.csv} files must contain the columns:
 * <ul>
 * <li>{@code time}</li>
 * <li>{@code production}</li>
 * </ul>
 */
public class PredictorLinearModelStandaloneTest {

	private static final ChannelAddress CHANNEL = SourceChannel.UNMANAGED_PRODUCTION_ACTIVE_POWER.channelAddress;
	private static final Path MODELS_DIRECTORY_PATH = Paths.get(OpenemsConstants.getOpenemsDataDir(), "models");
	private static final ZonedDateTime NOW = ZonedDateTime.of(2025, 3, 24, 23, 45, 0, 0, ZoneId.of("UTC"));
	private static final String DATA_FOLDER = "./test/data/";

	/**
	 * Tests training the model using local CSV input data.
	 *
	 * @throws Exception if an error occurs during training
	 */
	@Test
	@Ignore
	public void testTrainModel() throws Exception {
		final var clock = new TimeLeapClock(NOW.toInstant());
		var timedata = new DummyTimedata("timedata0");
		var localConfig = new DummyLocalConfig(MODELS_DIRECTORY_PATH);

		var records = readProduction("y_train.csv");
		for (Object[] record : records) {
			timedata.add((ZonedDateTime) record[0], CHANNEL, (int) record[1]);
		}

		var modelSerializer = new ModelSerializer(localConfig.modelsDirectoryPath());

		var modelFitRunnable = new ModelFitRunnable(//
				new DummyComponentManager(clock), //
				timedata, //
				new DummyWeather("weather0") //
						.withHistoricalWeather(readWeatherData("X_train.csv")), //
				localConfig, //
				trainingState -> {
					// ignore SetTrainingState
				}, //
				modelSerializer, //
				CHANNEL);
		modelFitRunnable.run();
	}

	/**
	 * Tests predicting with a trained model using test CSV input data.
	 *
	 * @throws Exception if an error occurs during prediction
	 */
	@Test
	@Ignore
	public void testPredict() throws Exception {
		final var clock = new TimeLeapClock(NOW.toInstant());
		var timedata = new DummyTimedata("timedata0");

		var sut = new PredictorProductionLinearModelImpl();

		new ComponentTest(sut) //
				.addReference("timedata", timedata) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("weather", new DummyWeather("weather0") //
						.withWeatherForecast(readWeatherData("X_test.csv"))) //
				.addReference("localConfig",
						new DummyLocalConfig(Paths.get(OpenemsConstants.getOpenemsDataDir(), "models"))) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		var prediction = sut.getPrediction(CHANNEL);

		var records = readProduction("y_test.csv");
		var yTest = new Integer[records.size()];
		for (var i = 0; i < records.size(); i++) {
			yTest[i] = (int) records.get(i)[1];
		}

		var yPred = prediction.asArray();
		var r2 = r2(yTest, yPred);
		var mae = mae(yTest, yPred);

		System.out.println("Model performance on test data:");
		System.out.println("R2: " + r2);
		System.out.println("MAE: " + mae);
	}

	private static WeatherData readWeatherData(String filename) throws IOException {
		var csvFile = DATA_FOLDER + filename;
		var mapBuilder = ImmutableSortedMap.<ZonedDateTime, WeatherSnapshot>naturalOrder();

		try (var br = new BufferedReader(new FileReader(csvFile))) {
			// Skip header
			br.readLine();

			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");

				var time = ZonedDateTime.parse(values[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
				double temperature = Double.parseDouble(values[1]);
				double directNormalIrradiance = Double.parseDouble(values[5]);
				double shortwaveRadiation = Double.parseDouble(values[6]);

				var weatherSnapshot = new WeatherSnapshot(shortwaveRadiation, directNormalIrradiance, temperature, -1);
				mapBuilder.put(time, weatherSnapshot);
			}
		}

		return WeatherData.from(mapBuilder.build());
	}

	private static List<Object[]> readProduction(String filename) throws IOException {
		String csvFile = DATA_FOLDER + filename;
		String line;
		String cvsSplitBy = ",";
		var records = new ArrayList<Object[]>();
		try (var br = new BufferedReader(new FileReader(csvFile))) {
			// Skip header
			br.readLine();

			while ((line = br.readLine()) != null) {
				String[] data = line.split(cvsSplitBy);

				var timeString = data[0].trim();
				var productionString = data[1].trim();

				var time = ZonedDateTime.parse(timeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));

				int production = (int) Math.round(Double.parseDouble(productionString));

				records.add(new Object[] { time, production });
			}
		}

		return records;
	}

	private static double r2(Integer[] actual, Integer[] predicted) {
		if (actual.length != predicted.length) {
			throw new IllegalArgumentException("Arrays must have the same length");
		}

		double meanActual = 0.0;
		double totalSumSquares = 0.0;
		double residualSumSquares = 0.0;

		for (var value : actual) {
			meanActual += value;
		}

		meanActual /= actual.length;

		for (int i = 0; i < actual.length; i++) {
			totalSumSquares += Math.pow(actual[i] - meanActual, 2);
			residualSumSquares += Math.pow(actual[i] - predicted[i], 2);
		}

		return 1 - (residualSumSquares / totalSumSquares);
	}

	private static double mae(Integer[] actual, Integer[] predicted) {
		if (actual.length != predicted.length) {
			throw new IllegalArgumentException("Arrays must have the same length");
		}

		double sumOfErrors = 0.0;

		for (int i = 0; i < actual.length; i++) {
			sumOfErrors += Math.abs(actual[i] - predicted[i]);
		}

		return sumOfErrors / actual.length;
	}
}
