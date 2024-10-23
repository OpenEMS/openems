package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.preprocessing.Differencing;

public class DifferencingPipe implements Stage<Object, Object> {

	@Override
	public Object execute(Object input) {
		return (input instanceof double[] in)//
				? Differencing.firstOrderDifferencing(in)//
				: new IllegalArgumentException("Input must be an instance of double[]");
	}
}
