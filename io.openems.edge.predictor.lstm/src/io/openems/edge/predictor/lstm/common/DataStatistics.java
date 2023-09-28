package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

public class DataStatistics {

	public static double getMean(ArrayList<Double> data) {

		double sum = 0.0;
		for (double x : data) {
			sum += x;
		}
		double mean = sum / data.size();
		return mean;
	}

	public static double getSTD(ArrayList<Double> data) {
		double mean = getMean(data);

		double sumSquaredDeviations = 0.0;
		for (double x : data) {
			sumSquaredDeviations += Math.pow(x - mean, 2);
		}

		double variance = sumSquaredDeviations / (data.size());
		double stdDeviation = Math.sqrt(variance);
		return stdDeviation;
	}

	

}
