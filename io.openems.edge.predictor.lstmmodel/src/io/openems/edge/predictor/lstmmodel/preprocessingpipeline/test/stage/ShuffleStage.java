package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.preprocessing.Shuffle;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class ShuffleStage implements Stage<double[][][], double[][][]> {

	@Override
	public double[][][] execute(double[][][] input) {
		double[][] trainData = input[0];
		double[] targetData = input[1][0];

		Shuffle shuffle = new Shuffle(trainData, targetData);

		double[][] shuffledData = shuffle.getData();
		double[] shuffledTarget = shuffle.getTarget();

		return new double[][][] { shuffledData, { shuffledTarget } };
	}

}
