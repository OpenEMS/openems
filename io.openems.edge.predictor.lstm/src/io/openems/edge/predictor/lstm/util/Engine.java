package io.openems.edge.predictor.lstm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.lstm.util.Lstm.LstmBuilder;
import io.openems.edge.predictor.lstm.utilities.MathUtils;

public class Engine implements EngineDriver {

	private double[][] inputMatrix;
	private double[] targetVector;
	private double[][] validateData;
	private double[] validateTarget;
	private int validatorCounter = 50;
	private double learningRate;
	// private Lstm generalLstm;

	private ArrayList<ArrayList<ArrayList<Double>>> weights = new ArrayList<ArrayList<ArrayList<Double>>>();
	private ArrayList<ArrayList<ArrayList<Double>>> bestWeights = new ArrayList<ArrayList<ArrayList<Double>>>();

	private ArrayList<ArrayList<Double>> finalWeight = new ArrayList<ArrayList<Double>>();

	/**
	 * This method train the LSTM network. and Update the finalWeight matrix.
	 * 
	 * @param epochs Number of times the forward and backward propagation.
	 * @param val    are the weights.
	 * 
	 */
	public void fit(int epochs, ArrayList<ArrayList<Double>> val) {

		AdaptiveLearningRate rate = new AdaptiveLearningRate();

		double perc = ((double) (0 + 1) / this.inputMatrix.length) * 100.00;
		this.learningRate = rate.scheduler(perc);

		Lstm ls = new LstmBuilder(this.inputMatrix[0], this.targetVector[0])//
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

		ArrayList<ArrayList<Double>> wieghtMatrix = ls.train();

		this.weights.add(wieghtMatrix);

		for (int i = 1; i < this.inputMatrix.length; i++) {

			perc = ((double) (i + 1) / this.inputMatrix.length);
			this.learningRate = rate.scheduler(perc);
			// System.out.println(this.learningRate);

			ls = new LstmBuilder(this.inputMatrix[i], this.targetVector[i])// double[] inputData, double outputData

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
			}
			ls.cells.get(0).setYt(wieghtMatrix.get(6).get((wieghtMatrix.get(6).size() - 1)));

			wieghtMatrix = ls.train();
			this.weights.add(wieghtMatrix);

			// int percentage = 90;
			// this.earlyStop(percentage, wieghtMatrix);
			if (this.weights
					.size() == this.validatorCounter/* (int) (this.inputMatrix.length * (float) (percentage * 0.01)) */) {
				int ind = this.selectWeight(this.weights);
				wieghtMatrix = this.weights.get(ind);
				System.out.println(ind);
				this.bestWeights.add(wieghtMatrix);
				this.weights.clear();
			}

		}
		// int ind = this.selectWeight(this.bestWeights);

