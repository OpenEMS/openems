package io.openems.edge.predictor.lstm.preprocessingpipeline;

import java.util.Arrays;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class ScalingPipe implements Stage<Object, Object> {

	private static final double MIN_SCALED = 0.2;
	private static final double MAX_SCALED = 0.8;

	private HyperParameters hyperParameter;

	public ScalingPipe(HyperParameters hyperParameters) {
		this.hyperParameter = hyperParameters;
	}

	@Override
	public Object execute(Object value) {
		if (value instanceof double[][] v) {
			return this.scaleSecondCase(v);

		} else if (value instanceof double[] v) {
			return (this.scaleFirstCase(v));

		} else {
			throw new IllegalArgumentException("Input must be an instance of double[]");
		}
	}

	/**
	 * Scales the data in the second case of a two-dimensional array using the
	 * preprocessing pipeline.
	 *
	 * @param value The two-dimensional array containing data to be scaled, where
	 *              each row represents a separate case.
	 * @return A two-dimensional array containing the scaled data, where each row
	 *         corresponds to the scaled data of the respective case.
	 */
	public double[][] scaleSecondCase(double[][] value) {
		if (value == null || value.length != 2 || value[0] == null || value[1] == null) {
			throw new IllegalArgumentException("Input must be a non-null 2xN array.");
		}

		var result = new double[2][];
		result[0] = this.scaleFirstCase(value[0]);
		result[1] = this.scaleFirstCase(value[1]);

		return result;
	}

	/**
	 * Scales the data in the first case of a one-dimensional array using the
	 * provided scaling range.
	 * 
	 * @param value The one-dimensional array containing data to be scaled.
	 * @return An array containing the scaled data.
	 */
	public double[] scaleFirstCase(double[] value) {
		var min = this.hyperParameter.getScalingMin();
		var max = this.hyperParameter.getScalingMax();

		return Arrays.stream(value)//
				.map(v -> MIN_SCALED + ((v - min) / (max - min)) * (MAX_SCALED - MIN_SCALED))//
				.toArray();
	}
}
