package io.openems.edge.predictor.lstmmodel.preprocessing;

public class MovingAverage {

	public static final int WINDOW_SIZE = 3;

	/**
	 * Compute the Moving average for the data array.
	 * 
	 * @param data the data for calculating the Moving average
	 * @return the moving average
	 */
	public static double[] movingAverage(double[] data) {

		double[] paddedInputData = new double[data.length + WINDOW_SIZE - 1];
		System.arraycopy(data, 0, paddedInputData, WINDOW_SIZE / 2, data.length);

		double[] movingAverages = new double[data.length];

		for (int i = 0; i < data.length; i++) {
			double sum = 0;
			for (int j = 0; j < WINDOW_SIZE; j++) {
				sum += paddedInputData[i + j];
			}
			movingAverages[i] = sum / WINDOW_SIZE;
		}

		return movingAverages;
	}
}
