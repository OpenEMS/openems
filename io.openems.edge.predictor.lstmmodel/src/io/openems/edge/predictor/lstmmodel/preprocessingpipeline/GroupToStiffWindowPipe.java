package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArray;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to2DArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GroupToStiffWindowPipe implements Stage<Object, Object> {
	private int window;

	public GroupToStiffWindowPipe(int windowSize) {
		this.window = windowSize;

	}

	@Override
	public Object execute(Object input) {
		if (input instanceof double[] inputData) {

			var resultArray = new double[2][][];

			var inputDataList = to1DArrayList(inputData);

			var stiffedTargetGroup = new double[1][];
			stiffedTargetGroup[0] = groupToStiffedTarget(inputDataList, this.window);

			var stiffedWindowGroup = groupToStiffedWindow(inputDataList, this.window);

			resultArray[0] = stiffedWindowGroup;
			resultArray[1] = stiffedTargetGroup;

			return resultArray;
		}
		return null;
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
	public static double[][] groupToStiffedWindow(ArrayList<Double> values, int windowSize) {
		if (windowSize < 1 || windowSize > values.size()) {
			throw new IllegalArgumentException("Invalid window size");
		}

		List<Integer> indices = IntStream.range(0, values.size() - windowSize + 1) //
				.filter(i -> i % (windowSize + 1) == 0) //
				.boxed() //
				.collect(Collectors.toList()); //

		List<List<Double>> windowedData = indices.stream() //
				.map(i -> values.subList(i, i + windowSize)) //
				.map(ArrayList::new) //
				.collect(Collectors.toList()); //

		return to2DArray(windowedData);
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
		if (windowSize < 1 || windowSize > val.size()) {
			throw new IllegalArgumentException("Invalid window size");
		}

		List<Double> windowedData = IntStream.range(0, val.size())//
				.filter(j -> j % (windowSize + 1) == windowSize)//
				.mapToObj(val::get)//
				.collect(Collectors.toList());

		return to1DArray(windowedData);
	}

	public static double[] groupToStiffedTarget(double[] val, int windowSize) {
		if (windowSize < 1 || windowSize > val.length) {
			throw new IllegalArgumentException("Invalid window size");
		}

		return IntStream.range(0, val.length)//
				.filter(j -> j % (windowSize + 1) == windowSize)//
				.mapToDouble(j -> val[j]).toArray();
	}

}
