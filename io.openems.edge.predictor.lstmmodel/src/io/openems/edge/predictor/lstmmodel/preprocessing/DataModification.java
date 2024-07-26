package io.openems.edge.predictor.lstmmodel.preprocessing;

import static io.openems.edge.predictor.lstmmodel.common.DataStatistics.getMean;
import static io.openems.edge.predictor.lstmmodel.common.DataStatistics.getStandardDeviation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

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
		return data.stream()//
				.map(value -> MIN_SCALED + ((value - min) / (max - min)) * (MAX_SCALED - MIN_SCALED))
				.collect(Collectors.toCollection(ArrayList::new));
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
		return Arrays.stream(data)//
				.map(value -> MIN_SCALED + ((value - min) / (max - min)) * (MAX_SCALED - MIN_SCALED))//
				.toArray();
	}

	/**
	 * Re-scales a single data point from the scaled range to the original range.
	 * This method re-scales a single data point from the scaled range (defined by
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
		return calculateScale(scaledData, MIN_SCALED, MAX_SCALED, minOriginal, maxOriginal);
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
		return data.stream()//
				.map(value -> calculateScale(value, MIN_SCALED, MAX_SCALED, minOriginal, maxOriginal))//
				.collect(Collectors.toCollection(ArrayList::new));
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
		return Arrays.stream(data)//
				.map(value -> calculateScale(value, MIN_SCALED, MAX_SCALED, minOriginal, maxOriginal))//
				.toArray();
	}

	/**
	 * Scales a value from a scaled range back to the original range.
	 *
	 * @param valScaled   The value in the scaled range to be converted back to the
	 *                    original range.
	 * @param minScaled   The minimum value of the scaled range.
	 * @param maxScaled   The maximum value of the scaled range.
	 * @param minOriginal The minimum value of the original range.
	 * @param maxOriginal The maximum value of the original range.
	 * @return The value converted back to the original range.
	 */
	private static double calculateScale(double valScaled, double minScaled, double maxScaled, double minOriginal,
			double maxOriginal) {
		return ((valScaled - minScaled) * (maxOriginal - minOriginal) / (maxScaled - minScaled)//
		) + minOriginal;
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
	 * @param hyperParameters The {@link HyperParameters} required for
	 *                        normalization.
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
	 * @param hyperParameters instance of {@link HyperParameters}
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
	 * deviation specified in the {@link HyperParameters}.
	 * 
	 * @param inputData       The input data point to be standardized.
	 * @param mean            The mean of the current data.
	 * @param standerdDev     The standard deviation of the current data.
	 * @param hyperParameters The {@link HyperParameters} containing the target mean
	 *                        and standard deviation.
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
	 * @param hyperParameters   instance of {@link HyperParameters}
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
	 * deviation, and {@link HyperParameters}. This method reverse standardizes each
	 * data point in the input list based on the provided mean, standard deviation,
	 * and {@link HyperParameters}. It returns a new Array containing the reverse
	 * standardized values.
	 * 
	 * @param data            The list of data points to be reverse standardized.
	 * @param mean            The list of means corresponding to the data points.
	 * @param standDeviation  The list of standard deviations corresponding to the
	 *                        data points.
	 * @param hyperParameters The {@link HyperParameters} containing the target mean
	 *                        and standard deviation.
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

	/**
	 * Reverse standardizes a list of data points based on given mean, standard
	 * deviation, and {@link HyperParameters}. This method reverse standardizes each
	 * data point in the input list based on the provided mean, standard deviation,
	 * and {@link HyperParameters}. It returns a new list containing the reverse
	 * standardized values.
	 * 
	 * @param data            The Array of data points to be reverse standardized.
	 * @param mean            The Array of means corresponding to the data points.
	 * @param standDeviation  The Array of standard deviations corresponding to the
	 *                        data points.
	 * @param hyperParameters The {@link HyperParameters} containing the target mean
	 *                        and standard deviation.
	 * @return A new Array containing the reverse standardized values.
	 */
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
	 * deviation, and {@link HyperParameters}. This method reverse standardizes each
	 * data point in the input list based on the provided mean, standard deviation,
	 * and {@link HyperParameters}. It returns a new Array containing the reverse
	 * standardized values.
	 * 
	 * @param data            The Array of data points to be reverse standardized.
	 * @param mean            The mean corresponding to the data points.
	 * @param standDeviation  The standard deviation corresponding to the data
	 *                        points.
	 * @param hyperParameters The {@link HyperParameters} containing the target mean
	 *                        and standard deviation.
	 * @return A new Array containing the reverse standardized values.
	 */
	public static double[] reverseStandrize(ArrayList<Double> data, double mean, double standDeviation,
			HyperParameters hyperParameters) {
		double[] revNorm = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			revNorm[i] = (reverseStandrize(data.get(i), mean, standDeviation, hyperParameters));
		}
		return revNorm;
	}

	/**
	 * Reverse standardizes a list of data points based on given mean, standard
	 * deviation, and {@link HyperParameters}. This method reverse standardizes each
	 * data point in the input list based on the provided mean, standard deviation,
	 * and {@link HyperParameters}. It returns a new list containing the reverse
	 * standardized values.
	 * 
	 * @param data            The list of data points to be reverse standardized.
	 * @param mean            The mean corresponding to the data points.
	 * @param standDeviation  The standard deviation corresponding to the data
	 *                        points.
	 * @param hyperParameters The {@link HyperParameters} containing the target mean
	 *                        and standard deviation.
	 * @return A new list containing the reverse standardized values.
	 */
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
	 * @param data The {@link ArrayList} of Double values representing the
	 *             time-series data.
	 * @param date The {@link ArrayList} of OffsetDateTime objects corresponding to
	 *             the timestamps of the data.
	 * @return An {@link ArrayList} of {@link ArrayList} of {@link ArrayList},
	 *         representing the modified data grouped by hours and minutes.
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
		ArrayList<ArrayList<Double>> secondModification = flatten3dto2d(firstModification);

		// Apply windowing to create the third modification
		ArrayList<ArrayList<Double>> thirdModification = applyWindowing(secondModification, hyperParameters);

		return thirdModification;
	}

	private static ArrayList<ArrayList<Double>> flatten3dto2d(//
			ArrayList<ArrayList<ArrayList<Double>>> data) {
		return data.stream()//
				.flatMap(twoDList -> twoDList.stream())//
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Decreases the dimensionality of a 4D ArrayList to a 3D ArrayList. This method
	 * flattens the input 4D ArrayList to a 3D ArrayList by merging the innermost
	 * ArrayLists into one. It returns the resulting 3D ArrayList.
	 * 
	 * @param model The 4D ArrayList to decrease in dimensionality.
	 * @return The resulting 3D ArrayList after decreasing the dimensionality.
	 */
	public static ArrayList<ArrayList<ArrayList<Double>>> flattern4dto3d(
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> model) {

		return model.stream()//
				.flatMap(threeDList -> threeDList.stream())//
				.collect(Collectors.toCollection(ArrayList::new));
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
		return Arrays.stream(data)//
				.map(value -> Double.isNaN(value) ? Double.NaN : Math.max(value, 0))//
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

	/**
	 * Scales each element in the input ArrayList by a specified scaling factor.
	 *
	 * @param data          The Array of Double values to be scaled.
	 * @param scalingFactor The factor by which each element in the data Array will
	 *                      be multiplied.
	 * @return A new Array containing the scaled values.
	 */
	public static double[] constantScaling(double[] data, double scalingFactor) {
		return Arrays.stream(data).map(val -> val * scalingFactor).toArray();
	}

	/**
	 * Reshapes a 3D ArrayList into a 4D ArrayList structure. This method takes a
	 * three-dimensional ArrayList of data and reshapes it into a four-dimensional
	 * ArrayList structure. The reshaping is performed by dividing the original data
	 * into blocks of size 4x24. The resulting four-dimensional ArrayList contains
	 * these blocks.
	 *
	 *
	 * @param dataList        The 3D list to be reshaped.
	 * @param hyperParameters The hyperparameters containing the interval used to
	 *                        reshape the list.
	 * @return A reshaped 4D list.
	 */
	public static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> reshape(
			ArrayList<ArrayList<ArrayList<Double>>> dataList, HyperParameters hyperParameters) {

		// Calculate the dimensions for reshaping
		int rowsPerDay = 60 / hyperParameters.getInterval() * 24;
		int numDays = dataList.size() / rowsPerDay;

		// Initialize the reshaped 4D list
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> reshapedData = new ArrayList<>();

		int dataIndex = 0;
		for (int day = 0; day < numDays; day++) {
			ArrayList<ArrayList<ArrayList<Double>>> dailyData = new ArrayList<>();
			for (int row = 0; row < rowsPerDay; row++) {
				dailyData.add(dataList.get(dataIndex));
				dataIndex++;
			}
			reshapedData.add(dailyData);
		}

		return reshapedData;
	}

	/**
	 * Updates the model with the specified weights based on the given indices and
	 * model type. This method extracts the optimum weights from the provided 4D
	 * ArrayList of models using the given indices and model type. It updates the
	 * hyperparameters with the extracted weights based on the model type.
	 * 
	 * @param allModel        The 4D ArrayList containing all models.
	 * @param indices         The list of indices specifying the location of optimum
	 *                        weights in the models.
	 * @param fileName        The name of the file to save the final model.
	 * @param modelType       The type of the model ("trend.txt" or
	 *                        "seasonality.txt").
	 * @param hyperParameters The hyperparameters to update with the extracted
	 *                        weights.
	 */
	public static void updateModel(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel, //
			List<List<Integer>> indices, //
			String fileName, //
			String modelType, //
			HyperParameters hyperParameters) {

		ArrayList<ArrayList<ArrayList<Double>>> optimumWeights = new ArrayList<ArrayList<ArrayList<Double>>>();

		for (List<Integer> idx : indices) {
			ArrayList<ArrayList<Double>> tempWeights = allModel//
					.get(idx.get(0))//
					.get(idx.get(1));
			optimumWeights.add(tempWeights);
		}

		switch (modelType.toLowerCase()) {
		case "trend":
			hyperParameters.updatModelTrend(optimumWeights);
			break;
		case "seasonality":
			hyperParameters.updateModelSeasonality(optimumWeights);
			break;
		default:
			throw new IllegalArgumentException("Invalid model type: " + modelType);
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
		return IntStream.range(0, featureA.length)//
				.mapToDouble(i -> featureA[i] * featureB[i])//
				.toArray();
	}

	/**
	 * Performs element-wise multiplication of two ArrayLists.
	 *
	 * @param featureA the first ArrayList
	 * @param featureB the second ArrayList
	 * @return a new ArrayList where each element is the result of multiplying the
	 *         corresponding elements of featureA and featureB
	 * @throws IllegalArgumentException if the input ArrayLists are of different
	 *                                  lengths
	 */
	public static ArrayList<Double> elementWiseMultiplication(ArrayList<Double> featureA, ArrayList<Double> featureB) {
		if (featureA.size() != featureB.size()) {
			throw new IllegalArgumentException("The input ArrayLists must have the same length.");
		}
		ArrayList<Double> result = new ArrayList<>();
		IntStream.range(0, featureA.size()).forEach(i -> result.add(featureA.get(i) * featureB.get(i)));
		return result;
	}

	/**
	 * Performs element-wise division of two ArrayLists. If an element in featureB
	 * is zero, the corresponding element in the result will be zero.
	 *
	 * @param featureA the first ArrayList
	 * @param featureB the second ArrayList
	 * @return a new ArrayList where each element is the result of dividing the
	 *         corresponding elements of featureA by featureB or zero if the element
	 *         in featureB is zero
	 * @throws IllegalArgumentException if the input ArrayLists are of different
	 *                                  lengths
	 */
	public static ArrayList<Double> elementWiseDiv(ArrayList<Double> featureA, ArrayList<Double> featureB) {
		if (featureA.size() != featureB.size()) {
			throw new IllegalArgumentException("The input ArrayLists must have the same length.");
		}
		ArrayList<Double> result = new ArrayList<>();
		IntStream.range(0, featureA.size())
				.forEach(i -> result.add((featureB.get(i) == 0) ? featureA.get(i) : featureA.get(i) / featureB.get(i)));
		return result;
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
		return IntStream.range(0, featureA.length)//
				.mapToDouble(i -> (featureB[i] == 0) ? featureA[i] : featureA[i] / featureB[i])//
				.toArray();
	}

}
