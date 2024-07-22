package io.openems.edge.predictor.lstmmodel.preprocessing;

import static io.openems.edge.predictor.lstmmodel.common.DataStatistics.getMean;
import static io.openems.edge.predictor.lstmmodel.common.DataStatistics.getStandardDeviation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//import io.openems.edge.predictor.lstm.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
//import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class DataModification {

	private static final double MIN_SCALED = 0.2;
	private static final double MAX_SCALED = 0.8;

	/**
	 * Scales a list of numeric data values to a specified range. This method scales
	 * a list of numeric data values to a specified range defined by the minimum
	 * (min) and maximum (max) values. The scaled data will be within the range
	 * defined by the minimumScaled (minScaled) and maximumScaled (maxScaled)
	 * values.
	 *
	 * @param data The list of numeric data values to be scaled.
	 * @param min  The original minimum value in the data.
	 * @param max  The original maximum value in the data.
	 * @return A new list containing the scaled data within the specified range.
	 */
	public static ArrayList<Double> scale(ArrayList<Double> data, double min, double max) {
		ArrayList<Double> scaledData = new ArrayList<>();
		for (Double value : data) {
			double scaledValue = MIN_SCALED + ((value - min) / (max - min)) * (MAX_SCALED - MIN_SCALED);
			scaledData.add(scaledValue);
		}
		return scaledData;
	}

	/**
	 * * Scales a list of numeric data values to a specified range. This method
	 * scales a list of numeric data values to a specified range defined by the
	 * minimum (min) and maximum (max) values. The scaled data will be within the
	 * range defined by the minimumScaled (minScaled) and maximumScaled (maxScaled)
	 * values.
	 * 
	 * @param data The array of numeric data values to be scaled.
	 * @param min  The original minimum value in the data.
	 * @param max  The original maximum value in the data.
	 * @return A new list containing the scaled data within the specified range.
	 */
	public static double[] scale(double[] data, double min, double max) {
		double[] scaledData = new double[data.length];

		for (int i = 0; i < data.length; i++) {
			double scaledValue = MIN_SCALED + ((data[i] - min) / (max - min)) * (MAX_SCALED - MIN_SCALED);
			scaledData[i] = (scaledValue);
		}
		return scaledData;
	}

	/**
	 * Rescales a single data point from the scaled range to the original range.
	 * This method rescales a single data point from the scaled range (defined by
	 * 'minScaled' and 'maxScaled') back to the original range, which is specified
	 * by 'minOriginal' and 'maxOriginal'. It performs the reverse scaling operation
	 * for a single data value.
	 *
	 * @param scaledData  The data point to be rescaled from the scaled range to the
	 *                    original range.
	 * @param minOriginal The minimum value of the training dataset (original data
	 *                    range).
	 * @param maxOriginal The maximum value of the training dataset (original data
	 *                    range).
	 * @return The rescaled data point in the original range.
	 */
	public static double scaleBack(double scaledData, double minOriginal, double maxOriginal) {
		return calc(scaledData, MIN_SCALED, MAX_SCALED, minOriginal, maxOriginal);
	}

	/**
	 * Scales back a list of double values from a scaled range to the original
	 * range. This method takes a list of scaled values and scales them back to
	 * their original range based on the specified minimum and maximum values of the
	 * original range.
	 *
	 * @param data        The list of double values to be scaled back.
	 * @param minOriginal The minimum value of the original range.
	 * @param maxOriginal The maximum value of the original range.
	 * @return A new ArrayList containing the scaled back values.
	 */
	public static ArrayList<Double> scaleBack(ArrayList<Double> data, double minOriginal, double maxOriginal) {
		ArrayList<Double> returnArr = new ArrayList<Double>();
		for (double val : data) {
			returnArr.add(calc(val, MIN_SCALED, MAX_SCALED, minOriginal, maxOriginal));
		}
		return returnArr;
	}

	/**
	 * * Scales back a list of double values from a scaled range to the original
	 * range. This method takes a list of scaled values and scales them back to
	 * their original range based on the specified minimum and maximum values of the
	 * original range.
	 * 
	 * @param data        The list of double values to be scaled back.
	 * @param minOriginal The minimum value of the original range.
	 * @param maxOriginal The maximum value of the original range.
	 * @return A new ArrayList containing the scaled back values.
	 */
	public static double[] scaleBack(double[] data, double minOriginal, double maxOriginal) {
		double[] returnArr = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			returnArr[i] = calc(data[i], MIN_SCALED, MAX_SCALED, minOriginal, maxOriginal);
		}
		return returnArr;
	}

	private static double calc(double valScaled, double minScaled, double maxScaled, double minOriginal,
			double maxOriginal) {
		return ((valScaled - minScaled) * (maxOriginal - minOriginal) / (maxScaled - minScaled)) //
				+ minOriginal;
	}

	/**
	 * Normalize a 2D array of data using standardization (z-score normalization).
	 * This method normalizes a 2D array of data by applying standardization
	 * (z-score normalization) to each row independently. The result is a new 2D
	 * array of normalized data.
	 *
	 * @param data            The 2D array of data to be normalized.
	 * @param hyperParameters instance of class HyperParameters
	 * @return A new 2D array containing the standardized (normalized) data.
	 */
	public static double[][] normalizeData(double[][] data, HyperParameters hyperParameters) {
		double[][] standData;
		standData = new double[data.length][data[0].length];// Here error
		for (int i = 0; i < data.length; i++) {
			standData[i] = standardize(data[i], hyperParameters);
		}
		return standData;
	}

	/**
	 * Normalizes the data based on the given target values, using standardization.
	 * This method calculates the standardization of each data point in the input
	 * data array with respect to the corresponding target value. It utilizes the
	 * mean and standard deviation of the input data array to perform the
	 * standardization.
	 * 
	 * @param data            The input data array containing the features to be
	 *                        normalized.
	 * @param target          The target values to which the data will be
	 *                        standardized.
	 * @param hyperParameters The hyperparameters required for normalization.
	 * @return A double array containing the normalized data.
	 */

	public static double[] normalizeData(double[][] data, double[] target, HyperParameters hyperParameters) {
		double[] standData;
		standData = new double[target.length];
		for (int i = 0; i < data.length; i++) {
			standData[i] = standardize(target[i], getMean(data[i]), getStandardDeviation(data[i]), hyperParameters);
		}
		return standData;

	}

	/**
	 * Standardizes a 1D array of data using Z-score normalization. This method
	 * standardizes a 1D array of data by applying Z-score normalization. It
	 * calculates the mean and standard deviation of the input data and then
	 * standardizes each data point.
	 *
	 * @param inputData       The 1D array of data to be standardized.
	 * @param hyperParameters instance of class HyperParameters
	 * @return A new 1D array containing the standardized (normalized) data.
	 */
	public static double[] standardize(double[] inputData, HyperParameters hyperParameters) {
		double meanCurrent = getMean(inputData);

		double stdDeviationCurrent = getStandardDeviation(inputData);
		double meanTarget = hyperParameters.getMean();
		double standerDeviationTarget = hyperParameters.getStanderDeviation();

		double[] standardizedData = new double[inputData.length];
		for (int i = 0; i < inputData.length; i++) {
			standardizedData[i] = meanTarget
					+ ((inputData[i] - meanCurrent) * (standerDeviationTarget / stdDeviationCurrent));
		}
		return standardizedData;
	}

	/**
	 * Standardizes a given input data point using mean and standard deviation. This
	 * method standardizes the input data point based on the provided mean and
	 * standard deviation of the current data and the target mean and standard
	 * deviation specified in the hyperparameters.
	 * 
	 * @param inputData       The input data point to be standardized.
	 * @param mean            The mean of the current data.
	 * @param standerdDev     The standard deviation of the current data.
	 * @param hyperParameters The hyperparameters containing the target mean and
	 *                        standard deviation.
	 * @return The standardized value of the input data point.
	 */

	public static double standardize(double inputData, double mean, double standerdDev,
			HyperParameters hyperParameters) {

		double meanCurrent = mean;

		double stdDeviationCurrent = standerdDev;
		double meanTarget = hyperParameters.getMean();
		double standerDeviationTarget = hyperParameters.getStanderDeviation();
		return meanTarget + ((inputData - meanCurrent) * (standerDeviationTarget / stdDeviationCurrent));

	}

	/**
	 * Reverse standardizes a data point that was previously standardized using
	 * Z-score normalization. This method reverses the standardization process for a
	 * single data point that was previously standardized using Z-score
	 * normalization. It requires the mean and standard deviation of the original
	 * data along with the Z-score value (zvalue) to perform the reverse
	 * standardization.
	 *
	 * @param mean              The mean of the original data.
	 * @param standardDeviation The standard deviation of the original data.
	 * @param zvalue            The Z-score value of the standardized data point.
	 * @param hyperParameters   instance of class HyperParameters
	 * @return The reverse standardized value in the original data's scale.
	 */

	public static double reverseStandrize(double zvalue, double mean, double standardDeviation,
			HyperParameters hyperParameters) {

		double reverseStand = 0;
		double meanTarget = hyperParameters.getMean();
		double standardDeviationTarget = hyperParameters.getStanderDeviation();

		reverseStand = ((zvalue - meanTarget) * (standardDeviation / standardDeviationTarget) + mean);
		return reverseStand;
	}

	/**
	 * Reverse standardizes a list of data points based on given mean, standard
	 * deviation, and hyperparameters. This method reverse standardizes each data
	 * point in the input list based on the provided mean, standard deviation, and
	 * hyperparameters. It returns a new list containing the reverse standardized
	 * values.
	 * 
	 * @param data            The list of data points to be reverse standardized.
	 * @param mean            The list of means corresponding to the data points.
	 * @param standDeviation  The list of standard deviations corresponding to the
	 *                        data points.
	 * @param hyperParameters The hyperparameters containing the target mean and
	 *                        standard deviation.
	 * @return A new list containing the reverse standardized values.
	 */

	public static double[] reverseStandrize(ArrayList<Double> data, ArrayList<Double> mean,
			ArrayList<Double> standDeviation, HyperParameters hyperParameters) {
		double[] revNorm = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			revNorm[i] = (reverseStandrize(data.get(i), mean.get(i), standDeviation.get(i), hyperParameters));

		}

		return revNorm;

	}

	public static double[] reverseStandrize(double[] data, double[] mean, double[] standDeviation,
			HyperParameters hyperParameters) {
		double[] revNorm = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			revNorm[i] = (reverseStandrize(data[i], mean[i], standDeviation[i], hyperParameters));

		}

		return revNorm;

	}

	/**
	 * Reverse standardizes a list of data points based on given mean, standard
	 * deviation, and hyperparameters. This method reverse standardizes each data
	 * point in the input list based on the provided mean, standard deviation, and
	 * hyperparameters. It returns a new list containing the reverse standardized
	 * values.
	 * 
	 * @param data            The list of data points to be reverse standardized.
	 * @param mean            The mean corresponding to the data points.
	 * @param standDeviation  The standard deviation corresponding to the data
	 *                        points.
	 * @param hyperParameters The hyperparameters containing the target mean and
	 *                        standard deviation.
	 * @return A new list containing the reverse standardized values.
	 */

	public static double[] reverseStandrize(ArrayList<Double> data, double mean, double standDeviation,
			HyperParameters hyperParameters) {
		double[] revNorm = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			revNorm[i] = (reverseStandrize(data.get(i), mean, standDeviation, hyperParameters));

		}

		return revNorm;

	}

	public static double[] reverseStandrize(double[] data, double mean, double standDeviation,
			HyperParameters hyperParameters) {
		double[] revNorm = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			revNorm[i] = (reverseStandrize(data[i], mean, standDeviation, hyperParameters));

		}

		return revNorm;

	}

	/**
	 * Modifies the given time-series data for long-term prediction by grouping it
	 * based on hours and minutes.
	 * 
	 * @param data The ArrayList of Double values representing the time-series data.
	 * @param date The ArrayList of OffsetDateTime objects corresponding to the
	 *             timestamps of the data.
	 * @return An ArrayList of ArrayLists of ArrayLists, representing the modified
	 *         data grouped by hours and minutes.
	 */

	public static ArrayList<ArrayList<ArrayList<Double>>> groupDataByHourAndMinute(ArrayList<Double> data,
			ArrayList<OffsetDateTime> date) {

		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<>();
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<>();

		GroupBy groupByHour = new GroupBy(data, date);
		groupByHour.hour();

		for (int i = 0; i < groupByHour.getGroupedDataByHour().size(); i++) {
			GroupBy groupByMinute = new GroupBy(groupByHour.getGroupedDataByHour().get(i),
					groupByHour.getGroupedDateByHour().get(i));

			groupByMinute.minute();
			dataGroupedByMinute.add(groupByMinute.getGroupedDataByMinute());
			dateGroupedByMinute.add(groupByMinute.getGroupedDateByMinute());
		}
		return dataGroupedByMinute;
	}

	/**
	 * Modify the data for trend term prediction.
	 * 
	 * @param data            The ArrayList of Double values data.
	 * @param date            The ArrayList of Double values date.
	 * @param hyperParameters The {@link HyperParameters}
	 * @return The ArrayList of modified values
	 */
	public static ArrayList<ArrayList<Double>> modifyFortrendPrediction(ArrayList<Double> data,
			ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {

		ArrayList<ArrayList<ArrayList<Double>>> firstModification = groupDataByHourAndMinute(data, date);

		// Flatten the structure of the first modification
		ArrayList<ArrayList<Double>> secondModification = flattenDataStructure(firstModification);

		// Apply windowing to create the third modification
		ArrayList<ArrayList<Double>> thirdModification = applyWindowing(secondModification, hyperParameters);

		return thirdModification;
	}

	private static ArrayList<ArrayList<Double>> flattenDataStructure(ArrayList<ArrayList<ArrayList<Double>>> data) {
		ArrayList<ArrayList<Double>> flattenedData = new ArrayList<>();

		for (ArrayList<ArrayList<Double>> hourData : data) {
			for (ArrayList<Double> minuteData : hourData) {
				flattenedData.add(minuteData);
			}
		}
		return flattenedData;
	}

	private static ArrayList<ArrayList<Double>> applyWindowing(ArrayList<ArrayList<Double>> data,
			HyperParameters hyperParameters) {
		ArrayList<ArrayList<Double>> windowedData = new ArrayList<>();
		int windowSize = hyperParameters.getWindowSizeTrend();

		for (int i = 0; i < data.size(); i++) {
			ArrayList<ArrayList<Double>> toCombine = new ArrayList<>();

			for (int j = 0; j <= windowSize; j++) {
				int index = (j + i) % data.size();
				toCombine.add(data.get(index));
			}
			windowedData.add(combinedArray(toCombine));
		}
		return windowedData;
	}

	/**
	 * Flatten the array by combining.
	 * 
	 * @param values The ArrayList of Double values.
	 * @return reGroupedsecond Teh Flattened ArrayList
	 */
	public static ArrayList<Double> combinedArray(ArrayList<ArrayList<Double>> values) {
		int minSize = values.stream()//
				.mapToInt(ArrayList::size)//
				.min()//
				.orElse(0);

		ArrayList<Double> reGroupedsecond = new ArrayList<>();

		for (int i = 0; i < minSize; i++) {
			for (ArrayList<Double> innerList : values) {
				reGroupedsecond.add(innerList.get(i));
			}
		}
		return reGroupedsecond;
	}

	/**
	 * Splits a list of Double values into multiple batches and returns the batches.
	 * The method divides the original list into a specified number of groups,
	 * ensuring that each group has an approximately equal number of elements. It
	 * handles any remainder by distributing the extra elements among the first few
	 * groups.
	 *
	 * @param originalList   The original list of Double values to be split into
	 *                       batches.
	 * @param numberOfGroups The desired number of groups to split the list into.
	 * @return An ArrayList of ArrayLists, where each inner ArrayList represents a
	 *         batch of Double values.
	 */

	public static ArrayList<ArrayList<Double>> getDataInBatch(ArrayList<Double> originalList, int numberOfGroups) {
		ArrayList<ArrayList<Double>> splitGroups = new ArrayList<>();

		int originalSize = originalList.size();
		int groupSize = originalSize / numberOfGroups;
		int remainder = originalSize % numberOfGroups;

		int currentIndex = 0;
		for (int i = 0; i < numberOfGroups; i++) {
			int groupCount = groupSize + (i < remainder ? 1 : 0);
			ArrayList<Double> group = new ArrayList<>(originalList.subList(currentIndex, currentIndex + groupCount));
			splitGroups.add(group);
			currentIndex += groupCount;
		}
		return splitGroups;
	}

	/**
	 * Splits a list of OffsetDateTime into multiple batches and returns the
	 * batches. The method divides the original list into a specified number of
	 * groups, ensuring that each group has an approximately equal number of
	 * elements. It handles any remainder by distributing the extra elements among
	 * the first few groups.
	 *
	 * @param originalList   The original list of OffsetDateTime to be split into
	 *                       batches.
	 * @param numberOfGroups The desired number of groups to split the list into.
	 * @return An ArrayList of ArrayLists, where each inner ArrayList represents a
	 *         batch of OffsetDateTime objects.
	 */
	public static ArrayList<ArrayList<OffsetDateTime>> getDateInBatch(ArrayList<OffsetDateTime> originalList,
			int numberOfGroups) {
		ArrayList<ArrayList<OffsetDateTime>> splitGroups = new ArrayList<>();

		int originalSize = originalList.size();
		int groupSize = originalSize / numberOfGroups;
		int remainder = originalSize % numberOfGroups;

		int currentIndex = 0;
		for (int i = 0; i < numberOfGroups; i++) {
			int groupCount = groupSize + (i < remainder ? 1 : 0);
			ArrayList<OffsetDateTime> group = new ArrayList<>(
					originalList.subList(currentIndex, currentIndex + groupCount));
			splitGroups.add(group);
			currentIndex += groupCount;
		}

		return splitGroups;
	}

	/**
	 * Removes negative values from the given ArrayList of Doubles by replacing them
	 * with 0.
	 *
	 * @param data The ArrayList of Doubles containing numeric values.
	 * @return ArrayList&lt;Double&gt; A new ArrayList&lt;Double&gt; with negative
	 *         values replaced by zero.
	 */

	public static ArrayList<Double> removeNegatives(ArrayList<Double> data) {
		return data.stream()//
				// Replace negative values with 0
				.map(value -> value == null || Double.isNaN(value) ? Double.NaN : Math.max(value, 0))
				.collect(Collectors.toCollection(ArrayList::new));

	}

	/**
	 * Replaces all negative values in the input array with 0. NaN values in the
	 * array remain unchanged.
	 *
	 * @param data the input array of doubles
	 * @return a new array with negative values replaced by 0
	 */

	public static double[] removeNegatives(double[] data) {
		return Arrays.stream(data) 
				.map(value -> Double.isNaN(value) ? Double.NaN : Math.max(value, 0)) 
				.toArray(); 
	}

	/**
	 * Scales each element in the input ArrayList by a specified scaling factor.
	 *
	 * @param data          The ArrayList of Double values to be scaled.
	 * @param scalingFactor The factor by which each element in the data ArrayList
	 *                      will be multiplied.
	 * @return A new ArrayList containing the scaled values.
	 */
	public static ArrayList<Double> constantScaling(ArrayList<Double> data, double scalingFactor) {
		return data.stream().map(val -> val * scalingFactor).collect(Collectors.toCollection(ArrayList::new));
	}

	public static double[] constantScaling(double[] data, double scalingFactor) {
		return Arrays.stream(data).map(val -> val * scalingFactor).toArray();
	}

	/**
	 * Reshapes a three-dimensional ArrayList into a four-dimensional ArrayList
	 * structure. This method takes a three-dimensional ArrayList of data and
	 * reshapes it into a four-dimensional ArrayList structure. The reshaping is
	 * performed by dividing the original data into blocks of size 4x24. The
	 * resulting four-dimensional ArrayList contains these blocks.
	 *
	 * @param dataList        The three-dimensional ArrayList to be reshaped.
	 * @param hyperParameters is the object of class HyperPrameters.
	 * @return A four-dimensional ArrayList structure containing the reshaped data.
	 */

	public static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> reshape(
			ArrayList<ArrayList<ArrayList<Double>>> dataList, HyperParameters hyperParameters) {

		int m = 60 / hyperParameters.getInterval() * 24;
		int n = dataList.size() / m;
		int o = 0;
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> temp2 = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		for (int i = 0; i < n; i++) {
			ArrayList<ArrayList<ArrayList<Double>>> temp1 = new ArrayList<ArrayList<ArrayList<Double>>>();
			for (int j = 0; j < m; j++) {
				temp1.add(dataList.get(o));
				o = o + 1;
			}
			temp2.add(temp1);
		}
		return temp2;
	}

	/**
	 * Decreases the dimensionality of a 4D ArrayList to a 3D ArrayList. This method
	 * flattens the input 4D ArrayList to a 3D ArrayList by merging the innermost
	 * ArrayLists into one. It returns the resulting 3D ArrayList.
	 * 
	 * @param model The 4D ArrayList to decrease in dimensionality.
	 * @return The resulting 3D ArrayList after decreasing the dimensionality.
	 */

	public static ArrayList<ArrayList<ArrayList<Double>>> decraseDimension(
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> model) {
		ArrayList<ArrayList<ArrayList<Double>>> result = new ArrayList<ArrayList<ArrayList<Double>>>();
		for (int i = 0; i < model.size(); i++) {
			for (int j = 0; j < model.get(i).size(); j++) {
				result.add(model.get(i).get(j));
			}
		}

		return result;

	}

	/**
	 * Updates the model with the specified weights based on the given indices and
	 * model type. This method extracts the optimum weights from the provided 4D
	 * ArrayList of models using the given indices and model type. It updates the
	 * hyperparameters with the extracted weights based on the model type.
	 * 
	 * @param allModel        The 4D ArrayList containing all models.
	 * @param index           The list of indices specifying the location of optimum
	 *                        weights in the models.
	 * @param fileName        The name of the file to save the final model.
	 * @param modelType       The type of the model ("trend.txt" or
	 *                        "seasonality.txt").
	 * @param hyperParameters The hyperparameters to update with the extracted
	 *                        weights.
	 */

	public static void updateModel(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel,
			List<List<Integer>> index, String fileName, String modelType, HyperParameters hyperParameters) {

		ArrayList<ArrayList<ArrayList<Double>>> optimumWeight = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();

		for (int i = 0; i < index.size(); i++) {
			ArrayList<ArrayList<Double>> temp1 = allModel.get(index.get(i).get(0)).get(index.get(i).get(1));
			optimumWeight.add(temp1);
		}
		finalWeight.add(optimumWeight);
		// SaveModel.saveModels(finalWeight, fileName);

		switch (modelType) {
		case "trend.txt":
			hyperParameters.updatModelTrend(optimumWeight);
			break;
		case "seasonality.txt":
			hyperParameters.updateModelSeasonality(optimumWeight);
			break;
		}

	}

	/**
	 * Performs element-wise multiplication of two arrays.
	 *
	 * @param featureA the first array
	 * @param featureB the second array
	 * @return a new array where each element is the product of the corresponding
	 *         elements of featureA and featureB
	 * @throws IllegalArgumentException if the input arrays are of different lengths
	 */
	public static double[] elementWiseMultiplication(double[] featureA, double[] featureB) {
		if (featureA.length != featureB.length) {
			throw new IllegalArgumentException("The input arrays must have the same length.");
		}
		return IntStream.range(0, featureA.length).mapToDouble(i -> featureA[i] * featureB[i]).toArray();
	}

	/**
	 * Performs element-wise division of two arrays. If an element in featureB is
	 * zero, the corresponding element in the result will be zero.
	 *
	 * @param featureA the first array
	 * @param featureB the second array
	 * @return a new array where each element is the result of dividing the
	 *         corresponding elements of featureA by featureB or zero if the element
	 *         in featureB is zero
	 * @throws IllegalArgumentException if the input arrays are of different lengths
	 */
	public static double[] elementWiseDiv(double[] featureA, double[] featureB) {
		if (featureA.length != featureB.length) {
			throw new IllegalArgumentException("The input arrays must have the same length.");
		}
		return IntStream.range(0, featureA.length).mapToDouble(i -> (featureB[i] == 0) ? 0 : featureA[i] / featureB[i])
				.toArray();
	}

}
