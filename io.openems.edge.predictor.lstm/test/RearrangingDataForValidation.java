import static org.junit.Assert.fail;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.DataStatistics;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;
import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstm.utilities.MathUtils;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class RearrangingDataForValidation {
	/**
	 * Rearranges and processes time-series data for trend analysis using a
	 * specified set of hyperparameters and a pre-trained model path.
	 *
	 * @param hyperParameters The hyperparameters configuration for the trend
	 *                        analysis.
	 * @param modlePath       The path to the pre-trained model for seasonality
	 *                        analysis.
	 * @throws Exception Throws an exception if there is an error during the
	 *                   process.
	 */

	public void main(HyperParameters hyperParameters, String modlePath) throws Exception {
		String csvFileName = "2.csv";
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> reShapeFirst = new ArrayList<ArrayList<Double>>();
		ReadCsv csv = new ReadCsv(csvFileName);
		ArrayList<Double> data = csv.getData();
		ArrayList<OffsetDateTime> date = csv.getDates();

		// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new
		// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		InterpolationManager inter = new InterpolationManager(data, date, hyperParameters);
		// ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> finalGroupedMatrix = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();
  //		hyperParameters.setModleSuffix("trend.txt");

		double minOfTrainingData = hyperParameters.getScalingMin();
		double maxOfTrainingData = hyperParameters.getScalingMax();
		// Grouping the data by hour and minute
		GroupBy groupAsHour = new GroupBy(inter.getInterpolatedData(), date);
		groupAsHour.hour();

		for (int i = 0; i < groupAsHour.getDataGroupedByHour().size(); i++) {

			GroupBy groupAsMinute = new GroupBy(groupAsHour.getDataGroupedByHour().get(i),
					groupAsHour.getDateGroupedByHour().get(i));
			groupAsMinute.minute();
			dataGroupedByMinute.add(groupAsMinute.getDataGroupedByMinute());
			dateGroupedByMinute.add(groupAsMinute.getDateGroupedByMinute());
		}

		// rehsaping the grouped data step1 : Reshaping the dimension of the grouped
		// matrix :
		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {

				reShapeFirst.add(dataGroupedByMinute.get(i).get(j));
			}

		}
		// reGrouping data from reshaped matrix

		int offset = 0;

		for (int i = 0; i < reShapeFirst.size(); i++) {
			ArrayList<ArrayList<Double>> toCombine = new ArrayList<ArrayList<Double>>();

			for (int j = 0; j <= hyperParameters.getWindowSizeTrend(); j++) {
				if (j + offset < reShapeFirst.size()) {
					toCombine.add(reShapeFirst.get(j + offset));

				} else {

					toCombine.add(reShapeFirst.get(j + offset - reShapeFirst.size()));

				}

			}

			finalGroupedMatrix.add(this.combinedArray(toCombine));
			offset++;

		}

		String path = modlePath + Integer.toString(hyperParameters.getCount()) + "trend.txt";
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = ReadModels.getModelForSeasonality(path,
				hyperParameters);

		for (int i = 0; i < allModels.size(); i++) {
			ArrayList<Double> rmsTemp1 = new ArrayList<Double>();

			for (int j = 0; j < finalGroupedMatrix.size(); j++) {

				double[][] validateData = PreProcessingImpl.groupToStiffedWindow(
						DataModification.scale(finalGroupedMatrix.get(j), hyperParameters.getScalingMin(),
								hyperParameters.getScalingMax()),
						hyperParameters.getWindowSizeTrend());
				double[] validationTarget = PreProcessingImpl.groupToStiffedTarget(finalGroupedMatrix.get(j),
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
		ReadModels.updateModel(allModels, optInd, Integer.toString(hyperParameters.getCount()) + "trend.txt");

	}

	/**
	 * Combines values from a list of ArrayLists into a single ArrayList, grouping
	 * values by their respective positions.
	 *
	 * <p>
	 * This method takes a list of ArrayLists as input, and for each position
	 * (index) within the smallest ArrayList in the list, it retrieves the value
	 * from that position in each ArrayList and combines them into a new ArrayList.
	 * The resulting ArrayList represents the values grouped by position across all
	 * input ArrayLists.
	 * </p>
	 *
	 * @param val The list of ArrayLists containing values to be combined.
	 * @return An ArrayList of Double representing the combined values.
	 */

	public ArrayList<Double> combinedArray(ArrayList<ArrayList<Double>> val) {
		ArrayList<Integer> sizeMatrix = new ArrayList<Integer>();
		ArrayList<Double> reGroupedsecond = new ArrayList<Double>();
		for (int i = 0; i < val.size(); i++) {
			sizeMatrix.add(val.get(i).size());

		}
		for (int i = 0; i < Collections.min(sizeMatrix); i++) {
			for (int j = 0; j < val.size(); j++) {

				reGroupedsecond.add(val.get(j).get(i));

			}

		}
		return reGroupedsecond;

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

	@Test
	public void test() throws Exception {
		HyperParameters hyperparameters = new HyperParameters();
		RearrangingDataForValidation obj = new RearrangingDataForValidation();
		String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
		obj.main(hyperparameters, path);
		fail("Not yet implemented");
	}

}
