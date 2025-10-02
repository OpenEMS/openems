package io.openems.edge.predictor.production.linearmodel;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

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
			List<QuarterlyWeatherSnapshot> weatherData, //
			String[] inputFeatures, //
			boolean includeDaytimeFeatures) {
		Map<String, Function<QuarterlyWeatherSnapshot, Double>> featureToMethodMap = Map.of(//
				"global_horizontal_irradiance", QuarterlyWeatherSnapshot::globalHorizontalIrradiance, //
				"direct_normal_irradiance", QuarterlyWeatherSnapshot::directNormalIrradiance);

		int nSamples = weatherData.size();
		int nFeatures = inputFeatures.length;
		var features = new double[nSamples][nFeatures];

		var dateTimeArray = new ZonedDateTime[nSamples];

		for (int i = 0; i < nSamples; i++) {
			var snapshot = weatherData.get(i);
			for (int j = 0; j < nFeatures; j++) {
				var featureName = inputFeatures[j];
				var extractor = featureToMethodMap.get(featureName);
				if (extractor == null) {
					throw new IllegalArgumentException("Unknown feature: " + featureName);
				}
				features[i][j] = extractor.apply(snapshot);
			}
			dateTimeArray[i] = snapshot.datetime();
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
