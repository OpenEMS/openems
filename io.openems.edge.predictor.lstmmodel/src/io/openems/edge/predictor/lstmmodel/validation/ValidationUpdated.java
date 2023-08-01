//package io.openems.edge.predictor.lstmmodel.validation;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
//import io.openems.edge.predictor.lstmmodel.predictor.ReadModels;
//import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
//import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
//import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
//import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
//import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;
//import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;
//
//public class ValidationUpdated {
//
//	ArrayList<Double> values;
//	ArrayList<OffsetDateTime> dates;
//	ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
//	ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
//	ArrayList<ArrayList<Double>> allPrediction = new ArrayList<ArrayList<Double>>();
//	ArrayList<ArrayList<Double>> allTargets = new ArrayList<ArrayList<Double>>();
//
//public ValidationUpdated() {
//		/**
//		 * Get Models
//		 */
//
//		ReadModels obj = new ReadModels();
//
//		/**
//		 * Get all data and dates
//		 **/
//
//		ReadCsv csv = new ReadCsv();
//		values = csv.data;
//		dates = csv.dates;
//
//		// PREPROCESSING
//		/**
//		 * Interpolation
//		 */
//
//		InterpolationManager inter = new InterpolationManager(values); // The result of interpolation is in
//																		// inter.interpolated
//		/**
//		 * Grouping the interpolated data by hour
//		 */
//
//		GroupBy groupAsHour = new GroupBy(inter.interpolated, dates);// The result are stored in
//																		// groupAS.groupedDateByHour and
//																		// groupAs.groupedDataByHour
//		groupAsHour.hour();
//
//		/**
//		 * Grouping data by minute
//		 */
//
//		for (int i = 0; i < groupAsHour.groupedDataByHour.size(); i++) {
//			GroupBy groupAsMinute = new GroupBy(groupAsHour.groupedDataByHour.get(i),
//					groupAsHour.groupedDateByHour.get(i));
//			groupAsMinute.minute();
//			dataGroupedByMinute.add(groupAsMinute.groupedDataByMin);
//			dateGroupedByMinute.add(groupAsMinute.groupedDateByMin);
//		}
//
//		/**
//		 * Making Prediction
//		 */
//		for (int i = 0; i < obj.allModel.size(); i++) {
//			int l = 0;
//
//			for (int j = 0; j < dateGroupedByMinute.size(); j++) {
//				for (int k = 0; k < dateGroupedByMinute.get(i).size(); k++) {
//					/**
//					 * get validation data
//					 */
//					int windowsSize = 7;
//					PreProcessingImpl preprocessing = new PreProcessingImpl(dataGroupedByMinute.get(i).get(j),
//							windowsSize);
//					preprocessing.scale(0.2, 0.8);
//					try {
//						double[][] trainData = preprocessing.getFeatureData(
//								preprocessing.trainTestSplit.trainIndexLower,
//								preprocessing.trainTestSplit.trainIndexHigher);
//
//						double[][] validateData = preprocessing.getFeatureData(
//								preprocessing.trainTestSplit.validateIndexLower,
//								preprocessing.trainTestSplit.validateIndexHigher);
//
//						double[][] testData = preprocessing.getFeatureData(preprocessing.trainTestSplit.testIndexLower,
//								preprocessing.trainTestSplit.testIndexHigher);
//
//						double[] trainTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.trainIndexLower,
//								preprocessing.trainTestSplit.trainIndexHigher);
//
//						double[] validateTarget = preprocessing.getTargetData(
//								preprocessing.trainTestSplit.validateIndexLower,
//								preprocessing.trainTestSplit.validateIndexHigher);
//						/**
//						 * suffeling the data set
//						 */
//						Suffle obj2 = new Suffle(validateData, validateTarget);
//
//						/**
//						 * Get time specific model
//						 */
//						ArrayList<ArrayList<Double>> val = obj.allModel.get(i).get(l);
//
//						/**
//						 * Converting double[][] to ArrayList<ArrayList<Double>>
//						 */
//
//						UtilityConversion obj3 = new UtilityConversion();
//						ArrayList<ArrayList<Double>> data = obj3.convert2DArrayTo2DArrayList(obj2.data);
//
//						/**
//						 * Make prediction
//						 */
//						ArrayList<Double> predicted = Predict(data, val);
//
//						/*
//						 * Saving the presicted values in the form of ArrayList<ArryList<Double>>
//						 * 
//						 */
//						allPrediction.add(predicted);
//
//						/**
//						 * saving the Target datas as ArrayList<ArryList<Double>>
//						 */
//
//						// convert double [] to ArrayList<Double>
//
//						allTargets.add(obj3.convertDoubleArrayToArrayListDouble(obj2.target));
//
//					}
//
//					catch (Exception e) {
//						e.printStackTrace();
//
//					}
//
//				}
//
//			}
//		}
//System.out.print(allTargets);
//	}
//
//	void rmsComputationManager(ArrayList<ArrayList<Double>>predicted,ArrayList<ArrayList<Double>>orginal) {
//		//condition 1 if of of the two Arrays Doesnot have 96 sub arrays
//		//---> check the size of both array and take the minimum size as row 
//		ArrayList<ArrayList<Double>>predictedReshaped = new ArrayList<ArrayList<Double>>();
//		ArrayList<ArrayList<Double>> orginalReshaped = new   ArrayList<ArrayList<Double>>();
//		
//		int row1 = predicted.size();
//		int row2 = orginal.size();
//		int row;
//		int col;
//		if(row1==row2) {
//			
//			row=row1;
//			}
//		else {
//		ArrayList<Integer> temp = new ArrayList<Integer> ();
//		temp.add(row1);
//		temp.add(row2);
//		row = getMinvalue(temp);
//		
//		}
//		
//		
//		//condition 2 if the subarrays doesnot have equal number of data
//		//-----> check the size of all sub arrays and take the minimum size as column
//		ArrayList<Integer>temp2= new ArrayList<Integer>();
//		ArrayList<Integer>temp3= new ArrayList<Integer>();
//	for (int i =0;i<row;i++) {
//		temp2.add(predicted.get(i).size());
//		temp3.add(orginal.get(i).size());
//		
//		
//		
//		
//	}
//	int col1 =  getMinvalue(temp2);
//	 int col2 = getMinvalue(temp3);
//	 if(col1==col2) {
//			col=col1;
//			}
//	 else {
//		 ArrayList<Integer> temp = new ArrayList<Integer>();
//		 temp.add(col1);
//		 temp.add(col2);
//		 col = getMinvalue(temp);
//	 }
//	
//	 
//	//making new matrix for prediction and orginal
//	 
//	for (int i = 0;i<row;i++) {
//		
//	  ArrayList<Double> temp4 = new  ArrayList<Double>();
//	 ArrayList<Double>temp5= new ArrayList<Double>();
//		for (int j = 0; j<col;j++) {
//			temp4.add(orginal.get(i).get(j));
//			temp5.add(predicted.get(i).get(j));
//			
//		}
//		predictedReshaped.add(temp5);
//		orginalReshaped.add(temp4);
//		
//	}
//	
//		
//
//	}
//
//	public double computeRms(ArrayList<Double> original, ArrayList<Double> computed) {
//
//		List<Double> diff = IntStream.range(0, original.size()) //
//				.mapToObj(i -> Math.pow(original.get(i) - computed.get(i), 2)) //
//				.collect(Collectors.toList());
//
//		return Math.sqrt(diff.stream() //
//				.mapToDouble(d -> d)//
//				.average()//
//				.orElse(0.0));
//
////	
//	}
//
//	public double computeAverageOfRms(ArrayList<Double> rms) {
//		double sum = 0;
//		for (int i = 0; 1 < rms.size(); i++) {
//			sum = sum + rms.get(i);
//
//		}
//		return sum / rms.size();
//	}
//
//	public int getMinvalue(ArrayList<Integer> index) {
//		int minVal = Collections.min(index);
//		return minVal;
//	}
//
//	
//	public static ArrayList<Double> Predict( ArrayList<ArrayList<Double>> data, ArrayList<ArrayList<Double>> val) {
//
////		System.out.println("wi:"+wi.size());
////		System.out.println("wo:"+wo.size());
////		System.out.println("wz:"+wz.size());
////		System.out.println("ri:"+Ri.size());
////		System.out.println("ri:"+Ro.size());
////		System.out.println("rz:"+Rz.size());
//		// System.out.println("wi:"+wi.size());
//		System.out.println(val.size());
//		ArrayList<Double> result = new ArrayList<Double>();
//		for (int i = 0; i < data.size(); i++) {
//			ArrayList<Double> wi = val.get(0);
//			ArrayList<Double> wo = val.get(1);
//			ArrayList<Double> wz = val.get(2);
//			ArrayList<Double> Ri = val.get(3);
//			ArrayList<Double> Ro = val.get(4);
//			ArrayList<Double> Rz = val.get(5);
//			
//	
//
//			result.add(predict(data.get(i)[i], wi, wo, wz, Ri, Ro, Rz));
//		}
//
//		return result;
//	}
//
//	public static double predict(double[] data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
//			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {
//		double ct = 0;
//
//		double yt = 0;
////		System.out.println("Input data size: "+inputData.size());
////		System.out.println("Wi size"+wi.size());
//
//		for (int i = 0; i < data.length; i++) {
//			double it = MathUtils.sigmoid(wi.get(i) * data.length + Ri.get(i) * yt);
//			double ot = MathUtils.sigmoid(wo.get(i) * data.length + Ro.get(i) * yt);
//			double zt = MathUtils.tanh(wz.get(i) * data.length + Rz.get(i) * yt);
//			ct = ct + it * zt;
//			yt = ot * MathUtils.tanh(ct);
//		}
//
//		return yt;
//	}
//}
