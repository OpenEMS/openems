package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

public class Lstm {

	private double[] inputData;
	private double outputData;
	private double derivativeLWrtRi; //
	private double derivativeLWrtRo; //
	private double derivativeLWrtRz; //
	private double derivativeLWrtWi; //
	private double derivativeLWrtWo; //
	private double derivativeLWrtWz; //
	private double learningRate; //

	ArrayList<Cell> cells;

	public Lstm(double[] input, double output, double learningRate) {
		this.inputData = input;
		this.outputData = output;
		this.derivativeLWrtRi = 0;
		this.derivativeLWrtRo = 0;
		this.derivativeLWrtRz = 0;
		this.derivativeLWrtWi = 0;
		this.derivativeLWrtWo = 0;
		this.derivativeLWrtWz = 0;
		this.derivativeLWrtRi = 0;
		this.derivativeLWrtRo = 0;
		this.derivativeLWrtRz = 0;
		this.learningRate = learningRate;

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
				// System.out.println(i + 1);
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

	public ArrayList<ArrayList<Double>> trainEpoc() {
		ArrayList<ArrayList<Double>> wI = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> wO = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> wZ = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> rI = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> rO = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> rZ = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> out = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> errorList = new ArrayList<Double>();

		int EPOCH = 1000;
		for (int i = 0; i < EPOCH; i++) {
			// System.out.println("Training " + i+1 + "/ " + EPOCH);
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

			errorList.add(cells.get(cells.size() - 1).getError());
			wI.add(temp1);
			wO.add(temp2);
			wZ.add(temp3);
			rI.add(temp4);
			rO.add(temp5);
			rZ.add(temp6);
			out.add(temp7);
		}

		System.out.println(errorList);
		int ind = findGlobalMinima(errorList);

		ArrayList<Double> err = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> return_arr = new ArrayList<ArrayList<Double>>();
		return_arr.add(wI.get(ind));
		return_arr.add(wO.get(ind));
		return_arr.add(wZ.get(ind));
		return_arr.add(rI.get(ind));
		return_arr.add(rO.get(ind));
		return_arr.add(rZ.get(ind));
		return_arr.add(out.get(ind));
		err.add(errorList.get(ind));
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
				//
			}
		}
//		System.out.println(mn);
//		System.out.println(index);
//		System.out.println(getMinIndex(mn));
//		System.out.println(testList.get(index.get(getMinIndex(mn))));

		if (mn.isEmpty()) {
			return getMinIndex(testList);
		} else {
			return index.get(getMinIndex(mn));
		}
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
