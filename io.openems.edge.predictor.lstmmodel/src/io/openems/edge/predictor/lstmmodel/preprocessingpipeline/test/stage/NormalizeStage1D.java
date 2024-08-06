package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class NormalizeStage1D implements Stage<double[], double[]> {

	private HyperParameters hyperParameters;

	public NormalizeStage1D(HyperParameters hyper) {
		this.hyperParameters = hyper;
	}

	@Override
	public double[] execute(double[] inputData) {
		return DataModification.standardize(inputData, this.hyperParameters);
	}
}
