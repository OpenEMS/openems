package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.preprocessing.DataModification.constantScaling;

public class ConstantScalingPipe implements Stage<Object, Object> {

	private double scalingFactor;

	public ConstantScalingPipe(double factor) {
		this.scalingFactor = factor;
	}

	@Override
	public Object execute(Object input) {
		return (input instanceof double[] in) //
				? constantScaling(in, this.scalingFactor) //
				: new IllegalArgumentException("Input must be an instance of double[]");
	}
}