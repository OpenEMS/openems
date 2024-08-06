package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.preprocessing.MovingAverage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class MovingAverageStage implements Stage<double[], double[]> {

	@Override
	public double[] execute(double[] value) {
		return MovingAverage.movingAverage(value);
	}

}
