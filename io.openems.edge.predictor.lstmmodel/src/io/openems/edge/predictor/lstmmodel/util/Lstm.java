package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

public class Lstm {

	private double[] inputData;
	private double outputData;
	private double derivativeLWrtRi = 0;; //
	private double derivativeLWrtRo = 0;; //
	private double derivativeLWrtRz = 0;; //
	private double derivativeLWrtWi = 0;; //
	private double derivativeLWrtWo = 0;; //
	private double derivativeLWrtWz = 0;; //
	private double learningRate; //
	private int epoch = 100;

	protected ArrayList<Cell> cells;

	public static Function<ArrayList<Double>, Integer> getMin = (arrayList) -> {
		return arrayList.indexOf(Collections.min(arrayList));
	};

	public Lstm(LstmBuilder builder) {
		this.inputData = builder.inputData;
		this.outputData = builder.outputData;
		this.derivativeLWrtRi = builder.derivativeLWrtRi;
		this.derivativeLWrtRo = builder.derivativeLWrtRo;
		this.derivativeLWrtRz = builder.derivativeLWrtRz;

		this.derivativeLWrtWi = builder.derivativeLWrtWi;
		this.derivativeLWrtWo = builder.derivativeLWrtWo;
		this.derivativeLWrtWz = builder.derivativeLWrtWz;
		this.learningRate = builder.learningRate;
		this.epoch = builder.epoch;
	}

