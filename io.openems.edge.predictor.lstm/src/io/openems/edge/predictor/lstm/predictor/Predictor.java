
package io.openems.edge.predictor.lstm.predictor;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.DataStatistics;

import java.util.ArrayList;

import io.openems.edge.predictor.lstm.utilities.MathUtils;


public class Predictor {

	public static ArrayList<Double> Predict(ArrayList<ArrayList<Double>> inputData,
			ArrayList<ArrayList<ArrayList<Double>>> val) {
		

		ArrayList<Double> result = new ArrayList<Double>();
		for (int i = 0; i < inputData.size(); i++) {
			ArrayList<Double> wi = val.get(i).get(0);
			ArrayList<Double> wo = val.get(i).get(1);
			ArrayList<Double> wz = val.get(i).get(2);
			ArrayList<Double> Ri = val.get(i).get(3);
			ArrayList<Double> Ro = val.get(i).get(4);
			ArrayList<Double> Rz = val.get(i).get(5);
			ArrayList<Double> ct = val.get(i).get(7);
			ArrayList<Double> yt = val.get(i).get(6);
			

			result.add(predict(inputData.get(i), wi, wo, wz, Ri, Ro, Rz,ct,yt));
		}

		return result;
	}

	public static double predict(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz,ArrayList<Double> cta,ArrayList<Double> yta) {
		double ct = 0;
		double xt =0;
		double it=0;
		double ot=0;
		double zt =0;
		double yt = 0;
		ArrayList<Double> standData = DataModification.standardize(inputData);
		
		for (int i = 0; i < standData.size(); i++) {
			 xt = standData.get(i);
//			 ct = cta.get(i);
//			 yt = yta.get(i);
			it = MathUtils.sigmoid(wi.get(i) * xt + Ri.get(i) * yt);
			ot = MathUtils.sigmoid(wo.get(i) * xt + Ro.get(i) * yt);
			zt = MathUtils.tanh(wz.get(i) * xt + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
			
			
		}
		double res = DataModification.reverseStandrize(DataStatistics.getMean(inputData), DataStatistics.getSTD(inputData),yt);

		return res;
	}


public static double predictFocoused(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
		ArrayList<Double> wz, ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz,ArrayList<Double> cta,ArrayList<Double> yta) {
	double ct = 0;
	double xt =0;
	double it=0;
	double ot=0;
	double zt =0;
	double yt = 0;
	ArrayList<Double> standData = DataModification.standardize(inputData);
	
	for (int i = 0; i < standData.size(); i++) {
		 xt = standData.get(i);
//		 ct = cta.get(i);
//		 yt = yta.get(i);
		it = MathUtils.sigmoid( Ri.get(i) * yt);
		ot = MathUtils.sigmoid( Ro.get(i) * yt);
		zt = MathUtils.tanh(wz.get(i) * xt );
		ct = ct + it * zt;
		yt = ot * MathUtils.tanh(ct);
		
		
	}
	double res = DataModification.reverseStandrize(DataStatistics.getMean(inputData), DataStatistics.getSTD(inputData),yt);

	return res;
}

}
