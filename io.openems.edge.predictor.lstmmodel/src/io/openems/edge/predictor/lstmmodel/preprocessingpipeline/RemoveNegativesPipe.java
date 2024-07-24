package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.removeNegatives;

public class RemoveNegativesPipe implements Stage<Object, Object> {

	@Override
	public Object execute(Object input) {
		return (input instanceof double[] in) //
				? removeNegatives(in) //
				: new IllegalArgumentException("Input must be an instance of double[]");
	}
}