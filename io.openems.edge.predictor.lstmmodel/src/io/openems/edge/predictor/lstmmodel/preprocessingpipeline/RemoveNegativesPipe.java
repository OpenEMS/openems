package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class RemoveNegativesPipe implements Stage<Object, Object> {

	@Override
	public Object execute(Object input) {

		return DataModification.removeNegatives((double[]) input);
	}

}
