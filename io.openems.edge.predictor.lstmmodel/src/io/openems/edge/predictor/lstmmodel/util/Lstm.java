package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;

public class Lstm {

	private double[] inputData;
	private double outputData;
	private double delLByDelRi;
	private double delLByDelRo;
	private double delLByDelRz;
	private double delLByDelWi;
	private double delLByDelWo;
	private double delLByDelWz;
	private double learningRate;
	private Calculations maths1;
	ArrayList<Cell> cells;

	public Lstm(double[] input, double output, double learningRate) {
		this.inputData = input;
		this.outputData = output;
		this.delLByDelRi = 0;
		this.delLByDelRo = 0;
		this.delLByDelRz = 0;
		this.delLByDelWi = 0;
		this.delLByDelWo = 0;
		this.delLByDelWz = 0;
		this.delLByDelRi = 0;
		this.delLByDelRo = 0;
		this.delLByDelRz = 0;
		this.learningRate = learningRate;
		this.maths1 = new Calculations();
	}

	public class Cell {

		private double error;

		double wi;
		double wo;
		double wz;
		// private double wf;
		double Ri;
		double Ro;
		// private double Rf;
		double Rz;
		private double ct;
		private double ot;
		private double zt;
		double yt;
		private double dlByDy;
		private double dlByDo;
		private double dlBydDc;
		private double dlByDi;
		private double dlByDz;
		private double delI;
		private double delO;
		private double delZ;
		private double it;

		private double xt;
		private double outputDataLoc;
		private Calculations maths;

		public Cell(double xt, double outputData) {
			this.dlBydDc = 0;
			this.error = 0;

			this.wi = 0;
			this.wo = 0;
			this.wz = 0;
			// this.wf = 0;
			this.Ri = 0;
			this.Ro = 0;
			// this.Rf = 0;
			this.Rz = 0;
			this.ct = 0;

			this.ot = 0;
			this.zt = 0;
			this.yt = 0;

			this.dlByDy = 0;
			this.dlByDo = 0;
			this.dlBydDc = 0;
			this.dlByDi = 0;
			this.dlByDz = 0;

			this.delI = 0;
			this.delO = 0;
			this.delZ = 0;
			this.it = 0;
			this.xt = xt;
			this.outputDataLoc = outputData;
			this.maths = new Calculations();
		}

		public void calcForw() {
			this.it = maths.sigmoid(this.wi * this.xt + this.Ri * this.yt);
			this.ot = maths.sigmoid(this.wo * this.xt + this.Ro * this.yt);
			// ft = maths.sigmoid(wf * xt + Rf * yt);
			this.zt = maths.tanh(this.wz * this.xt + this.Rz * this.yt);
			this.ct = this.ct + this.it * this.zt;
			this.yt = this.ot * maths.tanh(this.ct);
			error = this.outputDataLoc - this.yt;
		}

		public void calcBack() {
			this.dlByDy = this.error;
			this.dlByDo = this.dlByDy * this.maths.tanh(this.ct);
			this.dlBydDc = this.dlByDy * this.ot * this.maths.tanhDer(this.ct) + this.dlBydDc;
			this.dlByDi = this.dlBydDc * this.zt;
			this.dlByDz = this.dlBydDc * this.it;
			this.delI = this.dlByDi * this.maths.sigmoidDer(this.wi + this.Ri * this.yt);
			this.delO = this.dlByDo * this.maths.sigmoidDer(this.wo + this.Ro * this.yt);
			this.delZ = this.dlByDz * this.maths.tanhDer(this.wz + this.Rz * this.yt);
		}

	}

	public void initilizeCells() {
		this.cells = new ArrayList<>();
		for (int i = 0; i < inputData.length; i++) {
			Cell a = new Cell(inputData[i], outputData);
			cells.add(a);
		}

	}

