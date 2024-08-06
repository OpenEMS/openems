package io.openems.edge.predictor.lstmmodel.util;

import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.scaleBack;
import static io.openems.edge.predictor.lstmmodel.utilities.MathUtils.sigmoid;
import static io.openems.edge.predictor.lstmmodel.utilities.MathUtils.tanh;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.getMinIndex;

import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.util.Lstm.LstmBuilder;

public class Engine {

	private double[][] inputMatrix;
	private double[] targetVector;
	private double[][] validateData;
	private double[] validateTarget;
	private double learningRate;

	private ArrayList<ArrayList<ArrayList<Double>>> weights = new ArrayList<ArrayList<ArrayList<Double>>>();
	private ArrayList<ArrayList<Double>> finalWeights = new ArrayList<ArrayList<Double>>();

	/**
	 * This method train the LSTM network. and Update the finalWeight matrix.
	 * 
	 * @param epochs          Number of times the forward and backward propagation.
	 * @param val             are the weights.
	 * @param hyperParameters An instance of class HyperParameter
	 * 
	 */
	public void fit(int epochs, ArrayList<ArrayList<Double>> val, HyperParameters hyperParameters) {

		var rate = new AdaptiveLearningRate();

		this.learningRate = rate.scheduler(hyperParameters);

		// First Time default LSTM object
		var ls = new LstmBuilder(this.inputMatrix[0], this.targetVector[0])//
				.setLearningRate(this.learningRate) //
				.setEpoch(epochs)//
				.build();

		ls.initilizeCells();

		ls.setWi(val);
		ls.setWo(val);
		ls.setWz(val);
		ls.setRi(val);
		ls.setRo(val);
		ls.setRz(val);
		ls.setCt(val);
		ls.setYt(val);

		var wieghtMatrix = ls.train();

		this.weights.add(wieghtMatrix);

		for (int i = 1; i < this.inputMatrix.length; i++) {

			this.learningRate = rate.scheduler(hyperParameters);
			// Update the Lstm
			ls = new LstmBuilder(this.inputMatrix[i], this.targetVector[i])//
					.setLearningRate(this.learningRate) //
					.setEpoch(epochs) //
					.build();

			ls.initilizeCells();

			for (int j = 0; j < ls.cells.size(); j++) {

				ls.cells.get(j).setWi((wieghtMatrix.get(0)).get(j));
				ls.cells.get(j).setWo((wieghtMatrix.get(1)).get(j));
				ls.cells.get(j).setWz((wieghtMatrix.get(2)).get(j));
				ls.cells.get(j).setRi((wieghtMatrix.get(3)).get(j));
				ls.cells.get(j).setRo((wieghtMatrix.get(4)).get(j));
				ls.cells.get(j).setRz((wieghtMatrix.get(5)).get(j));
				ls.cells.get(j).setYtMinusOne(wieghtMatrix.get(6).get(j));
				ls.cells.get(j).setCtMinusOne(wieghtMatrix.get(7).get(j));
			}

			wieghtMatrix = ls.train();
			this.weights.add(wieghtMatrix);

		}

	}

	/**
	 * Predict using the model and the input data.
	 * 
	 * @param inputData      input data for the prediction.
	 * @param hyperParameter is the object of class HyperParameter
	 * @return result
	 */
	public double[] predict(double[][] inputData, HyperParameters hyperParameter) {

		var result = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {

			result[i] = this.singleValuePredict(inputData[i], //
					this.finalWeights.get(0), //
					this.finalWeights.get(1), //
					this.finalWeights.get(2), //
					this.finalWeights.get(3), //
					this.finalWeights.get(4), //
					this.finalWeights.get(5), //
					this.finalWeights.get(6), //
					this.finalWeights.get(7), //
					hyperParameter);
		}
		return result;
	}

	/**
	 * Validate to get the best model.
	 * 
	 * @param inputData      double array
	 * @param target         double array
	 * @param val            weight matrix
	 * @param hyperParameter An instance of class HyperParameter
	 * @return The resulted weight matrix
	 */
	public double[] validate(double[][] inputData, double[] target, ArrayList<ArrayList<Double>> val,
			HyperParameters hyperParameter) {

		var result = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {

			result[i] = this.singleValuePredict(inputData[i], //
					val.get(0), //
					val.get(1), //
					val.get(2), //
					val.get(3), //
					val.get(4), //
					val.get(5), //
					val.get(6), //
					val.get(7), //
					hyperParameter);
		}

		return result;
	}

