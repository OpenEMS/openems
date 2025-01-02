package io.openems.edge.predictor.lstm.util;

import java.util.ArrayList;

public class MatrixWeight {

	private final ArrayList<ArrayList<Double>> wI = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> wO = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> wZ = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> wF = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> rI = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> rO = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> rZ = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> rF = new ArrayList<ArrayList<Double>>();

	private final ArrayList<ArrayList<Double>> out = new ArrayList<ArrayList<Double>>();
	private final ArrayList<ArrayList<Double>> cT = new ArrayList<ArrayList<Double>>();
	private final ArrayList<Double> errorList = new ArrayList<Double>();

	public ArrayList<ArrayList<Double>> getWi() {
		return this.wI;
	}

	public ArrayList<ArrayList<Double>> getWo() {
		return this.wO;
	}

	public ArrayList<ArrayList<Double>> getWz() {
		return this.wZ;
	}

	public ArrayList<ArrayList<Double>> getRi() {
		return this.rI;
	}

	public ArrayList<ArrayList<Double>> getRo() {
		return this.rO;
	}

	public ArrayList<ArrayList<Double>> getRz() {
		return this.rZ;
	}

	public ArrayList<ArrayList<Double>> getOut() {
		return this.out;
	}

	public ArrayList<ArrayList<Double>> getCt() {
		return this.cT;
	}

	public ArrayList<Double> getErrorList() {
		return this.errorList;
	}

	public ArrayList<ArrayList<Double>> getWf() {
		return this.wF;
	}

	public ArrayList<ArrayList<Double>> getRf() {
		return this.rF;
	}
}
