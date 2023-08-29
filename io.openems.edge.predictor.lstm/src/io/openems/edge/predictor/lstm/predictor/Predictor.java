
package io.openems.edge.predictor.lstm.predictor;

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
			

			result.add(predict(inputData.get(i), wi, wo, wz, Ri, Ro, Rz));
		}

		return result;
	}

	public static double predict(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {
		double ct = 0;
		double xt =0;
		double it=0;
		double ot=0;
		double zt =0;
		double yt = 0;
		ArrayList<Double> standData = standardize(inputData);
		
		for (int i = 0; i < standData.size(); i++) {
			 xt = standData.get(i);
			it = MathUtils.sigmoid(wi.get(i) * xt + Ri.get(i) * yt);
			ot = MathUtils.sigmoid(wo.get(i) * xt + Ro.get(i) * yt);
			zt = MathUtils.tanh(wz.get(i) * xt + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
			
			
		}
		double res = reverseStandrize(getMean(inputData), getSTD(inputData),yt);

		return res;
	}
	public static ArrayList<Double> standardize(ArrayList<Double> data) {
        // Calculate mean and standard deviation
        
        
        double mean = getMean(data);
        double stdDeviation= getSTD(data);
        
        // Standardize the data using Z-score
        ArrayList<Double> standardizedData = new ArrayList<>();
        for (double x : data) {
            standardizedData.add((x - mean) / stdDeviation);
        }

        return standardizedData;
    }
	
	public static double getMean(ArrayList<Double> data) {
		
		double sum = 0.0;
        for (double x : data) {
            sum += x;
        }
        double mean = sum / data.size();
		return mean;
	}
	

	public static double getSTD(ArrayList<Double> data) {
		double mean = getMean(data);
		
		double sumSquaredDeviations = 0.0;
        for (double x : data) {
            sumSquaredDeviations += Math.pow(x - mean, 2);
        }
        
        double variance = sumSquaredDeviations / (data.size());
        double stdDeviation = Math.sqrt(variance);
		return stdDeviation;
	}
	
	
  public static  double reverseStandrize(double mean,double standardDeviation,double zvalue) {
	  double reverseStand =0;
	  reverseStand = (zvalue*standardDeviation+mean);
	  return reverseStand;
  }





}
