package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;

public interface EngineDriver {

	public void fit(int epochs);

	public double predict(double[] input_data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz);

	public int selectWeight(ArrayList<ArrayList<ArrayList<Double>>> wightMatrix);

	public double computeRMS(double[] original, double[] computed);

}
