package io.openems.edge.predictor.lstmmodel.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class UtilityConversion {

	/**
	 * double[] to ArrayList<Double>
	 */
	public static ArrayList<Double> doubleToArrayListDouble(double[] toBeConverted) {

		return DoubleStream.of(toBeConverted) //
				.boxed() //
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * List<Integer> to List<Double>
	 */
	public static List<Double> listIntegerToListDouble(List<Integer> toBeConverterd) {

		return toBeConverterd.stream() //
				.mapToDouble(i -> i) //
				.boxed() //
				.collect(Collectors.toList());

	}
	
	public static double[][] convert2DArrayListTo2DArray(ArrayList<ArrayList<Double>> data) {
		return data.stream() //
				.map(UtilityConversion::convert1DArrayListTo1DArray) //
				.toArray(double[][]::new);
	}
	
	public static double[] convert1DArrayListTo1DArray(ArrayList<Double> data) {
		return data.stream().mapToDouble(Double::doubleValue).toArray();
	}
	
}
