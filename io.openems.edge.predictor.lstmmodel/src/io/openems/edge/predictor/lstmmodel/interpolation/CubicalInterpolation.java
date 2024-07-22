package io.openems.edge.predictor.lstmmodel.interpolation;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class CubicalInterpolation extends SplineInterpolator {

	private ArrayList<Double> data;

	public CubicalInterpolation(ArrayList<Double> data) {
		this.data = data;
	}

	public CubicalInterpolation() {
	}

	/**
	 * Compute Cubical interpolation.
	 * 
	 * @return interpolated results
	 */
	public ArrayList<Double> compute() {
		var interPolation = new ArrayList<ArrayList<Double>>();
		var function = this.getFunctionForAllInterval(this.data);
		var diff = this.firstOrderDiff(function);

		for (int i = 0; i < diff.length; i++) {
			if (diff[i] != 1) {
				int requiredPoints = (int) (diff[i] - 1);
				interPolation.add(this.calculate(function.getPolynomials()[i].getCoefficients(), requiredPoints));
			}
		}
		this.generateCombineInstruction(interPolation, diff);
		return this.data;
	}

	private PolynomialSplineFunction getFunctionForAllInterval(ArrayList<Double> data) {
		long nonNaNCount = data.stream().filter(d -> !Double.isNaN(d)).count();

		double[] dataNew = new double[(int) nonNaNCount];
		double[] xVal = new double[(int) nonNaNCount];

		int[] index = { 0 };
		IntStream.range(0, data.size())//
				.filter(i -> !Double.isNaN(data.get(i)))//
				.forEach(i -> {
					dataNew[index[0]] = data.get(i);
					xVal[index[0]] = i + 1;
					index[0]++;
				});

		return interpolate(xVal, dataNew);
	}

	private double[] firstOrderDiff(PolynomialSplineFunction function) {
		double[] knots = function.getKnots();
		return IntStream.range(0, knots.length - 1).mapToDouble(i -> knots[i + 1] - knots[i]).toArray();
	}

	private ArrayList<Double> calculate(double[] weight, int requiredPoints) {

		ArrayList<Double> result = new ArrayList<Double>();
		for (int j = 0; j < requiredPoints; j++) {
			double sum = 0;
			for (int i = 0; i < weight.length; i++) {
				sum = sum + weight[i] * Math.pow(j + 1, i);
			}
			result.add(sum);
		}
		return result;
	}

	private void generateCombineInstruction(ArrayList<ArrayList<Double>> interPolatedValue, double[] firstOrderDiff) {

		int count = 0;
		int startingPoint = 0;
		int addedData = 0;

		for (int i = 0; i < firstOrderDiff.length; i++) {

			if (firstOrderDiff[i] != 1) {

				startingPoint = i + 1 + addedData;

				this.combineToData(startingPoint, (int) firstOrderDiff[i] - 1, interPolatedValue.get(count));
				addedData = (int) (firstOrderDiff[i] - 1 + addedData);
				count = count + 1;

			}

		}

	}

	private void combineToData(int startingPoint, int totalpointsRequired, ArrayList<Double> dataToAdd) {
		for (int i = 0; i < totalpointsRequired; i++) {

			this.data.set(i + startingPoint, dataToAdd.get(i));

		}

	}

	/**
	 * Can interpolate ?.
	 * 
	 * @return boolean yes or no.
	 */
	public boolean canInterpolate() {
		if (this.data.size() <= 4) {
			return false;
		} else {
			return true;
		}
	}

	public void setData(ArrayList<Double> val) {
		this.data = val;
	}

	public ArrayList<Double> getInterPolatedData() {
		return this.data;
	}

}
