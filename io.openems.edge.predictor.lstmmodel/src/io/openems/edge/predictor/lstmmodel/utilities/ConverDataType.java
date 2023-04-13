package io.openems.edge.predictor.lstmmodel.utilities;

/**
 * Simple enum for conversion types
 * 
 * <ul>
 * -1 is to convert train data
 * </ul>
 * 
 * <ul>
 * 0 is to convert validation data
 * </ul>
 * 
 * <ul>
 * 1 is to convert test data
 * </ul>
 */
public enum ConverDataType {

	TRAIN(-1), //
	VALIDATE(0), //
	TEST(1);

	private int numVal;

	ConverDataType(int numVal) {
		this.numVal = numVal;
	}

	public int getNumVal() {
		return numVal;
	}

}
