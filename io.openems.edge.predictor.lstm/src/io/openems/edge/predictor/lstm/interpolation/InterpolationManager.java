package io.openems.edge.predictor.lstm.interpolation;

import java.util.ArrayList;

public class InterpolationManager {

	
	public ArrayList<Double> interpolated = new ArrayList<Double>();

	public InterpolationManager(ArrayList<Double> data) {
		//System.out.println("Data" + data.size());

		ArrayList<Double> dataDouble =replaceNullWitNan(data);
		ArrayList<ArrayList<Double>> interpolatedGroupedData = new ArrayList<ArrayList<Double>>();
		
		double mean = calculateMean(dataDouble);

		ArrayList<ArrayList<Double>> groupedData = group(dataDouble);
//		System.out.println("Grouped  data : " + groupedData.size());
//		System.out.println("Grouped  data : " + groupedData.get(1).size());

//		System.out.println("Ungrouped Data: " + unGroup(groupedData).size());
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

				if (CubicalInterpolation.canInterpolate(data1) == false) {
//					System.out.println("Linear");
//					System.out.println("Passed" + data1);
					LinearInterpolation linear = new LinearInterpolation(data1);
					interpolatedTemp = linear.Data;
//					System.out.println("returned" + interpolatedTemp);
//					System.out.println("");
//				System.out.println(
//							"------------------------------------------------------------------------------------------");

				} else {
//					System.out.println("Cubical");
//					System.out.println("Passed" + data1);
					interpolatedTemp = CubicalInterpolation.Interpolate(data1);
//					System.out.println("returned" + interpolatedTemp);
//					System.out.println("");
//					System.out.println(
//							"------------------------------------------------------------------------------------------");

				}
				interpolatedGroupedData.add(interpolatedTemp);
				// System.out.println("ArrayListDouble datafterInterpolation"+cubic.data);
				// System.out.println(cubic.result);
				// linearInterpolation linear = new linearInterpolation(groupedData.get(i));

			} else {
//				System.out.println("No interpolation needed" + data1.size());
//				System.out.println("");
//				System.out.println(
//						"------------------------------------------------------------------------------------------");

				interpolatedGroupedData.add(data1);

			}

		}
		interpolated = unGroup(interpolatedGroupedData);
//		System.out.println("data" + data.size());
//
//		System.out.println("inter" + interpolated.size());
//		System.out.println("");
//		System.out
//				.println("------------------------------------------------------------------------------------------");

	}

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


