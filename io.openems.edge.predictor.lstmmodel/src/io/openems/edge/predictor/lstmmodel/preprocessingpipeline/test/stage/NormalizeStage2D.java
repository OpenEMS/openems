package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class NormalizeStage2D implements Stage<double[][], double[][]> {

	private HyperParameters hyperParameters;

	public NormalizeStage2D(HyperParameters hyper) {
		this.hyperParameters = hyper;
	}

	@Override
	public double[][] execute(double[][] inputData) {

		double[][] normdata = DataModification.normalizeData(inputData, this.hyperParameters);
		return normdata;
	}
}