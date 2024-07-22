package io.openems.edge.predictor.lstmmodel.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;

public class FilterOutliers {

	/**
	 * Filters out outliers from the dataset until no outliers are detected.
	 *
	 * @param data the input dataset
	 * @return the filtered dataset with outliers removed
	 */
	public static double[] filterOutlier(double[] data) {
		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("Input data must not be null or empty.");
		}

		double[] filteredData = Arrays.copyOf(data, data.length);
		int iterationCount = 0;
		boolean hasOutliers = true;

		while (hasOutliers && iterationCount <= 100) {
			var outlierIndices = detect(filteredData);

			if (outlierIndices.isEmpty()) {
				hasOutliers = false;
			} else {
				filteredData = filter(filteredData, outlierIndices);
			}

			iterationCount++;
		}

		return filteredData;
	}

	/**
	 * Applies the hyperbolic tangent function to data points at the specified
	 * indices.
	 *
	 * @param data  the input dataset
	 * @param index the indices of data points to be transformed
	 * @return the transformed dataset
	 */
	public static double[] filter(double[] data, ArrayList<Integer> indices) {

		if (data == null || indices == null) {
			throw new IllegalArgumentException("Input data and indices must not be null.");
		}

		if (indices.isEmpty()) {
			return data;
		}

		double[] result = data.clone();
		for (int index : indices) {
			if (index >= 0 && index < result.length) {
				result[index] = MathUtils.tanh(result[index]);
			} else {
				throw new IllegalArgumentException("Index out of bounds: " + index);
			}
		}
		return result;
	}

	/**
	 * Detects outliers in the dataset using the interquartile range (IQR) method.
	 *
	 * @param data the input dataset
	 * @return a list of indices of the detected outliers
	 */
	public static ArrayList<Integer> detect(double[] data) {

		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("Input data must not be null or empty.");
		}

		Percentile perc = new Percentile();
		var q1 = perc.evaluate(data, 25);// 25th percentile (Q1) (First percentile)
		var q3 = perc.evaluate(data, 75);// 75th percentile (Q3) (Third percentile)
		var iqr = q3 - q1;
		var upperLimit = q3 + 1.5 * iqr;
		var lowerLimit = q1 - 1.5 * iqr;

		// Detect outliers
		return IntStream.range(0, data.length)//
				.filter(i -> data[i] < lowerLimit || data[i] > upperLimit)
				.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
	}

}
