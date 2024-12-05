package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.preprocessing.Shuffle;

public class ShufflePipe implements Stage<Object, Object> {

	@Override
	public Object execute(Object input) {
		if (!(input instanceof double[][][] data)) {
			throw new IllegalArgumentException("Input must be a 3-dimensional double array.");
		}

		double[][] trainData = data[0];
		double[] targetData = data[1][0];

		Shuffle shuffle = new Shuffle(trainData, targetData);

		double[][] shuffledData = shuffle.getData();
		double[] shuffledTarget = shuffle.getTarget();

		return new double[][][] { shuffledData, { shuffledTarget } };
	}
}
