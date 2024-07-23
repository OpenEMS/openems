package io.openems.edge.predictor.lstmmodel.utilities;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class UtilityConversion {

	/**
	 * Convert {@link java.util.ArrayList} to double[][].
	 * 
	 * @param data {@link java.util.ArrayList} double
	 * @return result converted double [][]
	 */
	public static double[][] to2DArray(ArrayList<ArrayList<Double>> data) {
		return data.stream() //
				.map(UtilityConversion::to1DArray) //
				.toArray(double[][]::new);
	}

	/**
	 * Convert {@link java.util.List} of double.
	 * 
	 * @param data {@link java.util.List} of Double
	 * @return result converted double [][]
	 */
	public static double[][] to2DArray(List<List<Double>> data) {
		return data.stream() //
				.map(UtilityConversion::to1DArray) //
				.toArray(double[][]::new);
	}

	/**
	 * Convert {@link java.util.List} of double to double[].
	 * 
	 * @param data {@link java.util.List} of double
	 * @return result converted double []
	 */
	public static double[] to1DArray(List<Double> data) {
		return data.stream() //
				.mapToDouble(d -> {
					if (d == null || d.isNaN() || Double.isNaN(d)) {
						return Double.NaN;
					}
					return d.doubleValue();
				}).toArray();
	}

	/**
	 * Convert {@link java.util.List} of {@link OffsetDateTime} to
	 * {@link OffsetDateTime}[].
	 * 
	 * @param data {@link java.util.List} of {@link OffsetDateTime}
	 * @return result converted {@link OffsetDateTime} []
	 */
	public static OffsetDateTime[] to1DArray(ArrayList<OffsetDateTime> data) {
		return data.stream().toArray(OffsetDateTime[]::new);
	}

	/**
	 * Converts an ArrayList of Double values to an array of Integer values.
	 *
	 * @param data The ArrayList of Double values to be converted.
	 * @return An array of Integer values where each element represents the
	 *         converted value from the input ArrayList.
	 */
	public static Integer[] toInteger1DArray(ArrayList<Double> data) {
		return data.stream() //
				.mapToInt(d -> d.intValue())//
				.boxed()//
				.toArray(Integer[]::new);
	}

	/**
	 * Converts a three-dimensional ArrayList of Double values into a
	 * three-dimensional array.
	 *
	 * @param data The three-dimensional ArrayList to be converted.
	 * @return A three-dimensional array containing the elements of the input
	 *         ArrayList.
	 */
	public static double[][][] to3DArray(ArrayList<ArrayList<ArrayList<Double>>> data) {
		double[][][] returnArray = new double[data.size()][][];
		for (int i = 0; i < data.size(); i++) {
			returnArray[i] = to2DArray(data.get(i));
		}
		return returnArray;
	}

	/**
	 * Converts a three-dimensional array of Double values into a two-dimensional
	 * array. This method converts the input three-dimensional array into a
	 * three-dimensional ArrayList, then into a two-dimensional ArrayList, and
	 * finally into a two-dimensional array.
	 *
	 * @param data The three-dimensional array to be converted.s
	 * @return A two-dimensional array containing the elements of the input array.
	 */
	public static double[][] to2DList(double[][][] data) {
		return to2DArray(to2DArrayList(to3DArrayList(data)));
	}

	/**
	 * Converts a two-dimensional array of double values to a two-dimensional
	 * ArrayList of Double values.
	 *
	 * @param data The two-dimensional array of double values to be converted.
	 * @return A two-dimensional ArrayList of Double values representing the
	 *         converted data.
	 */
	public static ArrayList<ArrayList<Double>> to2DArrayList(double[][] data) {
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

	/**
	 * Converts a three-dimensional ArrayList of Double values into a
	 * two-dimensional ArrayList.
	 *
	 * @param data The three-dimensional ArrayList to be converted.
	 * @return A two-dimensional ArrayList containing the elements of the input
	 *         ArrayList.
	 */
	public static ArrayList<ArrayList<Double>> to2DArrayList(ArrayList<ArrayList<ArrayList<Double>>> data) {
		var resized = new ArrayList<ArrayList<Double>>();

		for (int i = 0; i < data.size(); i++) {
			for (int j = 0; j < data.get(i).size(); j++) {
				resized.add(data.get(i).get(j));
			}
		}
		return resized;
	}

	/**
	 * Convert double[] to {@link java.util.ArrayList} of Double.
	 * 
	 * @param toBeConverted array of double
	 * @return result converted Array list
	 */
	public static ArrayList<Double> to1DArrayList(double[] toBeConverted) {

		return DoubleStream.of(toBeConverted) //
				.boxed() //
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Convert double[] to {@link java.util.ArrayList} of OffsetDateTime.
	 * 
	 * @param toBeConverted array of OffsetDateTime
	 * @return result converted Array list
	 */
	public static ArrayList<OffsetDateTime> to1DArrayList(OffsetDateTime[] toBeConverted) {
		return Arrays.stream(toBeConverted).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * convert 2DArrayList To 1DArray.
	 * 
	 * @param data the data
	 * @return converted the converted
	 */
	public static ArrayList<Double> to1DArrayList(ArrayList<ArrayList<Double>> data) {
		return (ArrayList<Double>) data.stream()//
				.flatMap(Collection::stream)//
				.collect(Collectors.toList());
	}

	/**
	 * Convert {@link java.util.List} of Integer to {@link java.util.List} of
	 * Double.
	 * 
	 * @param toBeConverted the {@link java.util.List} of Integer
	 * @return result {@link java.util.List} of Double.
	 */
	public static List<Double> toBoxed1DList(List<Integer> toBeConverted) {
		return toBeConverted.stream() //
				.mapToDouble(i -> i == null ? null : i) //
				.boxed() //
				.collect(Collectors.toList());
	}

	/**
	 * Converts a three-dimensional array into a three-dimensional ArrayList of
	 * Double values.
	 *
	 * @param data The three-dimensional array to be converted.
	 * @return A three-dimensional ArrayList containing the elements of the input
	 *         array.
	 */
	public static ArrayList<ArrayList<ArrayList<Double>>> to3DArrayList(double[][][] data) {
		var returnArray = new ArrayList<ArrayList<ArrayList<Double>>>();
		for (int i = 0; i < data.length; i++) {
			returnArray.add(to2DArrayList(data[i]));
		}
		return returnArray;
	}

	/**
	 * Get the index of the Min element in an array.
	 * 
	 * @param arr double array.
	 * @return iMin index of the min element in an array.
	 */
	public static int getMinIndex(double[] arr) {
		if (arr == null || arr.length == 0) {
			throw new IllegalArgumentException("Array must not be empty or null");
		}

		return IntStream.range(0, arr.length)//
				.boxed()//
				.min((i, j) -> Double.compare(arr[i], arr[j]))//
				.orElseThrow();
	}
}
