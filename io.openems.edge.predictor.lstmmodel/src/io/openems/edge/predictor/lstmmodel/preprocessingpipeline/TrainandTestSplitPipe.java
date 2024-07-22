package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.util.stream.IntStream;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class TrainandTestSplitPipe implements Stage<Object, Object> {
	private HyperParameters hyp;

	public TrainandTestSplitPipe(HyperParameters hyperParameters) {
		this.hyp = hyperParameters;
	}

	/**
	 * Splits the provided data into training and validation datasets based on the
	 * configured split percentage.
	 * 
	 * @param value The array of data to be split.
	 * @return A 2D array where the first row contains the training data and the
	 *         second row contains the validation data.
	 */

	public double[][] execute(double[] value) {

		double splitPercentage = this.hyp.getDataSplitTrain();
		int dataSize = value.length - 1;

		int trainLowerIndex = 0;
		int trainUpperIndex = (int) (splitPercentage * dataSize);

		int testLowerIndex = trainUpperIndex;
		int testUpperIndex = dataSize + 1;

		double[] trainData = IntStream.range(trainLowerIndex, trainUpperIndex) //
				.mapToDouble(index -> value[index]) //
				.toArray();
		double[] testData = IntStream.range(testLowerIndex, testUpperIndex) //
				.mapToDouble(index -> value[index]) //
				.toArray();

		return this.combine(trainData, testData);
	}

	@Override
	public Object execute(Object value) {
		double[] valueTemp = (double[]) value;
		double splitPercentage = this.hyp.getDataSplitTrain();
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
