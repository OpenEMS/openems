package io.openems.edge.predictor.lstm.preprocessing;

public class TrainTestSplit {

	public int trainIndexLower;
	public int trainIndexHigher;
	public int validateIndexLower;
	public int validateIndexHigher;
	public int testIndexLower;
	public int testIndexHigher;
	public int totalSize;

	public TrainTestSplit(int size, int windowSize, double percentage, double valSplit) {

		this.totalSize = size;

		this.trainIndexLower = 0;
		this.trainIndexHigher = (int) (percentage * size);
		
		this.validateIndexLower =trainIndexHigher+1 ;
		this.validateIndexHigher = validateIndexLower + (int) (valSplit * size);

		this.testIndexLower = validateIndexLower ;
		this.testIndexHigher = testIndexLower + (int) (valSplit * size);
		this.validateIndexHigher = this.testIndexHigher;// here we are ignoring the test data 



		System.out.println(this.printTrainSplitPerc());
	}
	
	public TrainTestSplit(int size, int windowSize, double percentage) {

		this.totalSize = size - 1;

		this.trainIndexLower = 0;
		this.trainIndexHigher = (int) (percentage * size);
		
		

		this.testIndexLower = size - (96 + windowSize);
		this.testIndexHigher = this.totalSize;

		this.validateIndexLower = this.trainIndexHigher;
		this.validateIndexHigher = this.testIndexLower;

		System.out.println(this.printTrainSplitPerc());
	}

	/**
	 * Simple to string method for train/ test/validate split.
	 * 
	 * @return res string of the all the values
	 */
	public String printTrainSplitPerc() {
		StringBuilder sb = new StringBuilder();
		sb.append("Total Data size is : " + this.totalSize);

		sb.append("\n");

		sb.append("Train Data from : " //
				+ this.trainIndexLower //
				+ " to : " //
				+ this.trainIndexHigher //
				+ " size : " //
				+ (this.trainIndexHigher - this.trainIndexLower));

		sb.append("\n");

		sb.append("Validate Data from : " //
				+ this.validateIndexLower //
				+ " to : " //
				+ this.validateIndexHigher //
				+ " size : " //
				+ (this.validateIndexHigher - this.validateIndexLower));

		sb.append("\n");

		sb.append("Test Data from : " //
				+ this.testIndexLower //
				+ " to : " //
				+ this.testIndexHigher //
				+ " size : " //
				+ (this.testIndexHigher - this.testIndexLower));

		sb.append("\n");

		return sb.toString();
	}

}
