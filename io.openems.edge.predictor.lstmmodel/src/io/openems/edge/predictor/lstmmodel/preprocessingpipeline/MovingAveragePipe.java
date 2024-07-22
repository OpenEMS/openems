package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.preprocessing.MovingAverage;

public class MovingAveragePipe  implements Stage<Object, Object> {

	@Override
	public Object execute(Object input) {
		double[] inputData = (double[]) input;
		
		return MovingAverage.compute(inputData);
	}

}
