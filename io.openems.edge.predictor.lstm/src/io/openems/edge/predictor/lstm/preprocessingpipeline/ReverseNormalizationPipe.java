package io.openems.edge.predictor.lstm.preprocessingpipeline;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.preprocessing.DataModification;

public class ReverseNormalizationPipe implements Stage<Object, Object> {
	private Object mean;
	private Object stdDeviation;
	private HyperParameters hyperParameters;

	public ReverseNormalizationPipe(Object average, Object std, HyperParameters hyp) {
		this.mean = average;
		this.stdDeviation = std;
		this.hyperParameters = hyp;
	}

	@Override
	public Object execute(Object input) {
		try {
			if (input instanceof double[] inputArray) {
				if (this.mean instanceof double[] meanArray //
						&& this.stdDeviation instanceof double[] sdArray) {
					return DataModification.reverseStandrize(inputArray, meanArray, sdArray, this.hyperParameters);

				} else if (this.mean instanceof Double meanValue //
						&& this.stdDeviation instanceof Double sdValue) {
					return DataModification.reverseStandrize(inputArray, meanValue, sdValue, this.hyperParameters);

				} else {
					throw new IllegalArgumentException("Input must be an instance of double[]");
				}

			} else if (input instanceof Double inputArray) {
				double mean = (double) this.mean;
				double std = (double) this.stdDeviation;
				return DataModification.reverseStandrize(inputArray, mean, std, this.hyperParameters);

			} else {
				throw new IllegalArgumentException("Input must be an instance of double[]");
			}

		} catch (Exception e) {
			throw new RuntimeException("Error processing input data", e);
		}
	}
}
