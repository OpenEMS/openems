package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class DataModification {

	/**
	 * Scale data.
	 * 
	 * @param data data
	 * @param min  min value
	 * @param max  max value
	 * @return scaledData
	 */
	public static ArrayList<Double> scale(ArrayList<Double> data, double min, double max) {

		double minScaled = 0.2;
		double maxScaled = 0.8;
		ArrayList<Double> scaledData = new ArrayList<Double>();

		for (int i = 0; i < data.size(); i++) {
			double temp = ((data.get(i) - min) / (max - min)) * (maxScaled - minScaled);
			scaledData.add(minScaled + temp);
		}
		return scaledData;

	}

	/**
	 * Scale back data.
	 * 
	 * @param scaledData        data
	 * @param minOfTrainingData min value
	 * @param maxOfTrainingData max value
	 * @return scale back data
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
	 * Normalize Data.
	 * 
	 * @param data data
	 * @return NormalizedData
	 */
	public static double[][] normalizeData(double[][] data) {

		double[][] standData;
		standData = new double[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			standData[i] = standardize(data[i]);
		}
		return standData;

	}

	/**
	 * standardize data.
	 * 
	 * @param inputData data
	 * @return standardized data
	 */
	public static double[] standardize(double[] inputData) {
		// Calculate mean and standard deviation

		double mean = DataStatistics.getMean(UtilityConversion.convertDoubleArrayToArrayListDouble(inputData));

		double stdDeviation = DataStatistics
				.getStandardDeviation(UtilityConversion.convertDoubleArrayToArrayListDouble(inputData));

		// Standardize the data using Z-score
		double[] standardizedData = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {
			standardizedData[i] = (inputData[i] - mean) / stdDeviation;
		}

		return standardizedData;
	}

	/**
	 * standardize data.
	 * 
	 * @param data data
	 * @return standardized data
	 */
	public static ArrayList<Double> standardize(ArrayList<Double> data) {
		// Calculate mean and standard deviation

		double mean = DataStatistics.getMean(data);
		double stdDeviation = DataStatistics.getStandardDeviation(data);

		// Standardize the data using Z-score
		ArrayList<Double> standardizedData = new ArrayList<>();
		for (double x : data) {
			standardizedData.add((x - mean) / stdDeviation);
		}

		return standardizedData;
	}

	/**
	 * reverse standardize data.
	 * 
	 * @param mean              mean value
	 * @param standardDeviation standardDeviation
	 * @param zValue            zvalue
	 * @return reverseStandrize
	 */
	public static double reverseStandrize(double mean, double standardDeviation, double zValue) {
		double reverseStand = 0;
		reverseStand = (zValue * standardDeviation + mean);
		return reverseStand;
	}

}
