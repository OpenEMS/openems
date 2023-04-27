package io.openems.edge.predictor.lstmmodel.interpolation;

import java.util.ArrayList;

public class Interpolation {

	public Interpolation(ArrayList<Double> data) {

		System.out.println("Data Before Interpolation");
		System.out.print(data);

		// TODO Check for NaN
		if (Double.isNaN(data.get(0))) {
			data.set(0, this.calculateMean(data));
		}
		if (Double.isNaN(data.get(data.size() - 1))) {
			data.set(data.size() - 1, this.calculateMean(data));
		} else {
			//
		}
		ArrayList<ArrayList<ArrayList<Double>>> coordinate = this.determineInterpolatingPoints(data);
		for (int i = 0; i < coordinate.size(); i++) {
			ArrayList<Double> val = this.computeInterpolation(coordinate.get(i).get(0).get(0), //
					coordinate.get(i).get(0).get(1), //
					coordinate.get(i).get(1).get(0), //
					coordinate.get(i).get(1).get(1) //
			);
			data = this.conCat(data, val, coordinate.get(i).get(0).get(0), coordinate.get(i).get(0).get(1));

		}
		System.out.println("Data after Interpolation");

		System.out.println(data);
	}

	private ArrayList<ArrayList<ArrayList<Double>>> determineInterpolatingPoints(ArrayList<Double> data) {
		{

			double x1 = -1;
			double x2 = -1;
			double y1 = -1.0000;
			double y2 = -1.000000;
			int flag = 0;
			boolean flag1 = false;

			ArrayList<ArrayList<ArrayList<Double>>> coOrdinates = new ArrayList<ArrayList<ArrayList<Double>>>();

			for (int i = 0; i < data.size(); i++) {
				ArrayList<Double> x = new ArrayList<Double>();
				ArrayList<Double> y = new ArrayList<Double>();
				ArrayList<ArrayList<Double>> temp = new ArrayList<ArrayList<Double>>();

				if (Double.isNaN(data.get(i))) {
					flag1 = true;
				} else {
					flag1 = false;
				}

				if (flag1 == true && flag == 0) {
					x1 = i - 1;
					y1 = data.get(i - 1);
					flag = 1;

				} else if (flag1 == false && flag == 1) {
					x2 = i;
					y2 = data.get(i);
					flag = 0;
					x.add(x1);
					x.add(x2);
					y.add(y1);
					y.add(y2);

					temp.add(x);
					temp.add(y);
					coOrdinates.add(temp);
					Double.isNaN(i);

				} else {
					//
				}
			}
			return coOrdinates;
		}
	}

	private ArrayList<Double> computeInterpolation(double x1, double x2, double y1, double y2) {
		ArrayList<Double> val = new ArrayList<Double>();
		for (int i = 0; i < (x2 - x1); i++) {
			val.add(y1 * ((x2 - (i + x1)) / (x2 - x1)) + y2 * ((i) / (x2 - x1)));
		}
		return val;
	}

	private ArrayList<Double> conCat(ArrayList<Double> data, ArrayList<Double> val, double x1, double x2) {
		int tempX1 = (int) x1;
		int tempX2 = (int) x2;
		for (int i = 1; i < (tempX2 - tempX1); i++) {
			data.set((i + tempX1), val.get(i));
		}
		return data;
	}

	private double calculateMean(ArrayList<Double> data) {
		double sum = 0;
		for (int i = 0; i < data.size(); i++) {
			if (Double.isNaN(data.get(i))) {
				//
			} else {
				sum = sum + data.get(i);
			}

		}
		return sum / data.size();
	}

}
