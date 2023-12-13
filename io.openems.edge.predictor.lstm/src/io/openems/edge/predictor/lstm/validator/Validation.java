package io.openems.edge.predictor.lstm.validator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.DataStatistics;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstm.preprocessing.TrainTestSplit;
import io.openems.edge.predictor.lstm.utilities.MathUtils;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class Validation {
	private String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
	// private String pathTrend =
	// "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";

	public Validation(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {

		// this.validateSeasonality(data, date, hyperParameters);

		this.validateTrend(data, date, hyperParameters);
		// this.validateTrend(data, date, hyperParameters);

	}

	/**
	 * Validates in a time series of data using a given set of values and dates.
	 * This method performs the following steps: 1. Interpolates the input values to
	 * ensure a consistent time interval. 2. Calculates the minimum and maximum
	 * values in the interpolated data. 3. Groups the interpolated data and dates by
	 * hour. 4. Further groups the data and dates by minute within each hour.
	 *
	 * @param values          An ArrayList of Double values representing the time
	 *                        series data.
	 * @param dates           An ArrayList of OffsetDateTime objects corresponding
	 *                        to the timestamps of the data.
	 * @param hyperParameters An instance of class HyperParameters
	 * 
	 */

	public void validateSeasonality(ArrayList<Double> values, ArrayList<OffsetDateTime> dates,
			HyperParameters hyperParameters) {

		// ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new
		// ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();
		// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = new
		// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		int windowsSize = 0;

		windowsSize = hyperParameters.getWindowSizeSeasonality();
		hyperParameters.setModleSuffix("seasonality.txt");

		double minOfTrainingData;
		double maxOfTrainingData;

		/**
		 * compute interpolation
		 */
		InterpolationManager inter = new InterpolationManager(values, dates, hyperParameters);

		minOfTrainingData = hyperParameters.getScalingMin();
		maxOfTrainingData = hyperParameters.getScalingMax();

		/**
		 * Grouping the interpolated data by hour
		 */

		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = DataModification
				.modifyFroLongTermPrediction(inter.getInterpolatedData(), dates);

		/**
		 * Read Model //
		 */

		// ReadModels models = new
		// ReadModels(pathSeasonality+Integer.toString(itterNumb)+"seasonality.txt");
		// if (hyperParameters.getCount() > 0 && hyperParameters.getCount() % 100 == 0
		// || hyperParameters.getCount() == 26) {
		// System.out.println("Combining Models");
		//
		// String path = this.path + Integer.toString(hyperParameters.getCount()) +
		// hyperParameters.getModleSuffix();
		// allModels = ReadModels.getModelForSeasonality(path, hyperParameters);
		// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> oldModels = this
		// .getOldModelsseasonality(hyperParameters);
		// // ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> combinedModels =
		// // combineModelsSeasonality(oldModels,allModels);
		// allModels = oldModels;
		//
		// } else {

		String path = this.path + Integer.toString(hyperParameters.getCount()) + hyperParameters.getModleSuffix();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = ReadModels.getModelForSeasonality(path,
				hyperParameters);

		// }

		for (int h = 0; h < allModels.size(); h++) {

			ArrayList<Double> rmsTemp1 = new ArrayList<Double>();

			int k = 0;
			for (int i = 0; i < dataGroupedByMinute.size(); i++) {

				for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {

					PreProcessingImpl preprocessing = new PreProcessingImpl(DataModification.scale(
							dataGroupedByMinute.get(i).get(j), minOfTrainingData, maxOfTrainingData), windowsSize);
					PreProcessingImpl tempObj = new PreProcessingImpl(dataGroupedByMinute.get(i).get(j), windowsSize);

					try {

						TrainTestSplit splitIndex = new TrainTestSplit(dataGroupedByMinute.get(i).get(j).size(),
								windowsSize, hyperParameters.getDataSplitTrain(),
								hyperParameters.getDataSplitValidate());

						double[][] validateData = preprocessing.getFeatureData(splitIndex.getTrainLowerIndex(),
								splitIndex.getTrainUpperIndex());

						double[] tempTarget = tempObj.getTargetData(splitIndex.getTrainLowerIndex(),
								splitIndex.getTrainUpperIndex());

						// Suffle obj2 = new Suffle(validateData, validateTarget);
						ArrayList<ArrayList<Double>> val = allModels.get(h).get(k);

						ArrayList<Double> result = predictPre(validateData, val, minOfTrainingData, maxOfTrainingData);

						double rms = PerformanceMatrix
								.rmsError(UtilityConversion.convert1DArrayTo1DArrayList(tempTarget), result);

						rmsTemp1.add(rms);

						k = k + 1;
					} catch (Exception e) {
						e.printStackTrace();

					}
				}

			}
			rmsTemp2.add(rmsTemp1);

		}

		List<List<Integer>> optInd = findOptimumIndex(rmsTemp2);

		System.out.println("Optimum Index :" + optInd);
		ReadModels.updateModel(allModels, optInd,
				Integer.toString(hyperParameters.getCount()) + hyperParameters.getModleSuffix());

	}

	/**
	 * Validates and combines trend models for a given set of data and iteration
	 * number. This method performs validation of trend models by comparing their
	 * root mean square (RMS) errors with the validation data. It also combines
	 * trend models under specific conditions. The best-performing model is then
	 * saved for future use.
	 *
	 * @param values          The list of input values used for trend modeling.
	 * @param dates           The list of corresponding dates for the input values
	 *                        (not used in this method).
	 * @param hyperParameters The Instance of class HyperParameters
	 */
	public void validateTrend(ArrayList<Double> values, ArrayList<OffsetDateTime> dates,
			HyperParameters hyperParameters) {
		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();

		hyperParameters.setModleSuffix("trend.txt");

		double minOfTrainingData;
		double maxOfTrainingData;

		/**
		 * compute interpolation
		 */
		InterpolationManager inter = new InterpolationManager(values, dates, hyperParameters);
		minOfTrainingData = hyperParameters.getScalingMin();
		maxOfTrainingData = hyperParameters.getScalingMax();
		ArrayList<ArrayList<Double>> modifiedData = DataModification
				.modifyForShortTermPrediction(inter.getInterpolatedData(), dates, hyperParameters);

		String path = this.path + Integer.toString(hyperParameters.getCount()) + hyperParameters.getModleSuffix();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = ReadModels.getModelForSeasonality(path,
				hyperParameters);

		for (int i = 0; i < allModels.size(); i++) {
			ArrayList<Double> rmsTemp1 = new ArrayList<Double>();
			for (int j = 0; j < modifiedData.size(); j++) {
				double[][] validateData = PreProcessingImpl.groupToStiffedWindow(DataModification
						.scale(modifiedData.get(j), hyperParameters.getScalingMin(), hyperParameters.getScalingMax()),
						hyperParameters.getWindowSizeTrend());
				double[] validationTarget = PreProcessingImpl.groupToStiffedTarget(modifiedData.get(j),
						hyperParameters.getWindowSizeTrend());

				ArrayList<ArrayList<Double>> val = allModels.get(i).get(j);

				ArrayList<Double> result = predictPre(validateData, val, minOfTrainingData, maxOfTrainingData);

				double rms = PerformanceMatrix.rmsError(UtilityConversion.convert1DArrayTo1DArrayList(validationTarget),
						result);

				rmsTemp1.add(rms);

			}
			rmsTemp2.add(rmsTemp1);

		}
		List<List<Integer>> optInd = findOptimumIndex(rmsTemp2);

		System.out.println("Optimum Index :" + optInd);
		ReadModels.updateModel(allModels, optInd,
				Integer.toString(hyperParameters.getCount()) + hyperParameters.getModleSuffix());

	}

	/**
	 * Find the indices of the minimum values in each column of a 2D matrix. This
	 * method takes a 2D matrix represented as a List of Lists and finds the row
	 * indices of the minimum values in each column. The result is returned as a
	 * List of Lists, where each inner list contains two integers: the row index and
	 * column index of the minimum value.
	 *
	 * @param matrix A 2D matrix represented as a List of Lists of doubles.
	 * @return A List of Lists containing the row and column indices of the minimum
	 *         values in each column. If the input matrix is empty, an empty list is
	 *         returned.
	 */

	public static List<List<Integer>> findOptimumIndex(ArrayList<ArrayList<Double>> matrix) {
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

	/**
	 * Find the indices of the minimum values in each column of a 2D matrix. This
	 * method takes a 2D matrix represented as a List of Lists and finds the row
	 * indices of the minimum values in each column. The result is returned as a
	 * List of Lists, where each inner list contains two integers: the row index and
	 * column index of the minimum value.
	 *
	 * @param matrix A 2D matrix represented as a List of Lists of doubles.
	 * @param var    A parameter to insure the implementation of function
	 *               overloading.
	 * @return A List of Lists containing the row and column indices of the minimum
	 *         values in each column. If the input matrix is empty, an empty list is
	 *         returned.
	 * 
	 */

	public static List<List<Integer>> findOptimumIndex(ArrayList<ArrayList<Double>> matrix, String var) {
		List<List<Integer>> minimumIndices = new ArrayList<>();

		if (matrix.isEmpty() || matrix.get(0).isEmpty()) {
			return minimumIndices; // Empty matrix, return empty list
		}

		int numColumns = matrix.get(0).size();

		for (int col = 0; col < numColumns; col++) {
			double max = matrix.get(0).get(col);
			List<Integer> maxIndices = new ArrayList<>(Arrays.asList(0, col));

			for (int row = 0; row < matrix.size(); row++) {
				double value = matrix.get(row).get(col);

				if (value > max) {
					max = value;
					maxIndices.set(0, row);
				}
			}

			minimumIndices.add(maxIndices);
		}
		for (int i = 0; i < minimumIndices.size(); i++) {
			System.out.println(matrix.get(minimumIndices.get(i).get(0)).get(minimumIndices.get(i).get(1)));

		}

		return minimumIndices;
	}

	/**
	 * Estimate the optimum weight index from a list of indices. This method takes a
	 * list of indices and estimates the optimum weight index by finding the value
	 * with the maximum count among the provided indices.
	 *
	 * @param index A list of indices represented as a List of Lists of integers.
	 *              Each inner list is expected to contain at least one integer.
	 * @return An integer representing the estimated optimum weight index. If the
	 *         input list is empty, the result may be null.
	 */

	public static Integer estimateOptimumWeightIndex(List<List<Integer>> index) {

		Integer toReturn;
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for (int i = 0; i < index.size(); i++) {
			temp.add(index.get(i).get(0));
		}
		toReturn = findValueWithMaxCount(temp);
		return toReturn;
	}

	/**
	 * Find the value with the maximum count in a list of integers. This method
	 * takes a list of integers and determines the value with the highest count
	 * (mode) within the list.
	 *
	 * @param numbers A list of integers to analyze. It may be empty, but not null.
	 * @return The integer value with the maximum count in the list. If the input
	 *         list is empty, the result may be null.
	 */

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

	/**
	 * Predict the output values based on input data and model parameters. This
	 * method takes input data and a set of model parameters and predicts output
	 * values for each data point using the model.
	 *
	 * @param data              A 2D array representing the input data where each
	 *                          row is a data point.
	 * @param val               An ArrayList containing model parameters, including
	 *                          weight vectors and activation values. The ArrayList
	 *                          should contain the following sublists in this order:
	 *                          0: Input weight vector (wi) 1: Output weight vector
	 *                          (wo) 2: Recurrent weight vector (wz) 3: Recurrent
	 *                          input activations (rI) 4: Recurrent output
	 *                          activations (rO) 5: Recurrent update activations
	 *                          (rZ) 6: Current output (yt) 7: Current cell state
	 *                          (ct)
	 * @param minOfTrainingData Minimum value, used as reference for scaling the
	 *                          data
	 * @param maxOfTrainingData Maximum value, used as reference for scaling the
	 *                          data
	 * 
	 * @return An ArrayList of Double values representing the predicted output for
	 *         each input data point.
	 * 
	 */

	public static ArrayList<Double> predictPre(double[][] data, ArrayList<ArrayList<Double>> val,
			double minOfTrainingData, double maxOfTrainingData) {

		ArrayList<Double> result = new ArrayList<Double>();
		for (int i = 0; i < data.length; i++) {
			ArrayList<Double> wi = val.get(0);
			ArrayList<Double> wo = val.get(1);
			ArrayList<Double> wz = val.get(2);
			ArrayList<Double> rI = val.get(3);
			ArrayList<Double> rO = val.get(4);
			ArrayList<Double> rZ = val.get(5);
			ArrayList<Double> yt = val.get(6);
			ArrayList<Double> ct = val.get(7);

			result.add(predict(data[i], wi, wo, wz, rI, rO, rZ, yt, ct, maxOfTrainingData, minOfTrainingData));
		}

		return result;
	}

	/**
	 * Predict an output value based on input data and model parameters. This method
	 * takes input data, along with a set of model parameters, and predicts a single
	 * output value using a recurrent neural network (RNN) model.
	 *
	 * @param data              An array of doubles representing the input data.
	 * @param wi                An ArrayList of doubles representing the input
	 *                          weight vector (wi) for the RNN model.
	 * @param wo                An ArrayList of doubles representing the output
	 *                          weight vector (wo) for the RNN model.
	 * @param wz                An ArrayList of doubles representing the recurrent
	 *                          weight vector (wz) for the RNN model.
	 * @param rI                An ArrayList of doubles representing the recurrent
	 *                          input activations (rI) for the RNN model.
	 * @param rO                An ArrayList of doubles representing the recurrent
	 *                          output activations (rO) for the RNN model.
	 * @param rZ                An ArrayList of doubles representing the recurrent
	 *                          update activations (rZ) for the RNN model.
	 * @param ytl               An ArrayList of doubles representing the current
	 *                          output (yt) for the RNN model.
	 * @param ctl               An ArrayList of doubles representing the current
	 *                          cell state (ct) for the RNN model.
	 * @param minOfTrainingData Minimum value, used as reference for scaling the
	 *                          data
	 * @param maxOfTrainingData Maximum value, used as reference for scaling the
	 *                          data
	 * @return A double representing the predicted output value based on the input
	 *         data and model parameters.
	 */

	public static double predict(double[] data, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> rI, ArrayList<Double> rO, ArrayList<Double> rZ, ArrayList<Double> ytl,
			ArrayList<Double> ctl, double maxOfTrainingData, double minOfTrainingData) {
		double ct = 0;
		double ctMinusOne;

		double yt = 0;
		ArrayList<Double> standData = DataModification.standardize(UtilityConversion.convert1DArrayTo1DArrayList(data));

		for (int i = 0; i < data.length; i++) {
			ctMinusOne = ctl.get(i);

			double it = MathUtils.sigmoid(wi.get(i) * standData.get(i) + rI.get(i) * yt);
			double ot = MathUtils.sigmoid(wo.get(i) * standData.get(i) + rO.get(i) * yt);
			double zt = MathUtils.tanh(wz.get(i) * standData.get(i) + rZ.get(i) * yt);
			ct = ctMinusOne + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		double res = DataModification.reverseStandrize(
				DataStatistics.getMean(UtilityConversion.convert1DArrayTo1DArrayList(data)),
				DataStatistics.getStanderDeviation(UtilityConversion.convert1DArrayTo1DArrayList(data)), yt);
		res = DataModification.scaleBack(yt, maxOfTrainingData, minOfTrainingData);
		;
		return res;
	}

	/**
	 * Retrieve previous seasonality models up to a specified iteration number. This
	 * method retrieves seasonality models from previous iterations, up to the
	 * specified iteration number.
	 *
	 *
	 * @param hyperParameters is the object of class HyperParameters. should be
	 *                        retrieved.
	 * 
	 *                        Modle or seasonality model.
	 * 
	 * @return An ArrayList of ArrayLists of ArrayLists of Doubles containing the
	 *         seasonality models from previous iterations. Each innermost ArrayList
	 *         represents a seasonality model, and the outer ArrayLists group models
	 *         by iteration.
	 */

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getOldModelsseasonality(HyperParameters hyperParameters) {
		int itterNumb = hyperParameters.getCount();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allPrevious = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		for (int i = 0; i < itterNumb; i++) {
			ArrayList<ArrayList<ArrayList<Double>>> previosusBestModelReadModels = ReadModels.getModelForSeasonality(
					this.path + Integer.toString(itterNumb) + hyperParameters.getModleSuffix(), hyperParameters).get(0);
			allPrevious.add(previosusBestModelReadModels);

		}
		return allPrevious;

	}

	/**
	 * Retrieve previous seasonality models up to a specified iteration number. This
	 * method retrieves seasonality models from previous iterations, up to the
	 * specified iteration number.
	 *
	 * @param itterNumb   The iteration number up to which previous seasonality
	 *                    models * should be retrieved.
	 * @param modleSuffix decides if the modle is for trend or seasonality
	 * @return An ArrayList of ArrayLists of ArrayLists of Doubles containing the
	 *         seasonality models from previous iterations. Each innermost ArrayList
	 *         represents a seasonality model, and the outer ArrayLists group models
	 *         by iteration.
	 */

	public ArrayList<ArrayList<ArrayList<Double>>> getOldModelsTrend(int itterNumb, String modleSuffix) {
		ArrayList<ArrayList<ArrayList<Double>>> oldModelsTrend = new ArrayList<ArrayList<ArrayList<Double>>>();
		for (int i = 0; i < itterNumb; i++) {
			ArrayList<ArrayList<Double>> previosusBestModelReadModels = ReadModels
					.getModelForTrend(this.path + Integer.toString(itterNumb) + modleSuffix).get(0);
			oldModelsTrend.add(previosusBestModelReadModels);

		}
		return oldModelsTrend;

	}

	/**
	 * Combine seasonality models from previous iterations with a current set of
	 * models. This method combines a set of seasonality models from previous
	 * iterations with a current set of models. The combined models are stored in
	 * the 'model' parameter.
	 *
	 * @param allPrevious An ArrayList of ArrayLists of ArrayLists of Doubles
	 *                    containing seasonality models from previous iterations.
	 *                    Each innermost ArrayList represents a seasonality model,
	 *                    and the outer ArrayLists group models by iteration.
	 * @param model       An ArrayList of ArrayLists of ArrayLists of Doubles
	 *                    representing the current seasonality models.
	 * @return An ArrayList of ArrayLists of ArrayLists of Doubles containing the
	 *         combined seasonality models, including both previous and current
	 *         models.
	 */

	public static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> combineModelsSeasonality(
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allPrevious,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> model) {
		for (int i = 0; i < allPrevious.size(); i++) {
			model.add(allPrevious.get(i));
		}
		return model;
	}

	/**
	 * Copy existing models from the 'allOldModels' list and add them to the
	 * 'newModels' list. This method iterates through each element in the
	 * 'allOldModels' list and appends its content to the 'newModels' list. The
	 * 'newModels' list is modified in-place to include the models from
	 * 'allOldModels'.
	 *
	 * @param allOldModels An ArrayList of ArrayLists containing Double values,
	 *                     representing existing models.
	 * @param newModels    An ArrayList of ArrayLists containing Double values,
	 *                     where the existing models will be copied.
	 * @return The 'newModels' ArrayList after adding the models from
	 *         'allOldModels'.
	 */

	public ArrayList<ArrayList<ArrayList<Double>>> combineModelTrend(
			ArrayList<ArrayList<ArrayList<Double>>> allOldModels, ArrayList<ArrayList<ArrayList<Double>>> newModels) {
		for (int i = 0; i < allOldModels.size(); i++) {
			newModels.add(allOldModels.get(i));
		}
		return newModels;

	}
}
