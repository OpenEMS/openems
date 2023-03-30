package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;

public class TrainPredict {
	private double[][] inputMatrix;
	private double[] targetVector;
	private double[][] validateData;
	private double[] validateTarget;
	private ArrayList<ArrayList<ArrayList<Double>>> allWeight;
	private ArrayList<ArrayList<ArrayList<Double>>> allWeightFinal;

	public TrainPredict(double[][] inputMatrix, double[] targetVector, double[][] validateData,
			double[] validateTarget) {
		this.inputMatrix = inputMatrix;
		this.targetVector = targetVector;
		this.validateData = validateData;
		this.validateTarget = validateTarget;
		this.allWeight = new ArrayList<>();
		this.allWeightFinal = new ArrayList<>();
	}

	public ArrayList<ArrayList<Double>> train() {
		double perc = 0.0;
		ArrayList<ArrayList<Double>> val = new ArrayList<ArrayList<Double>>();

		for (int i = 0; i < inputMatrix.length; i++) {
			double learningRate = 1;

			perc = ((double) (i + 1) / inputMatrix.length) * 100.0;

			if (perc < 15) {
				learningRate = learningRate / 100000;
			} else if (15 < perc && perc < 30) {
				learningRate = learningRate / 10000;
			} else if (30 < perc && perc < 60) {
				learningRate = learningRate / 1000;
			} else if (60 < perc && perc < 90) {
				learningRate = learningRate / 100;
			} else {
				learningRate = learningRate / 10;
			}

			if (i == 0) {
				Lstm ls;
				ls = new Lstm(inputMatrix[i], targetVector[i], learningRate);
				ls.initilizeCells();
				val = ls.trainEpoc();
				allWeight.add(val);
			} else {
				Lstm ls = new Lstm(inputMatrix[i], targetVector[i], learningRate);
				ls.initilizeCells();
				for (int j = 0; j < ls.cells.size(); j++) {
					ls.cells.get(j).wi = (val.get(0)).get(j);
					ls.cells.get(j).wo = (val.get(1)).get(j);
					ls.cells.get(j).wz = (val.get(2)).get(j);
					ls.cells.get(j).Ri = (val.get(3)).get(j);
					ls.cells.get(j).Ro = (val.get(4)).get(j);
					ls.cells.get(j).Rz = (val.get(5)).get(j);
				}
				ls.cells.get(0).yt = (val.get(6)).get((val.get(6).size() - 1));

				val = ls.trainEpoc();
				allWeight.add(val);
			}

			if (allWeight.size() == 200) {
				int ind = selectWeight(allWeight);
				val = allWeight.get(ind);
				allWeightFinal.add(val);
				allWeight.clear();
			} else {
				double error = val.get(7).get(0);
				System.out.println("AllWeight = " + allWeight.size() + " error = " + error + " % completed = " + perc);
			}
		}
		int ind = selectWeight(allWeightFinal);
		val = allWeightFinal.get(ind);
		return val;
	}

	public double[] Predict(double[][] input_data, double[] Target, ArrayList<ArrayList<Double>> val) {

		double[] result = new double[input_data.length];
		for (int i = 0; i < input_data.length; i++) {

			result[i] = predict(input_data[i], val.get(0), val.get(1), val.get(2), val.get(3), val.get(4), val.get(5));
		}

		return result;
	}

	public static double predict(double[] input_data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {

		double ct = 0;
		double yt = 0;

		MathUtils maths = new MathUtils();

		for (int i = 0; i < wi.size(); i++) {
			double it = maths.sigmoid(wi.get(i) * input_data[i] + Ri.get(i) * yt);
			double ot = maths.sigmoid(wo.get(i) * input_data[i] + Ro.get(i) * yt);
			double zt = maths.tanh(wz.get(i) * input_data[i] + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * maths.tanh(ct);
		}
		return yt;
	}

	public int selectWeight(ArrayList<ArrayList<ArrayList<Double>>> wightMatrix) {

		System.out.println("***************************Validating**************************");

		double[] rms = new double[wightMatrix.size()];

		for (int k = 0; k < wightMatrix.size(); k++) {

			ArrayList<ArrayList<Double>> val = wightMatrix.get(k);

			double[] pre = this.Predict(this.validateData, this.validateTarget, val);

			rms[k] = this.computeRMS(this.validateTarget, pre);
		}
		int minInd = getMinIndex(rms);
		return minInd;
	}

	// TODO optimize this
	public int getMinIndex(double[] arr) {
		int minInd = 0;
		double minValue = arr[0];
		for (int l = 1; l < arr.length; l++) {
			if (arr[l] < minValue) {
				minValue = arr[l];
				minInd = l;
			}
		}
		System.out.println("RMS error=" + arr[minInd]);
		return minInd;
	}

	// TODO optimize this
	public double computeRMS(double[] original, double[] computed) {
		double[] diff = new double[original.length];
		double sum = 0;
		for (int i = 0; i < original.length; i++) {
			diff[i] = Math.pow(original[i] - computed[i], 2);
		}
		for (int i = 0; i < diff.length; i++) {
			sum += diff[i];
		}
		double mean = sum / diff.length;
		return Math.sqrt(mean);
	}
}