		// wieghtMatrix.clear();
		// this.finalWeight = this.bestWeights.get(ind);
	}

	@Override
	public void fit(int epochs) {
		// TODO Auto-generated method stub

	}

	// /**
	// * Do not need to go through entire data set to check the better weights,
	// check
	// * once at the specified percentage.
	// *
	// * @param percentage stopping percentage
	// * @param weightMatrix actualWeight matrix
	// */
	// private void earlyStop(int percentage, ArrayList<ArrayList<Double>>
	// weightMatrix) {
	//
	//
	//
	// }

	/**
	 * Simple learning rate update based on the number of iterations.
	 * 
	 * @param iterations   iterations
	 * @param length       Total length of the data.
	 * @param learningRate learning rate.
	 * @return updated learning rate
	 */
	// private double updateLearningRate(int iterations, int length) {
	// double learningRate = 1.0;
	// double perc = 0.0;
	// perc = ((double) (iterations + 1) / length) * 100.0;
	//
	// if (perc < 15) {
	// return learningRate / 10;
	// } else if (15 < perc && perc < 30) {
	// return learningRate / 100;
	// } else if (30 < perc && perc < 60) {
	// return learningRate / 1000;
	// } else if (60 < perc && perc < 90) {
	// return learningRate / 10000;
	// } else {
	// return learningRate / 100000;
	// }
	//
	// }

	/**
	 * Predict using the model and the input data.
	 * 
	 * @param inputData input data for the prediction.
	 * @return result
	 */
	public double[] predict(double[][] inputData) {

		double[] result = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {

			result[i] = this.singleValuePredict(inputData[i], //
					this.finalWeight.get(0), //
					this.finalWeight.get(1), //
					this.finalWeight.get(2), //
					this.finalWeight.get(3), //
					this.finalWeight.get(4), //
					this.finalWeight.get(5));
		}

		return result;
	}

	/**
	 * Validate to get the best model.
	 * 
	 * @param inputData double array
	 * @param target    double array
	 * @param val       weight matrix
	 * @return The resulted weight matrix
	 */
	public double[] validate(double[][] inputData, double[] target, ArrayList<ArrayList<Double>> val) {

		double[] result = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {

			result[i] = this.singleValuePredict(inputData[i], //
					val.get(0), //
					val.get(1), //
					val.get(2), //
					val.get(3), //
					val.get(4), //
					val.get(5));
		}

		return result;
	}

	/**
	 * Takes in an array of inputData and predicts single value.
	 * 
	 * @param inputData double array
	 * @param wi        weight wi
	 * @param wo        weight wo
	 * @param wz        weight wz
	 * @param Ri        weight Ri
	 * @param Ro        weight Ro
	 * @param Rz        weight Rz
	 * @return The predicted single double value
	 */
	private double singleValuePredict(double[] inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {

		double ct = 0;
		double yt = 0;

		for (int i = 0; i < wi.size(); i++) {
			double it = MathUtils.sigmoid(wi.get(i) * inputData[i] + Ri.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * inputData[i] + Ro.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) * inputData[i] + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		return yt;
	}

	/**
	 * Select Best Weight out of all the Weights.
	 * 
	 * @param wightMatrix All the matrices of the weight.
	 * @return index index of the best matrix.
	 */
	public int selectWeight(ArrayList<ArrayList<ArrayList<Double>>> wightMatrix) {

		// System.out.println("Validating...");

		double[] rms = new double[wightMatrix.size()];

		for (int k = 0; k < wightMatrix.size(); k++) {
			ArrayList<ArrayList<Double>> val = wightMatrix.get(k);
			double[] pre = this.validate(this.validateData, this.validateTarget, val);
			rms[k] = this.computeRms(this.validateTarget, pre);
		}
		int minInd = this.getMinIndex(rms);
		// System.out.println("Index : " + minInd);
		return minInd;
	}

	/**
	 * Get the index of the Min element in an array.
	 * 
	 * @param arr double array.
	 * @return iMin index of the min element in an array.
	 */
	public int getMinIndex(double[] arr) {
		double min = Arrays.stream(arr).min().orElseThrow();
		int iMin = Arrays.stream(arr).boxed().collect(Collectors.toList()).indexOf(min);
		// System.out.println("RMS error=" + arr[iMin]);
		return iMin;
	}

	/**
	 * Root mean squared of two arrays.
	 * 
	 * @param original original array
	 * @param computed computed array
	 * @return rmsValue root mean squared value
	 */
	public double computeRms(double[] original, double[] computed) {

		List<Double> diff = IntStream.range(0, original.length) //
				.mapToObj(i -> Math.pow(original[i] - computed[i], 2)) //
				.collect(Collectors.toList());

		return Math.sqrt(diff.stream() //
				.mapToDouble(d -> d)//
				.average()//
				.orElse(0.0));
	}

	public Engine(EngineBuilder builder) {
		this.inputMatrix = builder.inputMatrix;
		this.targetVector = builder.targetVector;
		this.validateData = builder.validateData;
		this.validateTarget = builder.validateTarget;
		this.validatorCounter = builder.validatorCounter;
	}

	public static class EngineBuilder {

		private double[][] inputMatrix;
		private double[] targetVector;
		private double[][] validateData;
		private double[] validateTarget;
		private int validatorCounter;

		public EngineBuilder(double[][] inputMatrix, double[] targetVector, double[][] validateData,
				double[] validateTarget, int validatorCounter) {
			this.inputMatrix = inputMatrix;
			this.targetVector = targetVector;
			this.validateData = validateData;
			this.validateTarget = validateTarget;
			this.validatorCounter = validatorCounter;
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

		public EngineBuilder setValidatorCounter(int validatorCounter) {
			this.validatorCounter = validatorCounter;
			return this;
		}

		// public EngineBuilder setWi() {
		// return this;
		// }

		public Engine build() {
			return new Engine(this);
		}

	}

	public ArrayList<ArrayList<ArrayList<Double>>> getWeights() {
		return this.weights;
	}
}