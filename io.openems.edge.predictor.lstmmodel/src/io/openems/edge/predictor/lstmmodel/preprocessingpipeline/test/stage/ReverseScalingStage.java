package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class ReverseScalingStage implements Stage<double[], double[]> {
	private HyperParameters hype;

	public ReverseScalingStage(HyperParameters hyperParameters) {
		this.hype = hyperParameters;

	}

	@Override
	public double[] execute(double[] input) {
		return DataModification.scaleBack(input, this.hype.getScalingMin(), this.hype.getScalingMax());
	}
}
