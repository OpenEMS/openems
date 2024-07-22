package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.constantScaling;

public class ConstantScalingPipe implements Stage<Object, Object> {

	private double scalingFactor;

	public ConstantScalingPipe(double factor) {
		this.scalingFactor = factor;
	}

	@Override
	public Object execute(Object input) {
		return (input instanceof double[] in) ? constantScaling(in, this.scalingFactor) : null;
	}
}