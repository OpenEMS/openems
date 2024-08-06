package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class RemoveNegativeStage implements Stage<double[], double[]> {

	@Override
	public double[] execute(double[] input) {
		return DataModification.removeNegatives(input);
	}
}