	public void forwardprop() {
		try {
			for (int i = 0; i < cells.size(); i++) {
				cells.get(i).calcForw();
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
				cells.get(i).calcBack();
			} else {
				cells.get(i).dlBydDc = cells.get(i + 1).dlBydDc;
				cells.get(i).calcBack();
			}
		}

		for (int i = 0; i < cells.size(); i++) {
			delLByDelRi += cells.get(i).yt * cells.get(i).delI;
			delLByDelRo += cells.get(i).yt * cells.get(i).delO;
			delLByDelRz += cells.get(i).yt * cells.get(i).delZ;

			delLByDelWi += cells.get(i).xt * cells.get(i).delI;
			delLByDelWo += cells.get(i).xt * cells.get(i).delO;
			delLByDelWz += cells.get(i).xt * cells.get(i).delZ;

			cells.get(i).wi += learningRate * delLByDelWi;
			cells.get(i).wo += learningRate * delLByDelWo;
			cells.get(i).wz += learningRate * delLByDelWz;
			cells.get(i).Ri += learningRate * delLByDelRi;
			cells.get(i).Ro = cells.get(i).Ri + learningRate * delLByDelRo;
			cells.get(i).Rz = cells.get(i).Ri + learningRate * delLByDelRz;
		}
	}

	public ArrayList<ArrayList<Double>> trainEpoc() {
		ArrayList<ArrayList<Double>> wi = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> wo = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> wz = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> Ri = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> Ro = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> Rz = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> out = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> error_list = new ArrayList<Double>();
		ArrayList<double[]> ct = new ArrayList<double[]>();
		ArrayList<double[]> ot = new ArrayList<double[]>();
		ArrayList<double[]> zt = new ArrayList<double[]>();
		ArrayList<double[]> yt = new ArrayList<double[]>();

		for (int i = 0; i < 25; i++) {
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
				temp1.add(cells.get(j).wi);
				temp2.add(cells.get(j).wo);
				temp3.add(cells.get(j).wz);
				temp4.add(cells.get(j).Ri);
				temp5.add(cells.get(j).Ro);
				temp6.add(cells.get(j).Rz);
				temp7.add(cells.get(j).yt);
			}

			error_list.add(cells.get(cells.size() - 1).error);
			wi.add(temp1);
			wo.add(temp2);
			wz.add(temp3);
			Ri.add(temp4);
			Ro.add(temp5);
			Rz.add(temp6);
			out.add(temp7);
		}

		int ind = signChangeInList(error_list);

		if (ind == -1) {
			int a = getMinIndex(error_list);
			ArrayList<ArrayList<Double>> return_arr = new ArrayList<ArrayList<Double>>();
			ArrayList<Double> err = new ArrayList<Double>();
			return_arr.add(wi.get(a));
			return_arr.add(wo.get(a));
			return_arr.add(wz.get(a));
			return_arr.add(Ri.get(a));
			return_arr.add(Ro.get(a));
			return_arr.add(Rz.get(a));
			return_arr.add(out.get(a));
			err.add(error_list.get(a));
			return_arr.add(err);
			return return_arr;

		} else {

			ArrayList<Double> err = new ArrayList<Double>();
			ArrayList<ArrayList<Double>> return_arr = new ArrayList<ArrayList<Double>>();
			return_arr.add(wi.get(ind));
			return_arr.add(wo.get(ind));
			return_arr.add(wz.get(ind));
			return_arr.add(Ri.get(ind));
			return_arr.add(Ro.get(ind));
			return_arr.add(Rz.get(ind));
			return_arr.add(out.get(ind));
			err.add(error_list.get(ind));
			return_arr.add(err);
			return return_arr;

		}

	}

	public static int signChangeInList(ArrayList<Double> testList) {
		for (int idx = 0; idx < testList.size() - 1; idx++) {
			if ((testList.get(idx) > 0 && testList.get(idx + 1) < 0)
					|| (testList.get(idx) < 0 && testList.get(idx + 1) > 0)) {
				return idx;
			}
		}
		return -1;
	}

	public static int getMinIndex(ArrayList<Double> arr) {
		double minVal = arr.get(0);
		int minIdx = 0;
		for (int i = 1; i < arr.size(); i++) {
			if (arr.get(i) < minVal) {
				minVal = arr.get(i);
				minIdx = i;
			}
		}
		return minIdx;
	}

}
