package io.openems.edge.predictor.lstmmodel.validation;

import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.model.SaveModel;
import io.openems.edge.predictor.lstmmodel.predictor.ReadModels;
import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Validation {
	ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalModel = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
	ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
	ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
	 ArrayList<ArrayList<Double>> rmsTemp2 = new  ArrayList<ArrayList<Double>>();
	ArrayList<Double> values;
	ArrayList<OffsetDateTime> dates;

	public Validation() {
		validate();
	}

	public void validate() {
		/***
		 * Read the models
		 */

		ReadModels obj = new ReadModels();
//		 finalModel = transpose(obj.allModel);
//		 for(int i =0;i<finalModel.size();i++) {
//		 
//		 System.out.print(finalModel);
//		 }
//		 
//
//		/**
//		 * Get the data
//		 */
		ReadCsv csv = new ReadCsv();
		values = csv.data;
		dates = csv.dates;
//
//		/**
//		 * preprocessing
//		 */
//		

		/**
		 * compute interpolation
		 */
		InterpolationManager inter = new InterpolationManager(values); // The result of interpolation is in
																		// inter.interpolated

		/**
		 * Grouping the interpolated data by hour
		 */

		GroupBy groupAsHour = new GroupBy(inter.interpolated, dates);// The result are stored in
																		// groupAS.groupedDateByHour and
																		// groupAs.groupedDataByHour
		groupAsHour.hour();

		/**
		 * Grouping data by minute
		 */

		for (int i = 0; i < groupAsHour.groupedDataByHour.size(); i++) {
			GroupBy groupAsMinute = new GroupBy(groupAsHour.groupedDataByHour.get(i),
					groupAsHour.groupedDateByHour.get(i));
			groupAsMinute.minute();
			dataGroupedByMinute.add(groupAsMinute.groupedDataByMin);
			dateGroupedByMinute.add(groupAsMinute.groupedDateByMin);
		}

		System.out.println(obj.allModel.size());
		System.out.println(obj.allModel.get(0).size());


		/**
		 * compute model //
		 */
		for (int h = 0; h < obj.allModel.size(); h++) {
			//
			System.out.print(h);
			ArrayList<ArrayList<ArrayList<Double>>> allModel = new ArrayList<ArrayList<ArrayList<Double>>>();
			
			ArrayList<Double>rmsTemp1 = new ArrayList<Double>();
		   
			int k = 0;
			for (int i = 0; i < dataGroupedByMinute.size(); i++) {
				
				
				
				for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
					System.out.println("Data Index: " + i + "," + j);
					System.out.println("weight Index: " + h + "," + k);

					System.out.println("Data by minutes" + dataGroupedByMinute.get(i).get(j));

					System.out.print("model: " + obj.allModel.get(h).get(k));
					// System.exit(0);
					int windowsSize = 7;
					PreProcessingImpl preprocessing = new PreProcessingImpl(dataGroupedByMinute.get(i).get(j),
							windowsSize);
					preprocessing.scale(0.2, 0.8);
					try {

						double[][] trainData = preprocessing.getFeatureData(
								preprocessing.trainTestSplit.trainIndexLower,
								preprocessing.trainTestSplit.trainIndexHigher);

						double[][] validateData = preprocessing.getFeatureData(
								preprocessing.trainTestSplit.validateIndexLower,
								preprocessing.trainTestSplit.validateIndexHigher);

						double[][] testData = preprocessing.getFeatureData(preprocessing.trainTestSplit.testIndexLower,
								preprocessing.trainTestSplit.testIndexHigher);

						double[] trainTarget = preprocessing.getTargetData(preprocessing.trainTestSplit.trainIndexLower,
								preprocessing.trainTestSplit.trainIndexHigher);

						double[] validateTarget = preprocessing.getTargetData(
								preprocessing.trainTestSplit.validateIndexLower,
								preprocessing.trainTestSplit.validateIndexHigher);

						Suffle obj1 = new Suffle(trainData, trainTarget);
						Suffle obj2 = new Suffle(validateData, validateTarget);

						/**
						 * changing double [][] to ArrayList<ArralList<Double>>
						 */

						// System.out.print("Data Passed for valodation"+obj2.data);
						ArrayList<ArrayList<Double>> val = obj.allModel.get(h).get(k);

						ArrayList<Double> result = Predict(obj2.data, val);
						double rms = computeRms(obj2.target, result);
						rmsTemp1.add(rms);
						
						//System.exit(0);

						k = k + 1;
						//System.out.println("Result :" + result);

					} //
//			}
//	
					catch (Exception e) {
						e.printStackTrace();

					}
				}
				
				
				
			}
			rmsTemp2.add(rmsTemp1);

		}
		
		System.out.println(rmsTemp2.size()+","+rmsTemp2.get(0).size());
		
		List<List<Integer>> minInd =findMinimumIndices(rmsTemp2);
		System.out.println("Minimum Index :" +minInd);
		obj. updateModel(minInd);
		
	}

	public static ArrayList<Double> Predict(double[][] data, ArrayList<ArrayList<Double>> val) {

//		System.out.println("wi:"+wi.size());
//		System.out.println("wo:"+wo.size());
//		System.out.println("wz:"+wz.size());
//		System.out.println("ri:"+Ri.size());
//		System.out.println("ri:"+Ro.size());
//		System.out.println("rz:"+Rz.size());
		// System.out.println("wi:"+wi.size());
		System.out.println(val.size());
		ArrayList<Double> result = new ArrayList<Double>();
		for (int i = 0; i < data.length; i++) {
			ArrayList<Double> wi = val.get(0);
			ArrayList<Double> wo = val.get(1);
			ArrayList<Double> wz = val.get(2);
			ArrayList<Double> Ri = val.get(3);
			ArrayList<Double> Ro = val.get(4);
			ArrayList<Double> Rz = val.get(5);
			
	

			result.add(predict(data[i], wi, wo, wz, Ri, Ro, Rz));
		}

		return result;
	}

	public static double predict(double[] data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz) {
		double ct = 0;

		double yt = 0;
//		System.out.println("Input data size: "+inputData.size());
//		System.out.println("Wi size"+wi.size());

		for (int i = 0; i < data.length; i++) {
			double it = MathUtils.sigmoid(wi.get(i) * data.length + Ri.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * data.length + Ro.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) * data.length + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}

		return yt;
	}

	public double computeRms(double[] original, ArrayList<Double> computed) {

		List<Double> diff = IntStream.range(0, original.length) //
				.mapToObj(i -> Math.pow(original[i] - computed.get(i), 2)) //
				.collect(Collectors.toList());

		return Math.sqrt(diff.stream() //
				.mapToDouble(d -> d)//
				.average()//
				.orElse(0.0));

//		
	}
	
	
	
	 public static List<List<Integer>> findMinimumIndices(ArrayList<ArrayList<Double>> matrix) {
	        List<List<Integer>> minimumIndices = new ArrayList<>();

	        if (matrix.isEmpty() || matrix.get(0).isEmpty()) {
	            return minimumIndices;  // Empty matrix, return empty list
	        }

	        int numColumns = matrix.get(0).size();

	        for (int col = 0; col < numColumns; col++) {
	            double min = matrix.get(0).get(col);
	            List<Integer> minIndices = new ArrayList<>(Arrays.asList(0, col));

	            for (int row = 1; row < matrix.size(); row++) {
	                double value = matrix.get(row).get(col);

	                if (value < min) {
	                    min = value;
	                    minIndices.set(0, row);
	                }
	            }
	            

	            minimumIndices.add(minIndices);
	        }

	        return minimumIndices;
	    }
	
//	 public static List<List<Integer>> findMinimumIndices(ArrayList<ArrayList<Double>> matrix) {
//	        List<List<Integer>> minimumIndices = new ArrayList<>();
//
//	        if (matrix.isEmpty() || matrix.get(0).isEmpty()) {
//	            return minimumIndices;  // Empty matrix, return empty list
//	        }
//
//	        int numRows = matrix.size();
//
//	        for (int row = 0; row < numRows; row++) {
//	            ArrayList<Double> currentRow = matrix.get(row);
//	            double min = currentRow.get(0);
//	            int minIndex = 0;
//
//	            for (int col = 1; col < currentRow.size(); col++) {
//	                double value = currentRow.get(col);
//
//	                if (value < min) {
//	                    min = value;
//	                    minIndex = col;
//	                }
//	            }
//
//	            minimumIndices.add(List.of(row, minIndex));
//	        }
//
//	        return minimumIndices;
//	    }
//	 
	

}