	/**
	 * Takes in an array of inputData and predicts single value.
	 * 
	 * @param inputData      double array
	 * @param wi             weight wi
	 * @param wo             weight wo
	 * @param wz             weight wz
	 * @param Ri             weight Ri
	 * @param Ro             weight Ro
	 * @param Rz             weight Rz
	 * @param ctV            vector containing cell state
	 * @param ytV            vector containing cell output
	 * 
	 * @param hyperParameter An instance of class HyperParameter
	 * @return The predicted single double value
	 */
	private double singleValuePredict(double[] inputData, //
			ArrayList<Double> wi, //
			ArrayList<Double> wo, //
			ArrayList<Double> wz, //
			ArrayList<Double> Ri, //
			ArrayList<Double> Ro, //
			ArrayList<Double> Rz, //
			ArrayList<Double> ytV, //
			ArrayList<Double> ctV, //
			HyperParameters hyperParameter) {

		var ct = 0.;
		var ctMinusOne = 0.;
		var yt = 0.;
		var standData = inputData;

		for (int i = 0; i < wi.size(); i++) {
			ctMinusOne = ctV.get(i);
			double it = sigmoid(wi.get(i) * standData[i] + Ri.get(i) * yt);
			double ot = sigmoid(wo.get(i) * standData[i] + Ro.get(i) * yt);
			double zt = tanh(wz.get(i) * standData[i] + Rz.get(i) * yt);
			ct = ctMinusOne + it * zt;
			yt = ot * tanh(ct);
		}
		return scaleBack(yt, hyperParameter.getScalingMin(), hyperParameter.getScalingMax());
	}

	/**
	 * Select Best Weight out of all the Weights.
	 * 
	 * @param wightMatrix    All the matrices of the weight.
	 * @param hyperParameter is the object of class HyperParameter
	 * @return index index of the best matrix.
	 */
	public int selectWeight(ArrayList<ArrayList<ArrayList<Double>>> wightMatrix, HyperParameters hyperParameter) {

		var rms = new double[wightMatrix.size()];

		for (int k = 0; k < wightMatrix.size(); k++) {
			var val = wightMatrix.get(k);
			var pre = this.validate(this.validateData, this.validateTarget, val, hyperParameter);
			rms[k] = DataStatistics.computeRms(this.validateTarget, pre);
		}
		var minInd = getMinIndex(rms);
		return minInd;
	}

	public Engine(EngineBuilder builder) {
		this.inputMatrix = builder.inputMatrix;
		this.targetVector = builder.targetVector;
		this.validateData = builder.validateData;
		this.validateTarget = builder.validateTarget;

	}

	public static class EngineBuilder {

		private double[][] inputMatrix;
		private double[] targetVector;
		private double[][] validateData;
		private double[] validateTarget;

		public EngineBuilder(double[][] inputMatrix, double[] targetVector, double[][] validateData,
				double[] validateTarget, int validatorCounter) {
			this.inputMatrix = inputMatrix;
			this.targetVector = targetVector;
			this.validateData = validateData;
			this.validateTarget = validateTarget;

		}

		public EngineBuilder() {
		}

		public EngineBuilder setInputMatrix(double[][] inputMatrix) {
			this.inputMatrix = inputMatrix;
			return this;
		}

		public EngineBuilder setTargetVector(double[] targetVector) {
			this.targetVector = targetVector;
			return this;
		}

		public EngineBuilder setValidateData(double[][] validateData) {
			this.validateData = validateData;
			return this;
		}

		public EngineBuilder setValidateTarget(double[] validateTarget) {
			this.validateTarget = validateTarget;
			return this;
		}

		public Engine build() {
			return new Engine(this);
		}
	}

	public ArrayList<ArrayList<ArrayList<Double>>> getWeights() {
		return this.weights;
	}

}
