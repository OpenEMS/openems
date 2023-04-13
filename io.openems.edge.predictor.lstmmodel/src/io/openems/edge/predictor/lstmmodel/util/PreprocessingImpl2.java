package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

//import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;
//import static io.openems.edge.predictor.lstmmodel.utilities.SlidingWindowSpliterator.windowed;

public class PreprocessingImpl2 implements PreProcessing {

	private double max = 0;
	private double min = 0;
	private int windowSize = 24;

	private ArrayList<Double> dataList;
	private ArrayList<Double> scaleDataList;

	public TrainTestSplit trainTestSplit;

	public double[][] trainData;
	public double[][] validateData;
	public double[][] testData;

	public double[] trainTarget;
	public double[] validateTarget;
	public double[] testTarget;

	public PreprocessingImpl2(List<Double> data, int windowSize) {
		this.dataList = (ArrayList<Double>) data;
		this.windowSize = windowSize;

		this.max = Collections.max(this.dataList);
		this.min = Collections.min(this.dataList);
		//TODO make percentage dynamic
		this.trainTestSplit = new TrainTestSplit(data.size(), windowSize, 0.7 );
	}

	/**
	 * Gets the feature data.
	 * 
	 * @param lower  lowest index of the data list
	 * @param upper  upper index of the data list
	 * @param window size of the window
	 * @return featureData featureData for model training.
	 * @throws Exception
	 */
	public double[][] getFeatureData(int lower, int upper) throws Exception {

		if (this.scaleDataList.isEmpty()) {
			throw new Exception("Scaled data is empty");
		}

		double[][] featureData = new double[upper - this.windowSize][this.windowSize];

		for (int i = lower; i < upper - this.windowSize; i++) {
			var temp = new double[this.windowSize];
			for (int j = 0; j < this.windowSize; j++) {
				var b = this.scaleDataList.get(i + j);
				temp[j] = b;
			}
			featureData[i] = temp;
		}
		return featureData;

	}

	/**
	 * Gets the target data.
	 * 
	 * @param lower  lowest index of the data list
	 * @param upper  upper index of the data list
	 * @param window size of the window
	 * @return targetDataList targetDataList for model training.
	 * @throws Exception
	 */
	public double[] getTargetData(int lower, int upper) throws Exception {

		if (this.scaleDataList.isEmpty()) {
			throw new Exception("Scaled data is empty");
		}

		double[] targetData = new double[upper - this.windowSize];

		for (int i = lower; i < (upper - this.windowSize); i++) {
			var b = (double) this.scaleDataList.get(i + this.windowSize);
			targetData[i] = b;
		}
		return targetData;
	}

	/**
	 * Scale the Data with min and max values of the list.
	 * 
	 * @param minScaled
	 * @param maxScaled
	 * @return scaled list
	 */
	public void scale(double minScaled, double maxScaled) {

		double scaleFactor = maxScaled - minScaled;

		scaleDataList = (ArrayList<Double>) this.dataList.stream() //
				.map(item -> (((item - this.min) / this.max) * (scaleFactor)) + minScaled) //
				.collect(Collectors.toList());
	}

	public Function<double[], ArrayList<Double>> CONVERT = UtilityConversion::doubleToArrayListDouble;

	public Integer[] reverseScale(double minScaled, double maxScaled, double[] result) {

		double scaleFactor = maxScaled - minScaled;
		ArrayList<Double> first = CONVERT.apply(result);

		List<Integer> second = first.stream() //
				.map(item -> (((item * this.max) / (scaleFactor)) + this.min)) //
				.map(p -> p.intValue()) //
				.collect(Collectors.toList());

		return second.stream() //
				.toArray(Integer[]::new);

	}

}