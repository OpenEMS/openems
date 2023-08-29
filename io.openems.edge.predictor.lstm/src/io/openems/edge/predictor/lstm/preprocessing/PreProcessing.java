package io.openems.edge.predictor.lstm.preprocessing;

public interface PreProcessing {

	public double minScaled = 0.2;
	public double maxScaled = 0.8;

	/**
	 * Scale the Data with min and max values of the list.
	 * 
	 * @param minScaled minimum scale
	 * @param maxScaled maximum scale
	 */
	public void scale(double minScaled, double maxScaled);

	/**
	 * Gets the feature data.
	 * 
	 * @param lower lowest index of the data list
	 * @param upper upper index of the data list
	 * @return featureData featureData for model training.
	 * @throws Exception when the scaleDatalist is empty
	 */
	public double[][] getFeatureData(int lower, int upper) throws Exception;

	/**
	 * Gets the target data.
	 * 
	 * @param lower lowest index of the data list
	 * @param upper upper index of the data list
	 * @return targetData targetDataList for model training.
	 * @throws Exception when the scaleDatalist is empty
	 */
	public double[] getTargetData(int lower, int upper) throws Exception;

}
