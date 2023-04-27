package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;

public interface EngineDriver {

	/**
	 * This method train the LSTM network. and Update the finalWeight matrix.
	 * 
	 * @param epochs Number of times the forward and backward propagation.
	 */
	public void fit(int epochs);

	/**
	 * Predict using the model and the input data.
	 * 
	 * @param inputData input data for the prediction.
	 * @return result
	 */
	public double[] predict(double[][] inputData);

	/**
	 * Select Best Weight out of all the Weights.
	 * 
	 * @param wightMatrix All the matrices of the weight.
	 * @return index index of the best matrix.
	 */
	public int selectWeight(ArrayList<ArrayList<ArrayList<Double>>> wightMatrix);

	/**
	 * Root mean squared of two arrays.
	 * 
	 * @param original original array
	 * @param computed computed array
	 * @return rmsValue root mean squared value
	 */
	public double computeRms(double[] original, double[] computed);

}
