package io.openems.edge.predictor.lstm.validator;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.DataStatistics;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.common.SaveModel;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
//import io.openems.edge.predictor.lstm.predictor.Predictor;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;
import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
//import io.openems.edge.predictor.lstm.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstm.preprocessing.Suffle;
import io.openems.edge.predictor.lstm.utilities.MathUtils;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Validation {
	String pathSeasonality = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
	String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
	

	public Validation(ArrayList<Double> data, ArrayList<OffsetDateTime> date,Integer itterNumb) {
		
		
//		minOfTrainingData = min;
//		maxOfTrainingData = max;
		validateSeasonality(data,date,itterNumb);
		//validateTrend(data,date,itterNumb);

	}

	public void validateSeasonality(ArrayList<Double>values, ArrayList<OffsetDateTime>dates,Integer itterNumb) {
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalModel = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();
		
		
		double minOfTrainingData;
		double maxOfTrainingData;
		 
		/**
		 * compute interpolation
		 */
		InterpolationManager inter = new InterpolationManager(values); // The result of interpolation is in
		minOfTrainingData = Collections.min(inter.interpolated);
		maxOfTrainingData = Collections.max(inter.interpolated);															// inter.interpolated

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

		


		/**
		 * Read Model //
		 */
		ReadModels models = new ReadModels(pathSeasonality+Integer.toString(itterNumb)+"seasonality.txt");
		for (int h = 0; h < models.allModel.size(); h++) {
			//
			System.out.print(h);
			ArrayList<Double>rmsTemp1 = new ArrayList<Double>();
		   
			int k = 0;
			for (int i = 0; i < dataGroupedByMinute.size(); i++) {
				
				
				
				for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
					

					

	
					int windowsSize = 7;
					PreProcessingImpl preprocessing = new PreProcessingImpl(DataModification.scale(dataGroupedByMinute.get(i).get(j) , minOfTrainingData, maxOfTrainingData), windowsSize);
							
					
					try {

						double[][] validateData = preprocessing.getFeatureData(
								preprocessing.trainTestSplit.trainIndexLower,
								preprocessing.trainTestSplit.trainIndexHigher);

						double[] validateTarget = preprocessing.getTargetData(
								preprocessing.trainTestSplit.trainIndexLower,
								preprocessing.trainTestSplit.trainIndexHigher);

		
						Suffle obj2 = new Suffle(validateData, validateTarget);
						//Normalize obj3 = new Normalize(obj2.data);
						

						/**
						 * changing double [][] to ArrayList<ArralList<Double>>
						 */

			
						ArrayList<ArrayList<Double>> val = models.allModel.get(h).get(k);
						
						
	
						
						ArrayList<Double> result = Predict(obj2.data, val);
					   
				
						double rms = computeRms(obj2.target, result);
						rmsTemp1.add(rms);
						System.out.println(rms);
						k = k + 1;
						

					} 

					catch (Exception e) {
						e.printStackTrace();

					}
				}
				
				
				
			}
			rmsTemp2.add(rmsTemp1);

		}
		
		
		
		List<List<Integer>> minInd =findMinimumIndices(rmsTemp2);
		//Integer optimumWeightIndex  = estimateOptimumWeightIndex(minInd);
		
		System.out.println("Minimum Index :" +minInd);
		models. updateModel(minInd,Integer.toString(itterNumb)+"seasonality.txt");
		
	}
	public void validateTrend(ArrayList<Double>values, ArrayList<OffsetDateTime>dates,Integer itterNumb) {
		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<ArrayList<Double>>> bestWeightTemp = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> bestWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		InterpolationManager inter = new InterpolationManager(values); // The result of interpolation is in
		double minOfTrainingData = Collections.min(inter.interpolated);
		double maxOfTrainingData = Collections.max(inter.interpolated);	
		int windowsSize = 7;
		PreProcessingImpl preprocessing = new PreProcessingImpl(DataModification.scale(values , minOfTrainingData, maxOfTrainingData), windowsSize);
		ReadModels models = new ReadModels(pathTrend+Integer.toString(itterNumb)+"Trend.txt");
		ArrayList<Double>rmsTemp1 = new ArrayList<Double>();
		List<Integer>minIndex1 = new ArrayList<>();
		List<List<Integer>>minIndex2 = new ArrayList<>() ;
		
		
				
		
		try {

			double[][] validateData = preprocessing.getFeatureData(
					preprocessing.trainTestSplit.validateIndexLower,
					preprocessing.trainTestSplit.validateIndexHigher);

			double[] validateTarget = preprocessing.getTargetData(
					preprocessing.trainTestSplit.validateIndexLower,
					preprocessing.trainTestSplit.validateIndexHigher);
			for (int h = 0; h < models.dataList.size(); h++) {
				
			ArrayList<ArrayList<Double>> val = models.dataList.get(h);
			
			
			//System.out.println(val);


			Suffle obj2 = new Suffle(validateData, validateTarget);

			
			
			ArrayList<Double> result = Predict(obj2.data, val);
		   

			double rms = computeRms(obj2.target, result);
			rmsTemp1.add(rms);
			System.out.println(rms);
			//k = k + 1;
		}
		}
		catch (Exception e) {
			e.printStackTrace();

		}
		int miniInd = rmsTemp1.indexOf(Collections.min(rmsTemp1));
		ArrayList<ArrayList<Double>> bestModel = models.dataList.get(miniInd);
		bestWeightTemp.add(bestModel);
		bestWeight.add(bestWeightTemp);
		
		
		SaveModel.saveModels(bestWeight,Integer.toString(itterNumb)+"Trend.txt");
		
//		System.out.println("Minimum Index :" +minIndex2);
//		models. updateModel(minIndex2,"Trend.txt");
		
		
		
		
		}
		
		
		
		
		
		 
		//List<List<Integer>> minInd =findMinimumIndices(rmsTemp2);
		//Integer optimumWeightIndex  = estimateOptimumWeightIndex(minInd);
		
		
		
	


	public double computeRms(double[] original, ArrayList<Double> computed) {
//		PerformanceMatrix pm = new PerformanceMatrix(computed,UtilityConversion.convert1DArrayTo1DArrayList(original),0.2);	
//		
//		pm.statusReport();
		
		return PerformanceMatrix.rmsError(computed,UtilityConversion.convert1DArrayTo1DArrayList(original));
		

	}

	public static List<List<Integer>> findMinimumIndices(ArrayList<ArrayList<Double>> matrix) {
		List<List<Integer>> minimumIndices = new ArrayList<>();

		if (matrix.isEmpty() || matrix.get(0).isEmpty()) {
			return minimumIndices; // Empty matrix, return empty list
		}

		int numColumns = matrix.get(0).size();

		for (int col = 0; col < numColumns; col++) {
			double min = matrix.get(0).get(col);
			List<Integer> minIndices = new ArrayList<>(Arrays.asList(0, col));

			for (int row = 0; row < matrix.size(); row++) {
				double value = matrix.get(row).get(col);

				if (value < min) {
					min = value;
					minIndices.set(0, row);
				}
			}

			minimumIndices.add(minIndices);
		}
		for (int i = 0; i < minimumIndices.size(); i++) {
			System.out.println(matrix.get(minimumIndices.get(i).get(0)).get(minimumIndices.get(i).get(1)));

		}

		return minimumIndices;
	}
	
	

	public static Integer estimateOptimumWeightIndex(List<List<Integer>> index) {

		Integer toReturn;
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for (int i = 0; i < index.size(); i++) {
			temp.add(index.get(i).get(0));
		}
		toReturn = findValueWithMaxCount(temp);
		return toReturn;
	}

	public static Integer findValueWithMaxCount(ArrayList<Integer> numbers) {
		if (numbers == null || numbers.isEmpty()) {
			return null; // Return null for an empty list or null input
		}

		// Create a HashMap to store the count of each value
		HashMap<Integer, Integer> countMap = new HashMap<>();

		// Traverse the ArrayList and count occurrences of each value
		for (Integer num : numbers) {
			countMap.put(num, countMap.getOrDefault(num, 0) + 1);
		}

		// Find the value with maximum count
		int maxCount = 0;
		Integer maxCountValue = null;
		for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
			int count = entry.getValue();
			if (count > maxCount) {
				maxCount = count;
				maxCountValue = entry.getKey();
			}
		}

		return maxCountValue;
	}
	
	public static ArrayList<Double> Predict(double[][] data, ArrayList<ArrayList<Double>> val) {


		
		ArrayList<Double> result = new ArrayList<Double>();
		for (int i = 0; i < data.length; i++) {
			ArrayList<Double> wi = val.get(0);
			ArrayList<Double> wo = val.get(1);
			ArrayList<Double> wz = val.get(2);
			ArrayList<Double> Ri = val.get(3);
			ArrayList<Double> Ro = val.get(4);
			ArrayList<Double> Rz = val.get(5);
			ArrayList<Double> yt = val.get(6);
			ArrayList<Double> ct = val.get(7);
			
	

			result.add(predict(data[i], wi, wo, wz, Ri, Ro, Rz,yt,ct));
		}

		return result;
	}

	public static double predict(double[] data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> Ri, ArrayList<Double> Ro, ArrayList<Double> Rz,ArrayList<Double> ytl,ArrayList<Double> ctl) {
		double ct = 0;

		double yt = 0;
		ArrayList<Double> standData = DataModification.standardize(UtilityConversion.convert1DArrayTo1DArrayList(data));
	

		for (int i = 0; i < data.length; i++) {
//			yt=ytl.get(i);
//			ct = ctl.get(i);
			double it = MathUtils.sigmoid(wi.get(i) * standData.get(i) + Ri.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * standData.get(i) + Ro.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) *  standData.get(i) + Rz.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		double res = DataModification.reverseStandrize(DataStatistics.getMean(UtilityConversion.convert1DArrayTo1DArrayList(data)), DataStatistics.getSTD(UtilityConversion.convert1DArrayTo1DArrayList(data)),yt);

		return res;
	}

}
