package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class DataModification {

	/**
	 * Scales a list of numeric data values to a specified range. * This method
	 * scales a list of numeric data values to a specified range defined by the
	 * minimum (min) and maximum (max) values. The scaled data will be within the
	 * range defined by the minimumScaled (minScaled) and maximumScaled (maxScaled)
	 * values. *
	 * 
	 * @param data The list of numeric data values to be scaled.
	 * @param min  The original minimum value in the data.
	 * @param max  The original maximum value in the data.
	 * @return A new list containing the scaled data within the specified range.
	 */

	public static ArrayList<Double> scale(ArrayList<Double> data, double min, double max) {

		double minScaled = 0.2;
		double maxScaled = 0.8;
		ArrayList<Double> scaledData = new ArrayList<Double>();

		for (Double value : data) {
			double temp = ((value - min) / (max - min)) * (maxScaled - minScaled);
			scaledData.add(minScaled + temp);
		}
		return scaledData;

	}

	/**
	 * Rescales a single data point from the scaled range to the original range.
	 * This method rescales a single data point from the scaled range (defined by
	 * 'minScaled' and 'maxScaled') back to the original range, which is specified
	 * by 'minOriginal' and 'maxOriginal'. It performs the reverse scaling operation
	 * for a single data value.
	 *
	 * @param scaledData        The data point to be rescaled from the scaled range
	 *                          to the original range.
	 * @param minOfTrainingData The minimum value of the training dataset (original
	 *                          data range).
	 * @param maxOfTrainingData The maximum value of the training dataset (original
	 *                          data range).
	 * @return The rescaled data point in the original range.
	 */

	public static double scaleBack(double scaledData, double minOfTrainingData, double maxOfTrainingData) {

		double minScaled = 0.2;// Collections.min(scaledData1);
		double maxScaled = 0.8;// Collections.max(scaledData1);
		double minOrginal = minOfTrainingData;// this value should be the minimum of training dataset
		double maxOrginal = maxOfTrainingData;// this value should be maximum of training dataset

		return (calc(scaledData, minScaled, maxScaled, minOrginal, maxOrginal));

	}

	private static double calc(double valScaled, double minScaled, double maxScaled, double minOrginal,
			double maxOrginal) {
		double orginal = ((valScaled - minScaled) * (maxOrginal - minOrginal) / (maxScaled - minScaled)) + minOrginal;
		return orginal;

	}

	/**
	 * Normalize a 2D array of data using standardization (z-score normalization).
	 * This method normalizes a 2D array of data by applying standardization
	 * (z-score normalization) to each row independently. The result is a new 2D
	 * array of normalized data.
	 *
	 * @param data1 The 2D array of data to be normalized.
	 * @return A new 2D array containing the standardized (normalized) data.
	 */

	public static double[][] normalizeData(double[][] data1) {
		double[][] standData;

		standData = new double[data1.length][data1[0].length];
		for (int i = 0; i < data1.length; i++) {
			standData[i] = standardize(data1[i]);
			// Calculate mean and standard deviation
		}
		return standData;

	}

	/**
	 * Standardizes a 1D array of data using Z-score normalization. This method
	 * standardizes a 1D array of data by applying Z-score normalization. It
	 * calculates the mean and standard deviation of the input data and then
	 * standardizes each data point.
	 *
	 * @param inputData The 1D array of data to be standardized.
	 * @return A new 1D array containing the standardized (normalized) data.
	 */

	public static double[] standardize(double[] inputData) {
		// Calculate mean and standard deviation

		double mean = DataStatistics.getMean(UtilityConversion.convert1DArrayTo1DArrayList(inputData));

		double stdDeviation = DataStatistics
				.getStanderDeviation(UtilityConversion.convert1DArrayTo1DArrayList(inputData));
		;

		// Standardize the data using Z-score
		double[] standardizedData = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {
			standardizedData[i] = (inputData[i] - mean) / stdDeviation;
		}

		return standardizedData;
	}

	/**
	 * Standardizes a list of numeric data using Z-score normalization. This method
	 * standardizes a list of numeric data by applying Z-score normalization. It
	 * calculates the mean and standard deviation of the input data and then
	 * standardizes each data point. The result is a new ArrayList containing the
	 * standardized (normalized) data.
	 *
	 * @param data The list of numeric data to be standardized.
	 * @return A new ArrayList containing the standardized (normalized) data.
	 */

	public static ArrayList<Double> standardize(ArrayList<Double> data) {
		// Calculate mean and standard deviation

		double mean = DataStatistics.getMean(data);
		double stdDeviation = DataStatistics.getStanderDeviation(data);

		// Standardize the data using Z-score
		ArrayList<Double> standardizedData = new ArrayList<>();
		for (double x : data) {
			standardizedData.add((x - mean) / stdDeviation);
		}

		return standardizedData;
	}

	/**
	 * Reverse standardizes a data point that was previously standardized using
	 * Z-score normalization. This method reverses the standardization process for a
	 * single data point that was previously standardized using Z-score
	 * normalization. It requires the mean and standard deviation of the original
	 * data along with the Z-score value (zvalue) to perform the reverse
	 * standardization.
	 *
	 * @param mean              The mean of the original data.
	 * @param standardDeviation The standard deviation of the original data.
	 * @param zvalue            The Z-score value of the standardized data point.
	 * @return The reverse standardized value in the original data's scale.
	 */

	public static double reverseStandrize(double mean, double standardDeviation, double zvalue) {
		double reverseStand = 0;
		reverseStand = (zvalue * standardDeviation + mean);
		return reverseStand;
	}

}
