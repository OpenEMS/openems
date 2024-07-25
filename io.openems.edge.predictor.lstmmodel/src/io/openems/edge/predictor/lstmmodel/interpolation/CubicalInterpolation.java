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
		var interpolation = new ArrayList<ArrayList<Double>>();
		var function = this.getFunctionForAllInterval(this.data);
		var differences = this.firstOrderDiff(function);

		for (int i = 0; i < differences.length; i++) {
			if (differences[i] != 1) {
				int requiredPoints = (int) (differences[i] - 1);
				interpolation.add(this.calculate(function.getPolynomials()[i].getCoefficients(), requiredPoints));
			}
		}
		this.generateCombineInstruction(interpolation, differences);
		return this.data;
	}

	private PolynomialSplineFunction getFunctionForAllInterval(ArrayList<Double> data) {
		var nonNaNCount = data.stream().filter(d -> !Double.isNaN(d)).count();

		var dataNew = new double[(int) nonNaNCount];
		var xVal = new double[(int) nonNaNCount];

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
		return IntStream.range(0, knots.length - 1)//
				.mapToDouble(i -> knots[i + 1] - knots[i])//
				.toArray();
	}

	private ArrayList<Double> calculate(double[] weight, int requiredPoints) {

		ArrayList<Double> result = new ArrayList<>();
		for (int j = 0; j < requiredPoints; j++) {
			double sum = 0;
			for (int i = 0; i < weight.length; i++) {
				sum += weight[i] * Math.pow(j + 1, i);
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
		var nonNaNCount = data.stream().filter(d -> !Double.isNaN(d)).count();
		
		return this.data.size() > 4 && nonNaNCount>2;
	}

	public void setData(ArrayList<Double> val) {
		this.data = val;
	}

	public ArrayList<Double> getInterPolatedData() {
		return this.data;
	}

}
