package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;

public class TrainPredict {
	
	
	protected  ArrayList<ArrayList<Double>> train(double[][] inputMatrix, double[] targetVector) {
		ArrayList<ArrayList<Double>> val = new ArrayList<ArrayList<Double>>();

		for (int i = 0; i < inputMatrix.length; i++) {
			double perc = ((i + 1) / (double) inputMatrix.length) * 100;
			if (i == 0) {
				Lstm ls = new Lstm(inputMatrix[i], targetVector[i], 0.01);
				ls.initilizeCells();
				val = ls.trainEpoc();
			} else {
				Lstm ls = new Lstm(inputMatrix[i], targetVector[i], 0.01);
				ls.initilizeCells();
				for (int j = 0; j < ls.cells.size(); j++) {
					ls.cells.get(j).wi = (val.get(0)).get(j);
					ls.cells.get(j).wo = (val.get(1)).get(j);
					ls.cells.get(j).wz = (val.get(2)).get(j);
					ls.cells.get(j).Ri = (val.get(3)).get(j);
					ls.cells.get(j).Ro = (val.get(4)).get(j);
					ls.cells.get(j).Rz = (val.get(5)).get(j);
				}
				ls.cells.get(0).yt = (val.get(6)).get((val.get(6).size()-1));

				val = ls.trainEpoc();

			}
			System.out.println("error=" + val.get(7).get(0) + " % completed = " + perc + " \r");
		}
		return val;
	}

	public static double predict(double[] input_data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz, ArrayList<Double> Ri, ArrayList<Double> Ro,
			ArrayList<Double> Rz) {
		double ct = 0;
		
		double yt = 0;
		Calculations maths = new Calculations();

		for (int i = 0; i < wi.size(); i++) {
			double it = maths.sigmoid(wi.get(i) * input_data[i] + Ri.get(i) * yt);
			double ot = maths.sigmoid(wo.get(i) * input_data[i] + Ro.get(i) * yt);
			double zt = maths.tanh(wz.get(i) * input_data[i] + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * maths.tanh(ct);
		}
		return yt;
	}
	
	
public double[] Predict(double[][] input_data,double[] Target, ArrayList<ArrayList<Double>> val) 
{
	ArrayList<Double> wi=val.get(0);
	ArrayList<Double> wo=val.get(1);
	ArrayList<Double> wz=val.get(2);
	ArrayList<Double> Ri=val.get(3);
	ArrayList<Double> Ro=val.get(4);
	ArrayList<Double> Rz=val.get(5);
	double[] result=new double[input_data.length];
	for(int i=0;i<input_data.length;i++)
	{
		
		result[i]=predict(input_data[i],  wi, wo,  wz,  Ri, Ro, Rz);
	}
	for(int i=0;i<input_data.length;i++)
	{
		System.out.println(Target[i]+","+result[i]);
	}
return result;
}

}
