
package io.openems.edge.predictor.lstmmodel.predictor;

import java.util.ArrayList;
import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;


public class Predictor {
	
	
	public static ArrayList<Double> Predict(ArrayList<ArrayList<Double>> inputData, ArrayList<ArrayList<ArrayList<Double>>> val) {
		
//		System.out.println("wi:"+wi.size());
//		System.out.println("wo:"+wo.size());
//		System.out.println("wz:"+wz.size());
//		System.out.println("ri:"+Ri.size());
//		System.out.println("ri:"+Ro.size());
//		System.out.println("rz:"+Rz.size());
		//System.out.println("wi:"+wi.size());
		System.out.println(val.size());
		ArrayList<Double>result = new ArrayList<Double>();
		for (int i = 0; i < inputData.size(); i++) {
			//System.out.println(val.get(i));
			ArrayList<Double> wi = val.get(i).get(0);
			ArrayList<Double> wo = val.get(i).get(1);
			ArrayList<Double> wz = val.get(i).get(2);
			ArrayList<Double> Ri = val.get(i).get(3);
			ArrayList<Double> Ro = val.get(i).get(4);
			ArrayList<Double> Rz = val.get(i).get(5);

			result.add( predict(inputData.get(i), wi, wo, wz, Ri, Ro, Rz));
		}

		return result;
	}

	public static double predict(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {
		double ct = 0;

		double yt =0;
//		System.out.println("Input data size: "+inputData.size());
//		System.out.println("Wi size"+wi.size());

		for (int i = 0; i < inputData.size(); i++) {
			double it = MathUtils.sigmoid(wi.get(i) * inputData.get(i) + Ri.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * inputData.get(i) + Ro.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) * inputData.get(i) + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		
		return yt;
	}

}
