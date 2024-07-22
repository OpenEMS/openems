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
	@Override
	public Object execute(Object value) {

		double[] valueTemp = (double[]) value;
		double splitPercentage = this.hyp.getDataSplitTrain();
		int dataSize = valueTemp.length - 1;

		int trainLowerIndex = 0;
		int trainUpperIndex = (int) (splitPercentage * dataSize);

		int testLowerIndex = trainUpperIndex;
		int testUpperIndex = dataSize + 1;

		double[][] combinedData = { // train data
				IntStream.range(trainLowerIndex, trainUpperIndex) //
						.mapToDouble(index -> valueTemp[index]) //
						.toArray(), // target data
				IntStream.range(testLowerIndex, testUpperIndex) //
						.mapToDouble(index -> valueTemp[index]) //
						.toArray() //
		};

		return combinedData;
	}
}
