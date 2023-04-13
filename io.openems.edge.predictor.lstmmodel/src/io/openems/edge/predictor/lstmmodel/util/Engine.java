package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.lstmmodel.util.Lstm.LstmBuilder;
import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;

public class Engine implements EngineDriver {

	private double[][] inputMatrix;
	private double[] targetVector;
	private double[][] validateData;
	private double[] validateTarget;

	private ArrayList<ArrayList<ArrayList<Double>>> weights = new ArrayList<ArrayList<ArrayList<Double>>>();
	private ArrayList<ArrayList<ArrayList<Double>>> bestWeights = new ArrayList<ArrayList<ArrayList<Double>>>();
	private ArrayList<ArrayList<Double>> finalWeight = new ArrayList<ArrayList<Double>>();

	public void fit(int epochs) {

		ArrayList<ArrayList<Double>> wieghtMatrix = new ArrayList<ArrayList<Double>>();

		double learningRate = 1;

		Lstm ls = new LstmBuilder()//
				.setInputData(inputMatrix[0]) //
				.setOutputData(targetVector[0]) //
				.setLearningRate(learningRate) //
				.setEpoch(epochs) //
				.build();

		ls.initilizeCells();

		wieghtMatrix = ls.train();

		weights.add(wieghtMatrix);

		for (int i = 1; i < this.inputMatrix.length; i++) {

			learningRate = updateLearningRate(i, inputMatrix.length, learningRate);

			ls = new LstmBuilder()//
					.setInputData(this.inputMatrix[i]) //
					.setOutputData(this.targetVector[i]) //
					.setLearningRate(learningRate) //
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
			ls.cells.get(0).yt = (wieghtMatrix.get(6)).get((wieghtMatrix.get(6).size() - 1));

			wieghtMatrix = ls.train();
			weights.add(wieghtMatrix);

			int percentage = 10;
			earlyStop(percentage, wieghtMatrix);

		}
		int ind = selectWeight(bestWeights);
		wieghtMatrix.clear();
		finalWeight = bestWeights.get(ind);
		// return wieghtMatrix;
	}

	private void earlyStop(int percentage, ArrayList<ArrayList<Double>> wieghtMatrix) {

		if (weights.size() == (int) (inputMatrix.length * (float) (percentage * 0.01))) {
			int ind = selectWeight(weights);
			wieghtMatrix = weights.get(ind);
			bestWeights.add(wieghtMatrix);
			weights.clear();
		} else {
			double error = wieghtMatrix.get(7).get(0);
			// System.out.println("AllWeight = " + weights.size() + " error = " + error);
		}

	}

	/**
	 * Simple learning rate update based on the number of iterations.
	 * 
	 * @param iterations
	 * @param length       Total length of the data.
	 * @param learningRate learning rate.
	 * @return updated learning rate
	 */
	private double updateLearningRate(int iterations, int length, double learningRate) {
		double perc = 0.0;
		perc = ((double) (iterations + 1) / length) * 100.0;

		if (perc < 15) {
			return learningRate / 100000;
		} else if (15 < perc && perc < 30) {
			return learningRate / 10000;
		} else if (30 < perc && perc < 60) {
			return learningRate / 1000;
		} else if (60 < perc && perc < 90) {
			return learningRate / 100;
		} else {
			return learningRate / 10;
		}

	}

	public double[] Predict(double[][] input_data, double[] Target) {

		double[] result = new double[input_data.length];
		for (int i = 0; i < input_data.length; i++) {

			result[i] = predict(input_data[i], //
					finalWeight.get(0), //
					finalWeight.get(1), //
					finalWeight.get(2), //
					finalWeight.get(3), //
					finalWeight.get(4), //
					finalWeight.get(5));
		}

		return result;
	}

	public double[] validate(double[][] input_data, double[] Target, ArrayList<ArrayList<Double>> val) {

		double[] result = new double[input_data.length];
		for (int i = 0; i < input_data.length; i++) {

			result[i] = predict(input_data[i], //
					val.get(0), //
					val.get(1), //
					val.get(2), //
					val.get(3), //
					val.get(4), //
					val.get(5));
		}

		return result;
	}

	public double predict(double[] input_data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {

		double ct = 0;
		double yt = 0;

		for (int i = 0; i < wi.size(); i++) {
			double it = MathUtils.sigmoid(wi.get(i) * input_data[i] + Ri.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * input_data[i] + Ro.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) * input_data[i] + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		return yt;
	}

	/**
	 * Select Best Weight out of all the Weights
	 * 
	 * @param wightMatrix All the matrices of the weight.
	 * @return index index of the best matrix.
	 */
	public int selectWeight(ArrayList<ArrayList<ArrayList<Double>>> wightMatrix) {

		System.out.println("Validating...");

		double[] rms = new double[wightMatrix.size()];

		for (int k = 0; k < wightMatrix.size(); k++) {
			ArrayList<ArrayList<Double>> val = wightMatrix.get(k);
			double[] pre = this.validate(this.validateData, this.validateTarget, val);
			rms[k] = this.computeRMS(this.validateTarget, pre);
		}
		int minInd = getMinIndex(rms);
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
		System.out.println("RMS error=" + arr[iMin]);
		return iMin;
	}

	/**
	 * Root mean squared of two arrays.
	 * 
	 * @param original
	 * @param computed
	 * @return
	 */
	public double computeRMS(double[] original, double[] computed) {

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
	}

	public static class EngineBuilder {

		public double[][] inputMatrix;
		public double[] targetVector;
		public double[][] validateData;
		public double[] validateTarget;
		public int epoch = 100;

		public EngineBuilder(double[][] inputMatrix, double[] targetVector, double[][] validateData,
				double[] validateTarget) {
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
}
