package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import static io.openems.edge.predictor.lstmmodel.utilities.SlidingWindowSpliterator.windowed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class GrouptoWindowpipe implements Stage<Object, Object> {

	public static final Function<List<List<Double>>, double[][]> twoDListToTwoDArray = UtilityConversion::to2DArray;

	private int window;

	public GrouptoWindowpipe(int windowSize) {
		this.window = windowSize;
	}

	@Override
	public Object execute(Object input) {
		if (input instanceof double[] inputData) {
			try {
				double[][] windowedData = this.getWindowDataTrain(inputData);
				double[] windowedTarget = this.getTargetData(inputData);
				return new double[][][] { windowedData, new double[][] { windowedTarget } };
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private double[][] getWindowDataTrain(double[] data) {
		int lower = 0;
		int upper = data.length - 1;

		var subList = IntStream.range(lower, upper)//
				.mapToObj(index -> data[index]) //
				.collect(Collectors.toCollection(ArrayList::new));

		List<List<Double>> res = windowed(subList, this.window) //
				.map(s -> s.collect(Collectors.toList())) //
				.collect(Collectors.toList());

		return twoDListToTwoDArray.apply(res);
	}

	/**
	 * Retrieves the target data from a list of scaled data points.
	 * 
	 * @param data The list containing scaled data points.
	 * @return An array containing the target data.
	 * @throws Exception If the provided list of scaled data is empty.
	 */
	public double[] getTargetData(double[] data) throws Exception {

		if (data.length == 0) {
			throw new Exception("Scaled data is empty");
		}
		int lower = 0;
		int upper = data.length - 1;

		double[] subArr = IntStream.range(lower + this.window, upper + 1) //
				.mapToDouble(index -> data[index]) //
				.toArray();

		return subArr;
	}
}
