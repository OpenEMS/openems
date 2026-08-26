package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.preprocessing.DataModification.scaleBack;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class ReverseScalingPipe implements Stage<Object, Object> {
	private HyperParameters hype;

	public ReverseScalingPipe(HyperParameters hyperParameters) {
		this.hype = hyperParameters;
	}

	@Override
	public Object execute(Object input) {
		return input instanceof double[] inputArray //
				? scaleBack(inputArray, this.hype.getScalingMin(), this.hype.getScalingMax()) //
				: new IllegalArgumentException("Input must be an instance of double[]");
	}
}