	public void forwardprop() {
		try {
			for (int i = 0; i < cells.size(); i++) {
				cells.get(i).forwardPropogation();
				if (i < cells.size() - 1) {
					cells.get(i + 1).yt = cells.get(i).yt;
					cells.get(i + 1).ct = cells.get(i).ct;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			System.out.println(
					"Warning index error occured in forward prop. This was supposed to happen. Don't worry about it.");
		}
	}

	public void backwardprop() {
		for (int i = cells.size() - 1; i >= 0; i--) {
			if (i == cells.size() - 1) {
				cells.get(i).backwardPropogation();
			} else {
				cells.get(i).dlByDc = cells.get(i + 1).dlByDc;
				cells.get(i).backwardPropogation();
			}
		}

		for (int i = 0; i < cells.size(); i++) {
			derivativeLWrtRi += cells.get(i).yt * cells.get(i).delI;
			derivativeLWrtRo += cells.get(i).yt * cells.get(i).delO;
			derivativeLWrtRz += cells.get(i).yt * cells.get(i).delZ;

			derivativeLWrtWi += cells.get(i).xt * cells.get(i).delI;
			derivativeLWrtWo += cells.get(i).xt * cells.get(i).delO;
			derivativeLWrtWz += cells.get(i).xt * cells.get(i).delZ;

			cells.get(i).setWi(cells.get(i).getWi() + this.learningRate * derivativeLWrtWi);
			cells.get(i).setWo(cells.get(i).getWo() + this.learningRate * derivativeLWrtWo);
			cells.get(i).setWz(cells.get(i).getWz() + this.learningRate * derivativeLWrtWz);
			cells.get(i).setRi(cells.get(i).getRi() + this.learningRate * derivativeLWrtRi);
			cells.get(i).setRo(cells.get(i).getRo() + this.learningRate * derivativeLWrtRo);
			cells.get(i).setRz(cells.get(i).getRz() + this.learningRate * derivativeLWrtRz);

		}
	}

	public ArrayList<ArrayList<Double>> train() {

		MatrixWeight mW = new MatrixWeight();
		//System.out.print("Epoch : ");
		for (int i = 0; i < this.epoch; i++) {
		//	System.out.print(" " + i + " ..");

			forwardprop();
			backwardprop();

			ArrayList<Double> temp1 = new ArrayList<Double>();
			ArrayList<Double> temp2 = new ArrayList<Double>();
			ArrayList<Double> temp3 = new ArrayList<Double>();
			ArrayList<Double> temp4 = new ArrayList<Double>();
			ArrayList<Double> temp5 = new ArrayList<Double>();
			ArrayList<Double> temp6 = new ArrayList<Double>();
			ArrayList<Double> temp7 = new ArrayList<Double>();
			for (int j = 0; j < cells.size(); j++) {
				temp1.add(cells.get(j).getWi()); // wi
				temp2.add(cells.get(j).getWo()); // wo
				temp3.add(cells.get(j).getWz()); // wz
				temp4.add(cells.get(j).getRi()); // Ri
				temp5.add(cells.get(j).getRo()); // Ro
				temp6.add(cells.get(j).getRz()); // Rz
				temp7.add(cells.get(j).yt);
			}

			mW.errorList.add(cells.get(cells.size() - 1).getError());
			mW.wI.add(temp1);
			mW.wO.add(temp2);
			mW.wZ.add(temp3);
			mW.rI.add(temp4);
			mW.rO.add(temp5);
			mW.rZ.add(temp6);
			mW.out.add(temp7);
		}

		int ind = findGlobalMinima(mW.errorList);

		ArrayList<Double> err = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> return_arr = new ArrayList<ArrayList<Double>>();
		return_arr.add(mW.wI.get(ind));
		return_arr.add(mW.wO.get(ind));
		return_arr.add(mW.wZ.get(ind));
		return_arr.add(mW.rI.get(ind));
		return_arr.add(mW.rO.get(ind));
		return_arr.add(mW.rZ.get(ind));
		return_arr.add(mW.out.get(ind));
		err.add(mW.errorList.get(ind));
		return_arr.add(err);
		return return_arr;

	}

	public static int findGlobalMinima(ArrayList<Double> testList) {

		ArrayList<Double> mn = new ArrayList<Double>();
		ArrayList<Integer> index = new ArrayList<Integer>();

		for (int idx = 0; idx < testList.size() - 1; idx++) {
			if ((testList.get(idx) > 0 && testList.get(idx + 1) < 0)) {
				mn.add(testList.get(idx));
				index.add(idx);
			} else if ((testList.get(idx) < 0 && testList.get(idx + 1) > 0)) {
				mn.add(testList.get(idx + 1));
				index.add(idx + 1);
			} else {
				// do nothing
			}
		}

		return mn.isEmpty() ? getMin.apply(testList) : index.get(getMin.apply(mn));

	}

	public double[] getInputData() {
		return inputData;
	}

	public double getOutputData() {
		return outputData;
	}

	public double getDerivativeLWrtRi() {
		return derivativeLWrtRi;
	}

	public double getDerivativeLWrtRo() {
		return derivativeLWrtRo;
	}

	public double getDerivativeLWrtRz() {
		return derivativeLWrtRz;
	}

	public double getDerivativeLWrtWi() {
		return derivativeLWrtWi;
	}

	public double getDerivativeLWrtWo() {
		return derivativeLWrtWo;
	}

	public double getDerivativeLWrtWz() {
		return derivativeLWrtWz;
	}

	public double getLearningRate() {
		return learningRate;
	}

	public ArrayList<Cell> getCells() {
		return cells;
	}

	public static class LstmBuilder {

		protected double[] inputData;
		protected double outputData;
		protected double derivativeLWrtRi; //
		protected double derivativeLWrtRo; //
		protected double derivativeLWrtRz; //
		protected double derivativeLWrtWi; //
		protected double derivativeLWrtWo; //
		protected double derivativeLWrtWz; //
		protected double learningRate; //
		protected int epoch = 100; //

		protected ArrayList<Cell> cells;

		public LstmBuilder(double[] inputData, double outputData) {
			this.inputData = inputData;
			this.outputData = outputData;
		}

		public LstmBuilder() {

		}

		public LstmBuilder setInputData(double[] inputData) {
			this.inputData = inputData;
			return this;
		}

		public LstmBuilder setOutputData(double outputData) {
			this.outputData = outputData;
			return this;
		}

		public LstmBuilder setDerivativeLWrtRi(double derivativeLWrtRi) {
			this.derivativeLWrtRi = derivativeLWrtRi;
			return this;
		}

		public LstmBuilder setDerivativeLWrtRo(double derivativeLWrtRo) {
			this.derivativeLWrtRo = derivativeLWrtRo;
			return this;
		}

		public LstmBuilder setDerivativeLWrtRz(double derivativeLWrtRz) {
			this.derivativeLWrtRz = derivativeLWrtRz;
			return this;
		}

		public LstmBuilder setDerivativeLWrtWi(double derivativeLWrtWi) {
			this.derivativeLWrtWi = derivativeLWrtWi;
			return this;
		}

		public LstmBuilder setDerivativeLWrtWo(double derivativeLWrtWo) {
			this.derivativeLWrtWo = derivativeLWrtWo;
			return this;
		}

		public LstmBuilder setDerivativeLWrtWz(double derivativeLWrtWz) {
			this.derivativeLWrtWz = derivativeLWrtWz;
			return this;
		}

		public LstmBuilder setLearningRate(double learningRate) {
			this.learningRate = learningRate;
			return this;
		}

		public LstmBuilder setEpoch(int epoch) {
			this.epoch = epoch;
			return this;
		}

		public Lstm build() {
			return new Lstm(this);
		}
	}

	public void initilizeCells() {
		this.cells = new ArrayList<>();
		for (int i = 0; i < inputData.length; i++) {
			Cell a = new Cell(inputData[i], outputData);
			cells.add(a);
		}

	}

}
