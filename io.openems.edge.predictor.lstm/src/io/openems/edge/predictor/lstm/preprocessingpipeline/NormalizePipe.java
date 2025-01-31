package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.preprocessing.DataModification.normalizeData;
import static io.openems.edge.predictor.lstm.preprocessing.DataModification.standardize;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class NormalizePipe implements Stage<Object, Object> {

	private HyperParameters hyperParameters;

	public NormalizePipe(HyperParameters hyper) {
		this.hyperParameters = hyper;
	}

	@Override
	public Object execute(Object input) {
		try {
			return switch (input) {
			case double[][][] inputArray -> {
				double[][] trainData = inputArray[0];
				double[] targetData = inputArray[1][0];

				double[][] normalizedTrainData = normalizeData(trainData, this.hyperParameters);
				double[] normalizedTargetData = normalizeData(trainData, targetData, this.hyperParameters);

				yield new double[][][] { normalizedTrainData, { normalizedTargetData } };
			}

			case double[][] inputArray //
				-> normalizeData(inputArray, this.hyperParameters);

			case double[] inputArray //
				-> standardize(inputArray, this.hyperParameters);

			default //
				-> throw new IllegalArgumentException("Illegal Argument encountered during normalization");
			};
		} catch (Exception e) {
			throw new RuntimeException("Illegal Argument encountered during normalization");
		}
	}
}
