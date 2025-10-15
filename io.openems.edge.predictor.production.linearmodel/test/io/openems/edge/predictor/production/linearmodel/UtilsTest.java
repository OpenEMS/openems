package io.openems.edge.predictor.production.linearmodel;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

public class UtilsTest {

	private static final double DELTA = 1e-6;

	@Test
	public void testGetWeatherDataFeatureMatrix_ShouldExtractCorrectFeatures() {
		var weatherData = List.of(//
				new QuarterlyWeatherSnapshot(ZonedDateTime.parse("2025-04-08T08:00:00Z"), 100.0, 150.0), //
				new QuarterlyWeatherSnapshot(ZonedDateTime.parse("2025-04-08T09:00:00Z"), 110.0, 160.0));

		String[] inputFeatures = { "global_horizontal_irradiance", "direct_normal_irradiance" };

		double[][] featureMatrix = Utils.getWeatherDataFeatureMatrix(weatherData, inputFeatures, false);

		assertEquals(2, featureMatrix.length);
		assertEquals(2, featureMatrix[0].length);

		assertEquals(100.0, featureMatrix[0][0], DELTA);
		assertEquals(150.0, featureMatrix[0][1], DELTA);

		assertEquals(110.0, featureMatrix[1][0], DELTA);
		assertEquals(160.0, featureMatrix[1][1], DELTA);
	}

	@Test
	public void testGetWeatherDataFeatureMatrix_ShouldConstructCorrectDaytimeFeatures() {
		var weatherData = List.of(//
				new QuarterlyWeatherSnapshot(ZonedDateTime.parse("2025-04-08T08:00:00Z"), 100.0, 150.0), //
				new QuarterlyWeatherSnapshot(ZonedDateTime.parse("2025-04-08T10:00:00Z"), 110.0, 160.0));

		String[] inputFeatures = { "global_horizontal_irradiance", "direct_normal_irradiance" };

		double[][] featureMatrix = Utils.getWeatherDataFeatureMatrix(weatherData, inputFeatures, true);

		assertEquals(2, featureMatrix.length);
		assertEquals(7, featureMatrix[0].length);

		assertEquals(100.0, featureMatrix[0][0], DELTA);
		assertEquals(150.0, featureMatrix[0][1], DELTA);
		assertEquals(1.0, featureMatrix[0][2], DELTA);
		assertEquals(0.0, featureMatrix[0][3], DELTA);
		assertEquals(0.0, featureMatrix[0][4], DELTA);
		assertEquals(0.0, featureMatrix[0][5], DELTA);
		assertEquals(0.0, featureMatrix[0][6], DELTA);

		assertEquals(110.0, featureMatrix[1][0], DELTA);
		assertEquals(160.0, featureMatrix[1][1], DELTA);
		assertEquals(0.0, featureMatrix[1][2], DELTA);
		assertEquals(1.0, featureMatrix[1][3], DELTA);
		assertEquals(0.0, featureMatrix[1][4], DELTA);
		assertEquals(0.0, featureMatrix[1][5], DELTA);
		assertEquals(0.0, featureMatrix[1][6], DELTA);
	}

	@Test
	public void testGetWeatherDataFeatureMatrix_ShouldReturnEmptyFeatureMatrix_IfEmptyWeatherData() {
		var weatherData = new ArrayList<QuarterlyWeatherSnapshot>();
		String[] inputFeatures = { "global_horizontal_irradiance" };

		double[][] featureMatrix = Utils.getWeatherDataFeatureMatrix(weatherData, inputFeatures, false);

		assertEquals(0, featureMatrix.length);
	}
}
