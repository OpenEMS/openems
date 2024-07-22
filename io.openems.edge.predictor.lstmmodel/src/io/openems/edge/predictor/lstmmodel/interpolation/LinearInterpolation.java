package io.openems.edge.predictor.lstmmodel.interpolation;

import java.util.ArrayList;
import java.util.Arrays;

public class LinearInterpolation {

	/**
	 * Interpolates NaN values in the provided data set.
	 *
	 * @param data The input data set with NaN values.
	 * @return The data set with NaN values replaced by interpolated values.
	 */

	public static ArrayList<Double> interpolate(ArrayList<Double> data) {

		ArrayList<ArrayList<Integer>> coordinate = determineInterpolatingPoints(data);
		for (int i = 0; i < coordinate.size(); i++) {
			var xVal1 = coordinate.get(i).get(0);
			var xVal2 = coordinate.get(i).get(1);

			var ineterPolationResult = computeInterpolation(xVal1, xVal2, data.get(xVal1), data.get((int) xVal2));
			data = combine(data, ineterPolationResult, xVal1, xVal2);

		}
		return data;

	}

	/**
	 * Determines the indices where NaN values are sandwiched between non-NaN values
	 * in a given data set.
	 *
	 * @param data The input data set.
	 * @return A list of coordinate pairs representing the indices where NaN values
	 *         are sandwiched.
	 */

	public static ArrayList<ArrayList<Integer>> determineInterpolatingPoints(ArrayList<Double> data) {

		ArrayList<ArrayList<Integer>> coordinates = new ArrayList<>();

		var inNaNSequence = false;
		var xVal1 = -1;

		for (int i = 0; i < data.size(); i++) {
			var currentValue = data.get(i);

			if (Double.isNaN(currentValue)) {
				if (!inNaNSequence) {
					xVal1 = i - 1;
					inNaNSequence = true;
				}
			} else {
				if (inNaNSequence) {
					var xVal2 = i;
					ArrayList<Integer> temp = new ArrayList<>();
					temp.add(xVal1);
					temp.add(xVal2);
					coordinates.add(temp);
					inNaNSequence = false;
				}
			}
		}
		return coordinates;
	}

	/**
	 * Computes linear interpolation between two values.
	 *
	 * @param xValue1 The x-value corresponding to the first data point.
	 * @param xValue2 The x-value corresponding to the second data point.
	 * @param yValue1 The y-value corresponding to the first data point.
	 * @param yValue2 The y-value corresponding to the second data point.
	 * @return A list of interpolated y-values between xValue1 and xValue2.
	 */

	public static ArrayList<Double> computeInterpolation(int xValue1, int xValue2, double yValue1, double yValue2) {
		var interPolatedResults = new ArrayList<Double>();
		var xVal1 = (double) xValue1;
		var xVal2 = (double) xValue2;

		for (int i = 1; i < (xValue2 - xValue1); i++) {
			interPolatedResults
					.add((yValue1 * ((xVal2 - (i + xVal1)) / (xVal2 - xVal1)) + yValue2 * ((i) / (xVal2 - xVal1))));
		}
		return interPolatedResults;
	}

	/**
	 * Combines the original data set with the interpolation result.
	 *
	 * @param orginalData        The original data set.
	 * @param interpolatedResult The result of linear interpolation.
	 * @param xValue1            The first index used for interpolation.
	 * @param xValue2            The second index used for interpolation.
	 * @return The combined data set with interpolated values.
	 */

	public static ArrayList<Double> combine(ArrayList<Double> orginalData, ArrayList<Double> interpolatedResult,
			int xValue1, int xValue2) {

		for (int i = 0; i < (interpolatedResult.size()); i++) {
			orginalData.set((i + xValue1 + 1), interpolatedResult.get(i));
		}
		return orginalData;
	}

}
