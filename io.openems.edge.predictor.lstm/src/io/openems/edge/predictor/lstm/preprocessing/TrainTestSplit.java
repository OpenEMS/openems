package io.openems.edge.predictor.lstm.preprocessing;

public class TrainTestSplit {

	private int trainIndexLower;
	private int trainIndexHigher;
	private int validateIndexLower;
	private int validateIndexHigher;
	private int testIndexLower;
	private int testIndexHigher;
	private int totalSize;

	public TrainTestSplit(int size, int windowSize, double percentage, double valSplit) {

		this.totalSize = size;

		this.trainIndexLower = 0;
		this.trainIndexHigher = (int) (percentage * size);
		
		this.validateIndexLower = this.trainIndexHigher + 1;
		this.validateIndexHigher = this.validateIndexLower + (int) (valSplit * size);

		this.testIndexLower = this.validateIndexLower;
		this.testIndexHigher = this.testIndexLower + (int) (valSplit * size);
		this.validateIndexHigher = this.testIndexHigher;// here we are ignoring the test data 

	}
	
	public TrainTestSplit(int size, int windowSize, double percentage) {

		this.totalSize = size - 1;

		this.trainIndexLower = 0;
		this.trainIndexHigher = (int) (percentage * size);
		
		

		this.testIndexLower = size - (96 + windowSize);
		this.testIndexHigher = this.totalSize;

		this.validateIndexLower = this.trainIndexHigher;
		this.validateIndexHigher = this.testIndexLower;

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
	
	public int getTrainLowerIndex() {
		return this.trainIndexLower;
	}
	
	public int getTrainUpperIndex() {
		return this.trainIndexHigher;
	}
	
	public int getTestLowerIndex() {
		return this.testIndexLower;
	}
	
	public int getTestUpperIndex() {
		return this.testIndexHigher;
	}
	
	public int getValidateLowerIndex() {
		return this.validateIndexLower;
	}
	
	public int getValidateUpperIndex() {
		return this.validateIndexHigher;
	}
	

}
