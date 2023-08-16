package io.openems.edge.predictor.lstm.validator;

import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.predictor.ReadModels;
import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
import io.openems.edge.predictor.lstmmodel.preprocessing.Normalize;
import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstmmodel.preprocessing.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Validation {
	ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalModel = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
	ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
	ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
	ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();
	ArrayList<Double> values;
	ArrayList<OffsetDateTime> dates;
	double minOfTrainingData;
	double maxOfTrainingData;

	public Validation(ArrayList<Double> data, ArrayList<OffsetDateTime> date, double min, double max) {
		values = data;
		dates = date;
		minOfTrainingData = min;
		maxOfTrainingData = max;
		this.validate();

	}

	public void validate() {
		/***
		 * Read the models
		 */

		ReadCsv csv = new ReadCsv();
		

		/**
		 * Preprocess 
		 */
		

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

		


		/**
		 * compute model //
		 */
		ReadModels obj = new ReadModels();
		for (int h = 0; h < obj.allModel.size(); h++) {
			//
			System.out.print(h);
			ArrayList<Double>rmsTemp1 = new ArrayList<Double>();
		   
			int k = 0;
			for (int i = 0; i < dataGroupedByMinute.size(); i++) {
				
				
				
				for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {
					

					

	
					int windowsSize = 7;
					PreProcessingImpl preprocessing = new PreProcessingImpl(dataGroupedByMinute.get(i).get(j),
							windowsSize,  minOfTrainingData,  maxOfTrainingData);
					preprocessing.scale(0.2, 0.8);
					try {

						double[][] validateData = preprocessing.getFeatureData(
								preprocessing.trainTestSplit.validateIndexLower,
								preprocessing.trainTestSplit.validateIndexHigher);

						double[] validateTarget = preprocessing.getTargetData(
								preprocessing.trainTestSplit.validateIndexLower,
								preprocessing.trainTestSplit.validateIndexHigher);

		
						Suffle obj2 = new Suffle(validateData, validateTarget);
						Normalize obj3 = new Normalize(obj2.data);
						

						/**
						 * changing double [][] to ArrayList<ArralList<Double>>
						 */

			
						ArrayList<ArrayList<Double>> val = ReadModels.allModel.get(h).get(k);
						
						
	
						
						ArrayList<Double> result = Predict(obj3.standData, val);
					   
				
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
		Integer optimumWeightIndex  = estimateOptimumWeightIndex(minInd);
		
		System.out.println("Minimum Index :" +optimumWeightIndex);
		obj. updateModel(optimumWeightIndex);
		
	}

	public static ArrayList<Double> Predict(double[][] data, ArrayList<ArrayList<Double>> val) {

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

		for (int i = 0; i < data.length; i++) {
			double it = MathUtils.sigmoid(wi.get(i) * data[i] + Ri.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * data[i] + Ro.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) * data[i] + Rz.get(i) * yt);
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

}
