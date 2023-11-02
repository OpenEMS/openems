package io.openems.edge.predictor.lstm.interpolation;

import java.util.ArrayList;

public class InterpolationManager {

	private ArrayList<Double> interpolated = new ArrayList<Double>();

	public InterpolationManager(ArrayList<Double> data) {
		// System.out.println("Data" + data.size());

		ArrayList<Double> dataDouble = replaceNullWitNan(data);
		ArrayList<ArrayList<Double>> interpolatedGroupedData = new ArrayList<ArrayList<Double>>();

		double mean = calculateMean(dataDouble);

		ArrayList<ArrayList<Double>> groupedData = this.group(dataDouble);

		for (int i = 0; i < groupedData.size(); i++) {
			ArrayList<Double> interpolatedTemp = new ArrayList<Double>();
			ArrayList<Double> data1 = new ArrayList<Double>();
			data1 = groupedData.get(i);

			boolean interpolationNeeded = this.interpolationDecision(groupedData.get(i));
			if (interpolationNeeded == true) {

				if (Double.isNaN(data1.get(0))) {
					data1.set(0, mean);
				}
				if ((Double.isNaN(data1.get(data1.size() - 1)))) {
					data1.set(data1.size() - 1, mean);
				}

				if (CubicalInterpolation.canInterpolate(data1) == false) {

					LinearInterpolation linear = new LinearInterpolation(data1);
					interpolatedTemp = linear.getData();

				} else {

					interpolatedTemp = CubicalInterpolation.interpolate(data1);

				}
				interpolatedGroupedData.add(interpolatedTemp);

			} else {

				interpolatedGroupedData.add(data1);

			}

		}
		this.interpolated = this.unGroup(interpolatedGroupedData);

	}

	/**
	 * Groups a list of data into sublists of a specified size. This method takes a
	 * list of data and groups it into sublists of a specified size. Each sublist
	 * will contain up to `groupSize` elements, except for the last sublist, which
	 * may contain fewer elements if the total number of elements is not a multiple
	 * of `groupSize`.
	 * 
	 * @param data The list of data to be grouped.
	 * @return A list of sublists, each containing up to `groupSize` elements.
	 */
	public ArrayList<ArrayList<Double>> group(ArrayList<Double> data) {
		int groupSize = 96;
		ArrayList<ArrayList<Double>> groupedData = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> temp = new ArrayList<Double>();
		for (int i = 0; i < data.size(); i++) {

			if (i != 0 && i % groupSize == 0) {

				// System.out.println(i);
				temp.add(data.get(i));
				groupedData.add(temp);
				temp = new ArrayList<Double>();
			} else {
				temp.add(data.get(i));
			}
			if (i == data.size() - 1) {
				groupedData.add(temp);

			}

		}

		return groupedData;
	}

	/**
	 * Ungroups a list of sublists into a single list. This method takes a list of
	 * sublists and combines them into a single list, preserving the order of
	 * elements within the sublists.
	 *
	 * @param data The list of sublists to be ungrouped.
	 * @return A single list containing all elements from the sublists.
	 */

	public ArrayList<Double> unGroup(ArrayList<ArrayList<Double>> data) {
		ArrayList<Double> toReturn = new ArrayList<Double>();

		for (int i = 0; i < data.size(); i++) {
			for (int j = 0; j < data.get(i).size(); j++) {
				toReturn.add(data.get(i).get(j));
			}

		}

		return toReturn;
	}

	/**
	 * Calculates the mean (average) of a list of numeric values, excluding NaN
	 * values. This method computes the mean of a list of numeric values, excluding
	 * any NaN (Not-a-Number) values present in the list. If the input list is empty
	 * or contains only NaN values, the result will be NaN.
	 *
	 * @param data The list of numeric values from which to calculate the mean.
	 * @return The mean of the non-NaN numeric values in the input list.
	 */

	public static double calculateMean(ArrayList<Double> data) {
		double sum = data.stream().filter(value -> !Double.isNaN(value)).mapToDouble(Double::doubleValue).sum();

		return sum / data.size();
	}

	static ArrayList<Double> replaceNullWitNan(ArrayList<Double> data) {

		for (int i = 0; i < data.size(); i++) {
			if (data.get(i) == null) {
				data.set(i, (double) Float.NaN);

			}
		}
		return data;
	}

	private boolean interpolationDecision(ArrayList<Double> data) {
		for (int i = 0; i < data.size(); i++) {
			if (Double.isNaN(data.get(i))) {
				return true;
			} else {
				// Do nothing
			}

		}
		return false;
	}

	public ArrayList<Double> getInterpolatedData() {
		return this.interpolated;

	}
}
