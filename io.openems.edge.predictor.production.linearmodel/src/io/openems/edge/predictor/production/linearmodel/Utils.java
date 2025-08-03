package io.openems.edge.predictor.production.linearmodel;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;

import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public final class Utils {

	private static final int[][] TIME_INTERVALS = { //
			{ 6, 9 }, //
			{ 9, 12 }, //
			{ 12, 15 }, //
			{ 15, 18 }, //
			{ 18, 21 } //
	};

	private Utils() {
	}

	/**
	 * Generates a feature matrix of the specified features from the provided
	 * weather data and adds daytime features.
	 * 
	 * @param weatherData            WeatherData object.
	 * @param inputFeatures          Array of feature names that should be
	 *                               extracted.
	 * @param includeDaytimeFeatures Boolean if daytime features should be included.
	 * @return 2D array (matrix) where each row corresponds to a WeatherSnapshot and
	 *         each column corresponds to one of the specified features.
	 */
	public static double[][] getWeatherDataFeatureMatrix(//
			WeatherData weatherData, //
			String[] inputFeatures, //
			boolean includeDaytimeFeatures) {
		var weatherDataMap = weatherData.toMap();
		var dateTimeArray = weatherDataMap.keySet().toArray(ZonedDateTime[]::new);
		var weatherDataArray = weatherDataMap.values().toArray(WeatherSnapshot[]::new);

		Map<String, Function<WeatherSnapshot, Double>> featureToMethodMap = Map.of(//
				"global_horizontal_irradiance", WeatherSnapshot::globalHorizontalIrradiance, //
				"direct_normal_irradiance", WeatherSnapshot::directNormalIrradiance, //
				"temperature", WeatherSnapshot::temperature, //
				"weather_code", snapshot -> (double) snapshot.weatherCode() //
		);

		int nSamples = weatherDataArray.length;
		int nFeatures = inputFeatures.length;
		var features = new double[nSamples][nFeatures];

		for (int i = 0; i < nSamples; i++) {
			var snapshot = weatherDataArray[i];
			for (int j = 0; j < nFeatures; j++) {
				var featureName = inputFeatures[j];
				var extractor = featureToMethodMap.get(featureName);
				if (extractor == null) {
					throw new IllegalArgumentException("Unknown feature: " + featureName);
				}
				features[i][j] = extractor.apply(snapshot);
			}
		}

		if (includeDaytimeFeatures) {
			features = addDaytimeFeatures(features, dateTimeArray);
		}

		return features;
	}

	private static double[][] addDaytimeFeatures(double[][] features, ZonedDateTime[] dateTimeArray) {
		int nSamples = features.length;
		int originalFeaturesCount = features[0].length;
		int additionalFeaturesCount = TIME_INTERVALS.length;

		double[][] extendedFeatures = new double[nSamples][originalFeaturesCount + additionalFeaturesCount];

		for (int i = 0; i < nSamples; i++) {
			System.arraycopy(features[i], 0, extendedFeatures[i], 0, originalFeaturesCount);

			int hourOfDay = dateTimeArray[i].getHour();

			for (int j = 0; j < TIME_INTERVALS.length; j++) {
				int start = TIME_INTERVALS[j][0];
				int end = TIME_INTERVALS[j][1];
				extendedFeatures[i][originalFeaturesCount + j] = (start <= hourOfDay && hourOfDay < end) ? 1.0 : 0.0;
			}
		}

		return extendedFeatures;
	}
}
