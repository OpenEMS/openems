package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class DataModification {
	ArrayList<Double> data = new ArrayList<Double>();

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

	public static double[][] NormalizeData(double data1[][]) {
		double[][] standData;

		standData = new double[data1.length][data1[0].length];
		for (int i = 0; i < data1.length; i++) {
			standData[i] = standardize(data1[i]);
			// Calculate mean and standard deviation
		}
		return standData;

	}

	public static double[] standardize(double[] inputData) {
		// Calculate mean and standard deviation

		double mean = DataStatistics.getMean(UtilityConversion.convert1DArrayTo1DArrayList(inputData));

		double stdDeviation = DataStatistics.getSTD(UtilityConversion.convert1DArrayTo1DArrayList(inputData));
		;

		// Standardize the data using Z-score
		double[] standardizedData = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {
			standardizedData[i] = (inputData[i] - mean) / stdDeviation;
		}

		return standardizedData;
	}

	public static ArrayList<Double> standardize(ArrayList<Double> data) {
		// Calculate mean and standard deviation

		double mean = DataStatistics.getMean(data);
		double stdDeviation = DataStatistics.getSTD(data);

		// Standardize the data using Z-score
		ArrayList<Double> standardizedData = new ArrayList<>();
		for (double x : data) {
			standardizedData.add((x - mean) / stdDeviation);
		}

		return standardizedData;
	}
//		

	public static double reverseStandrize(double mean, double standardDeviation, double zvalue) {
		double reverseStand = 0;
		reverseStand = (zvalue * standardDeviation + mean);
		return reverseStand;
	}

}
