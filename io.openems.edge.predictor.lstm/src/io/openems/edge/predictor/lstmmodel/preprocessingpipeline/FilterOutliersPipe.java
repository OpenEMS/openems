package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.preprocessing.FilterOutliers;

public class FilterOutliersPipe implements Stage<Object, Object> {

	@Override
	public Object execute(Object input) {
		return (input instanceof double[] in)//
				? FilterOutliers.filterOutlier(in)
				: new IllegalArgumentException("Input must be an instance of double[]");
	}
}
