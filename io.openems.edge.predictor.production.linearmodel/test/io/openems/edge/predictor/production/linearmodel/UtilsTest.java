package io.openems.edge.predictor.production.linearmodel;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public class UtilsTest {

	private static final double DELTA = 1e-6;

	@Test
	public void testGetWeatherDataFeatureMatrix_ShouldExtractCorrectFeatures() {
		WeatherSnapshot snapshot1 = new WeatherSnapshot(100, 150, 25.5, 1);
		WeatherSnapshot snapshot2 = new WeatherSnapshot(110, 160, 26.5, 2);
		ZonedDateTime time1 = ZonedDateTime.parse("2025-04-08T08:00:00Z");
		ZonedDateTime time2 = ZonedDateTime.parse("2025-04-08T09:00:00Z");

		ImmutableSortedMap<ZonedDateTime, WeatherSnapshot> dataMap = ImmutableSortedMap.of(time1, snapshot1, time2,
				snapshot2);
		WeatherData weatherData = WeatherData.from(dataMap);

		String[] inputFeatures = { "global_horizontal_irradiance", "temperature", "weather_code" };

		double[][] featureMatrix = Utils.getWeatherDataFeatureMatrix(weatherData, inputFeatures, false);

		assertEquals(2, featureMatrix.length);
		assertEquals(3, featureMatrix[0].length);

		assertEquals(100, featureMatrix[0][0], DELTA);
		assertEquals(25.5, featureMatrix[0][1], DELTA);
		assertEquals(1.0, featureMatrix[0][2], DELTA);

		assertEquals(110, featureMatrix[1][0], DELTA);
		assertEquals(26.5, featureMatrix[1][1], DELTA);
		assertEquals(2.0, featureMatrix[1][2], DELTA);
	}

	@Test
	public void testGetWeatherDataFeatureMatrix_ShouldConstructCorrectDaytimeFeatures() {
		WeatherSnapshot snapshot1 = new WeatherSnapshot(100, 150, 25.5, 1);
		ZonedDateTime time1 = ZonedDateTime.parse("2025-04-08T08:00:00Z");
		ZonedDateTime time2 = ZonedDateTime.parse("2025-04-08T10:00:00Z");

		ImmutableSortedMap<ZonedDateTime, WeatherSnapshot> dataMap = ImmutableSortedMap.of(time1, snapshot1, time2,
				snapshot1);
		WeatherData weatherData = WeatherData.from(dataMap);

		String[] inputFeatures = { "global_horizontal_irradiance" };

		double[][] featureMatrix = Utils.getWeatherDataFeatureMatrix(weatherData, inputFeatures, true);

		assertEquals(2, featureMatrix.length);
		assertEquals(6, featureMatrix[0].length);

		assertEquals(1.0, featureMatrix[0][1], DELTA);
		assertEquals(0.0, featureMatrix[0][2], DELTA);
		assertEquals(0.0, featureMatrix[0][3], DELTA);
		assertEquals(0.0, featureMatrix[0][4], DELTA);
		assertEquals(0.0, featureMatrix[0][5], DELTA);

		assertEquals(0.0, featureMatrix[1][1], DELTA);
		assertEquals(1.0, featureMatrix[1][2], DELTA);
		assertEquals(0.0, featureMatrix[1][3], DELTA);
		assertEquals(0.0, featureMatrix[1][4], DELTA);
		assertEquals(0.0, featureMatrix[1][5], DELTA);
	}

	@Test
	public void testGetWeatherDataFeatureMatrix_ShouldReturnEmptyFeatureMatrix_IfEmptyWeatherData() {
		WeatherData weatherData = WeatherData.EMPTY_WEATHER_DATA;
		String[] inputFeatures = { "global_horizontal_irradiance" };

		double[][] featureMatrix = Utils.getWeatherDataFeatureMatrix(weatherData, inputFeatures, false);

		assertEquals(0, featureMatrix.length);
	}
}
