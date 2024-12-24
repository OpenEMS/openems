package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.utilities.SlidingWindowSpliterator.windowed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

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
				var windowedTarget = this.getTargetData(inputData);
				var windowedData = this.getWindowDataTrain(inputData);

				return new double[][][] { windowedData, new double[][] { windowedTarget } };

			} catch (Exception e) {
				throw new RuntimeException("Error processing input data", e);
			}
		} else {
			throw new IllegalArgumentException("Input must be an instance of double[]");
		}
	}

	private double[][] getWindowDataTrain(double[] data) {
		var lower = 0;
		var upper = data.length - 1;

		var subList = IntStream.range(lower, upper)//
				.mapToObj(index -> data[index]) //
				.collect(Collectors.toCollection(ArrayList::new));

		var res = windowed(subList, this.window) //
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
		var lower = 0;
		var upper = data.length - 1;

		var subArr = IntStream.range(lower + this.window, upper + 1) //
				.mapToDouble(index -> data[index]) //
				.toArray();

		return subArr;
	}
}
