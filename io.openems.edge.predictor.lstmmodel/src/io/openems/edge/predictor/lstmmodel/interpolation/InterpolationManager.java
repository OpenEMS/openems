package io.openems.edge.predictor.lstmmodel.interpolation;

import java.util.ArrayList;

public class InterpolationManager {

	ArrayList<Double> interpolated = new ArrayList<Double>();

	public InterpolationManager(ArrayList<Double> data) {
		System.out.println("Data" + data.size());

		ArrayList<Double> dataDouble = replaceNullWitNan(data);
		ArrayList<ArrayList<Double>> interpolatedGroupedData = new ArrayList<ArrayList<Double>>();

		double mean = calculateMean(dataDouble);

		ArrayList<ArrayList<Double>> groupedData = group(dataDouble);

		for (int i = 0; i < groupedData.size(); i++) {
			ArrayList<Double> interpolatedTemp = new ArrayList<Double>();
			ArrayList<Double> data1 = new ArrayList<Double>();
			data1 = groupedData.get(i);

			boolean interpolationNeeded = interpolationDecision(groupedData.get(i));
			if (interpolationNeeded == true) {

				if (Double.isNaN(data1.get(0))) {
					data1.set(0, mean);
				}
				if ((Double.isNaN(data1.get(data1.size() - 1)))) {
					data1.set(data1.size() - 1, mean);
				}

				CubicalInterpolation cubic = new CubicalInterpolation();

				if (cubic.canInterpolate(data1) == false) {

					LinearInterpolation linear = new LinearInterpolation(data1);
					interpolatedTemp = linear.Data;

				} else {
					interpolatedTemp = cubic.Interpolate(data1);
				}
				interpolatedGroupedData.add(interpolatedTemp);

			} else {

				interpolatedGroupedData.add(data1);

			}

		}
		interpolated = unGroup(interpolatedGroupedData);

	}

	public ArrayList<ArrayList<Double>> group(ArrayList<Double> data) {
		int groupSize = 2880;
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

	public ArrayList<Double> unGroup(ArrayList<ArrayList<Double>> data) {
		ArrayList<Double> toReturn = new ArrayList<Double>();

		for (int i = 0; i < data.size(); i++) {
			for (int j = 0; j < data.get(i).size(); j++) {
				toReturn.add(data.get(i).get(j));
			}

		}

		return toReturn;
	}

	static double calculateMean(ArrayList<Double> data) {
		double sum = 0;
		for (int i = 0; i < data.size(); i++) {
			if (Double.isNaN(data.get(i))) {

			} else {
				sum = sum + data.get(i);
			}

		}
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
}
