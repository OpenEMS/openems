package io.openems.edge.predictor.lstm.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class UtilityConversion {

	/**
	 * Convert double[] to {@link java.util.ArrayList} of Double.
	 * 
	 * @param toBeConverted array of double
	 * @return result converted Array list
	 */
	public static ArrayList<Double> convertDoubleArrayToArrayListDouble(double[] toBeConverted) {

		return DoubleStream.of(toBeConverted) //
				.boxed() //
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Convert {@link java.util.List} of Integer to {@link java.util.List} of
	 * Double.
	 * 
	 * @param toBeConverted the {@link java.util.List} of Integer
	 * @return result {@link java.util.List} of Double.
	 */
	public static List<Double> convertListIntegerToListDouble(List<Integer> toBeConverted) {
		return toBeConverted.stream() //
				.mapToDouble(i -> i == null ? null : i) //
				.boxed() //
				.collect(Collectors.toList());
	}

	/**
	 * Convert {@link java.util.ArrayList} to double[][].
	 * 
	 * @param data {@link java.util.ArrayList} double
	 * @return result converted double [][]
	 */
	public static double[][] convert2DArrayListTo2DArray(ArrayList<ArrayList<Double>> data) {
		return data.stream() //
				.map(UtilityConversion::convert1DArrayListTo1DArray) //
				.toArray(double[][]::new);
	}

	/**
	 * Convert {@link java.util.List} of double.
	 * 
	 * @param data {@link java.util.List} of Double
	 * @return result converted double [][]
	 */
	public static double[][] convert2DArrayListTo2DArray(List<List<Double>> data) {
		return data.stream() //
				.map(UtilityConversion::convert1DArrayListTo1DArray) //
				.toArray(double[][]::new);
	}

	/**
	 * Convert {@link java.util.ArrayList} of Double to double[].
	 * 
	 * @param data {@link java.util.ArrayList} of Double
	 * @return result converted double []
	 */
	public static double[] convert1DArrayListTo1DArray(ArrayList<Double> data) {
		return data.stream() //
				.mapToDouble(Double::doubleValue) //
				.toArray();
	}

	/**
	 * Convert {@link java.util.List} of double to double[].
	 * 
	 * @param data {@link java.util.List} of double
	 * @return result converted double []
	 */
	public static double[] convert1DArrayListTo1DArray(List<Double> data) {
		return data.stream() //
				.mapToDouble(Double::doubleValue) //
				.toArray();
	}

	public static ArrayList<ArrayList<Double>> convert2DArrayTo2DArrayList(double[][] data) {
		ArrayList<ArrayList<Double>> toReturn = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < data.length; i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for (int j = 0; j < data[i].length; j++) {
				temp.add(data[i][j]);

			}
			toReturn.add(temp);
		}
		return toReturn;

	}

}
