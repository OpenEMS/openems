package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class ReverseScalingPipe implements Stage<Object, Object> {
	private HyperParameters hype;

	public ReverseScalingPipe(HyperParameters hyperParameters) {
		this.hype = hyperParameters;

	}

	@Override
	public Object execute(Object input) {

		double[] inputData = (double[]) input;

		return DataModification.scaleBack(inputData, this.hype.getScalingMin(), this.hype.getScalingMax());

	}
}
