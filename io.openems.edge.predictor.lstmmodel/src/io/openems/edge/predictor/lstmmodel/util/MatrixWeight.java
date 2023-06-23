package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;

public class MatrixWeight {

	public ArrayList<ArrayList<Double>> wI = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Double>> wO = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Double>> wZ = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Double>> rI = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Double>> rO = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Double>> rZ = new ArrayList<ArrayList<Double>>();
	
	public ArrayList<ArrayList<Double>> out = new ArrayList<ArrayList<Double>>();
	public ArrayList<ArrayList<Double>> cT = new ArrayList<ArrayList<Double>>();
	public ArrayList<Double> errorList = new ArrayList<Double>();

}
