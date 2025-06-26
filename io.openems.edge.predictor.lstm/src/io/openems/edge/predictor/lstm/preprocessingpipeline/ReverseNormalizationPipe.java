package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.preprocessing.DataModification.reverseStandardize;

import io.openems.edge.predictor.lstm.common.HyperParameters;

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
			return switch (input) {
			case double[] inputArray -> {
				if (this.mean instanceof double[] meanArray //
						&& this.stdDeviation instanceof double[] sdArray) {
					yield reverseStandardize(inputArray, meanArray, sdArray, this.hyperParameters);

				} else if (this.mean instanceof Double meanValue //
						&& this.stdDeviation instanceof Double sdValue) {
					yield reverseStandardize(inputArray, meanValue, sdValue, this.hyperParameters);

				} else {
					throw new IllegalArgumentException("Input must be an instance of double[]");
				}
			}

			case Double inputArray -> {
				double mean = (double) this.mean;
				double std = (double) this.stdDeviation;
				yield reverseStandardize(inputArray, mean, std, this.hyperParameters);
			}

			default //
				-> throw new IllegalArgumentException("Input must be an instance of double[]");
			};

		} catch (Exception e) {
			throw new RuntimeException("Error processing input data", e);
		}
	}
}
