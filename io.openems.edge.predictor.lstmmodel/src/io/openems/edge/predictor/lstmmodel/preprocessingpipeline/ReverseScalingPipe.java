package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.scaleBack;

public class ReverseScalingPipe implements Stage<Object, Object> {
	private HyperParameters hype;

	public ReverseScalingPipe(HyperParameters hyperParameters) {
		this.hype = hyperParameters;
	}

	@Override
	public Object execute(Object input) {
		return (input instanceof double[] inputArray) //
				? scaleBack(inputArray, this.hype.getScalingMin(), this.hype.getScalingMax()) //
				: null;
	}
}
