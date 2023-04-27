package io.openems.edge.predictor.lstmmodel.preprocessing;

public class TrainTestSplit {

	public int trainIndexLower;
	public int trainIndexHigher;
	public int validateIndexLower;
	public int validateIndexHigher;
	public int testIndexLower;
	public int testIndexHigher;
	public int totalSize;

	public TrainTestSplit(int totalSize, int windowSize, double percentage) {

		this.totalSize = totalSize - 1;

		this.trainIndexLower = 0;
		this.trainIndexHigher = (int) (percentage * totalSize);

		testIndexLower = totalSize - (96 + windowSize);
		testIndexHigher = this.totalSize;

		validateIndexLower = this.trainIndexHigher;
		validateIndexHigher = this.testIndexLower;

		System.out.println(printTrainSplitPerc());
	}

	public String printTrainSplitPerc() {
		StringBuilder sb = new StringBuilder();
		sb.append("Total Data size is : " + this.totalSize);

		sb.append("\n");

		sb.append("Train Data from : " //
				+ this.trainIndexLower //
				+ " to : " //
				+ this.trainIndexHigher //
				+ " size : " //
				+ (trainIndexHigher - trainIndexLower));

		sb.append("\n");

		sb.append("Validate Data from : " //
				+ this.validateIndexLower //
				+ " to : " //
				+ this.validateIndexHigher //
				+ " size : " //
				+ (validateIndexHigher - validateIndexLower));

		sb.append("\n");

		sb.append("Test Data from : " //
				+ this.testIndexLower //
				+ " to : " //
				+ this.testIndexHigher //
				+ " size : " //
				+ (testIndexHigher - testIndexLower));

		sb.append("\n");

		return sb.toString();
	}

}
