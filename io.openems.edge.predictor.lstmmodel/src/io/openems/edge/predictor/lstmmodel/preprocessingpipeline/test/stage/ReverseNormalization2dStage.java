package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class ReverseNormalization2dStage implements Stage<double[], double[]> {

	private double[] mean;
	private double[] standardDeviation;
	private final HyperParameters hyperParameters;

	public ReverseNormalization2dStage(HyperParameters hyp) {
		this.hyperParameters = hyp;
	}

	public ReverseNormalization2dStage(HyperParameters hyp, double[] mean, double[] standerDeviation) {
		this.mean = mean;
		this.standardDeviation = standerDeviation;
		this.hyperParameters = hyp;
	}

	@Override
	public double[] execute(double[] input) {
		return DataModification.reverseStandrize(input, this.mean, this.standardDeviation, this.hyperParameters);
	}
}
