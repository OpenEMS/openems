package io.openems.edge.predictor.lstm.preprocessing;

import static io.openems.edge.predictor.lstm.utilities.SlidingWindowSpliterator.windowed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class PreProcessingImpl {

	public static final Function<double[], ArrayList<Double>> CONVERT_DOUBLE_ARRAY_TO_DOUBLE_ARRAYLIST = UtilityConversion::convertDoubleArrayToArrayListDouble;
	public static final Function<List<List<Double>>, double[][]> CONVERT_2DDOUBLE_LIST_TO_2DDOUBLE_ARRAY = UtilityConversion::convert2DArrayListTo2DArray;

	private int windowSize = 7;

	private ArrayList<Double> scaleDataList;

	// private TrainTestSplit trainTestSplit;

	private double[][] trainData;
	private double[][] validateData;
	private double[][] testData;

	private double[] trainTarget;
	private double[] validateTarget;
	private double[] testTarget;

	public PreProcessingImpl(List<Double> data, int windowSize) {
		this.windowSize = windowSize;
		this.scaleDataList = (ArrayList<Double>) data;

		// TODO make percentage dynamic
		// this.trainTestSplit = new TrainTestSplit(data.size(), windowSize, 0.8, 0.1);
	}

	/**
	 * Gets the feature data.
	 * 
	 * @param lower lowest index of the data list
	 * @param upper upper index of the data list
	 * @return featureData featureData for model training.
	 * @throws Exception when the scaleDatalist is empty
	 */
	public double[][] getFeatureData(int lower, int upper) throws Exception {

		if (this.scaleDataList.isEmpty()) {
			throw new Exception("Scaled data is empty");
		}

		double[] subArr = IntStream.range(lower, upper) //
				.mapToDouble(index -> this.scaleDataList.get(index)) //
				.toArray();

		List<List<Double>> res = windowed(CONVERT_DOUBLE_ARRAY_TO_DOUBLE_ARRAYLIST.apply(subArr), this.windowSize) //
				.map(s -> s.collect(Collectors.toList())) //
				.collect(Collectors.toList());

		return CONVERT_2DDOUBLE_LIST_TO_2DDOUBLE_ARRAY.apply(res);

	}

	/**
	 * Gets the target data.
	 * 
	 * @param lower lowest index of the data list
	 * @param upper upper index of the data list
	 * @return targetData targetDataList for model training.
	 * @throws Exception when the scaleDatalist is empty
	 */
	public double[] getTargetData(int lower, int upper) throws Exception {

		if (this.scaleDataList.isEmpty()) {
			throw new Exception("Scaled data is empty");
		}

		double[] subArr = IntStream.range(lower + this.windowSize, upper + 1) //
				.mapToDouble(index -> this.scaleDataList.get(index)) //
				.toArray();

		return subArr;
	}

	/**
	 * Groups the values in the input ArrayList into windows of a specified size and
	 * converts the grouped data into a 2D array representing the stiffed windowed
	 * structure.
	 *
	 * @param val        The input ArrayList of Double values to be grouped into
	 *                   windows.
	 * @param windowSize The size of each window for grouping the values.
	 * @return A 2D array representing the stiffed windowed structure of the grouped
	 *         values. //
	 */

	public static double[][] groupToStiffedWindow(ArrayList<Double> val, int windowSize) {
		ArrayList<ArrayList<Double>> windowedData = new ArrayList<ArrayList<Double>>();
		int index = 0;
		for (int i = 0; i < val.size(); i++) {

			ArrayList<Double> temp = new ArrayList<Double>();
			if (index + windowSize < val.size()) {

				for (int j = 0; j < windowSize; j++) {
					temp.add(val.get(index + j));

				}

				windowedData.add(temp);
				index = index + windowSize + 1;

			}
			
		}
		
		return UtilityConversion.convert2DArrayListTo2DArray(windowedData);

	}

	/**
	 * Groups the values in the input ArrayList into a stiffed target structure,
	 * extracting every nth element to form windows of a specified size and converts
	 * the grouped data into a 1D array.
	 *
	 * @param val        The input ArrayList of Double values from which the stiffed
	 *                   target structure is created.
	 * @param windowSize The size of each window, representing the step size for
	 *                   selecting elements.
	 * @return A 1D array representing the stiffed target structure of the grouped
	 *         values.
	 */

	public static double[] groupToStiffedTarget(ArrayList<Double> val, int windowSize) {
		int index = windowSize;
		ArrayList<Double> windowedData = new ArrayList<Double>();
		for (int i = 0; i < val.size(); i++) {

			// ArrayList<Double> temp = new ArrayList<Double>();
			if (index  < val.size()) {
				windowedData.add(val.get(index));

			}
			index = index + windowSize + 1;

		}

		return UtilityConversion.convert1DArrayListTo1DArray(windowedData);

	}

	public double[][] getTrainData() {
		return this.trainData;
	}

	public double[][] getValidateData() {
		return this.validateData;
	}

	public double[][] getTestData() {
		return this.testData;
	}

	public double[] getTrainTarget() {
		return this.trainTarget;
	}

	public double[] getValidateTarget() {
		return this.validateTarget;
	}

	public double[] getTestTarget() {
		return this.testTarget;
	}

}
