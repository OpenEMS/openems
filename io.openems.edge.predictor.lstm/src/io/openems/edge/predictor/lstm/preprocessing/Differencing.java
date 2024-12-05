package io.openems.edge.predictor.lstm.preprocessing;

import static java.util.stream.IntStream.range;

public class Differencing {

	/**
	 * First order Differencing.
	 * 
	 * @param data data for Differencing
	 * @return the first order Differencing
	 */
	public static double[] firstOrderDifferencing(double[] data) {
		if (data.length < 2) {
			throw new IllegalArgumentException("Data array must contain at least two elements.");
		}

		return range(0, data.length - 1)//
				.mapToDouble(i -> data[i] - data[i + 1])//
				.toArray();
	}

	/**
	 * first Order Accumulating.
	 * 
	 * @param data data for Differencing
	 * @param init data for init
	 * @return the first order Differencing
	 */
	public static double[] firstOrderAccumulating(double[] data, double init) {
		if (data.length == 0) {
			throw new IllegalArgumentException("Data array must not be empty.");
		}

		var accumulating = new double[data.length];
		accumulating[0] = data[0] + init;

		range(1, data.length)//
				.forEach(i -> accumulating[i] = accumulating[i - 1] + data[i]);

		return accumulating;
	}

	/**
	 * first Order Accumulating.
	 * 
	 * @param data data for Differencing
	 * @param init data for init
	 * @return the first order Differencing
	 */
	public static double firstOrderAccumulating(double data, double init) {
		return data + init;
	}
}
