package io.openems.edge.predictor.lstmmodel.preprocessing;

public class MovingAverage {
	/**
	 * Compute the Moving average for the data array.
	 * 
	 * @param data the data for calculating the Moving average
	 * @return the moving average
	 */
	public static double[] compute(double[] data) {
		int windowSize = 3;

		double[] paddedInputData = new double[data.length + windowSize - 1];
		System.arraycopy(data, 0, paddedInputData, windowSize / 2, data.length);

		double[] movingAverages = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			double sum = 0;
			for (int j = 0; j < windowSize; j++) {
				sum += paddedInputData[i + j];
			}
			movingAverages[i] = sum / windowSize;
		}

		return movingAverages;

	}

}
