package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.normalizeData;
import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.standardize;

public class NormalizePipe implements Stage<Object, Object> {

	private HyperParameters hyperParameters;

	public NormalizePipe(HyperParameters hyper) {
		this.hyperParameters = hyper;

	}

	@Override
	public Object execute(Object input) {

		if (input instanceof double[][][] inputArray) {

			double[][] normalizedTrainData = normalizeData(//
					inputArray[0], /* is the train data */
					this.hyperParameters//
			);

			double[] normalizedTargetData = normalizeData(//
					inputArray[0], /* is the train data */
					inputArray[1][0], /* is the target data */
					this.hyperParameters//
			);

			return new double[][][] { normalizedTrainData, { normalizedTargetData } };

		} else if (input instanceof double[][] inputArray) {
			double[][] normdata = normalizeData(inputArray, this.hyperParameters);
			return normdata;
		} else if (input instanceof double[] inputArray) {
			return standardize(inputArray, this.hyperParameters);
		} else {
			throw new IllegalArgumentException("Illegal Argument encountered during normalization");
		}
	}
}
