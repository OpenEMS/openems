package io.openems.edge.predictor.lstmmodel.util;

public class TrainTestSplit {

	public int trainIndexLower;
	public int trainIndexHigher;
	public int validateIndexLower;
	public int validateIndexHigher;
	public int testIndexLower;
	public int testIndexHigher;
	public int totalSize;

	public TrainTestSplit(int totalSize, int windowSize, double percentage) {

		this.totalSize = totalSize;

		// 0 to trainIndex
		this.trainIndexLower = 0;
		this.trainIndexHigher = (int) (percentage * totalSize);

		// testIndex to totalSize

		testIndexLower = totalSize - windowSize - 1;
		testIndexHigher = totalSize;

		// (trainIndex +1) to validateIndex
		validateIndexLower = this.trainIndexLower + 1;
		validateIndexHigher = this.testIndexLower - 1;
	}

}
