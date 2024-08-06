package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import java.util.stream.IntStream;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class TrainandTestSplitStage implements Stage<double[], double[][]> {

	private final HyperParameters hyperParameters;

	public TrainandTestSplitStage(HyperParameters hyperParameters) {
		super();
		this.hyperParameters = hyperParameters;
	}

	@Override
	public double[][] execute(double[] valueTemp) {
		double splitPercentage = this.hyperParameters.getDataSplitTrain();
		int dataSize = valueTemp.length - 1;

		int trainLowerIndex = 0;
		int trainUpperIndex = (int) (splitPercentage * dataSize);

		int testLowerIndex = trainUpperIndex;
		int testUpperIndex = dataSize + 1;

		double[] trainData = IntStream.range(trainLowerIndex, trainUpperIndex) //
				.mapToDouble(index -> valueTemp[index]) //
				.toArray();
		double[] testData = IntStream.range(testLowerIndex, testUpperIndex) //
				.mapToDouble(index -> valueTemp[index]) //
				.toArray();

		return this.combine(trainData, testData);

	}

	private double[][] combine(double[] train, double[] test) {
		double[][] combined = new double[2][];
		combined[0] = train;
		combined[1] = test;
		return combined;
	}
}
