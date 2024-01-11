package io.openems.edge.predictor.lstm.interpolation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class CubicalInterpolation {
	/**
	 * Interpolate data using the given input data and return the interpolated
	 * result. This method performs interpolation on the input data using a specific
	 * interpolation algorithm. It calculates interpolation values based on the
	 * provided data and returns the interpolated result.
	 *
	 * @param data The input data for interpolation.
	 * @return An ArrayList of Double representing the interpolated data.
	 */
	static ArrayList<Double> interpolate(ArrayList<Double> data) {
		ArrayList<ArrayList<Double>> interpolationData = getInterpolatingData(data);

		ArrayList<ArrayList<Double>> xInterval = groupToInterval(interpolationData.get(0));
		ArrayList<ArrayList<Double>> yInterval = groupToInterval(interpolationData.get(1));
		ArrayList<Double> mVector = generateAndSolveTriDiagonal(xInterval, yInterval);

		for (int i = 0; i < interpolationData.get(2).size(); i++) {

			int tempval = (int) Math.round(interpolationData.get(2).get(i));
			data.set(tempval, interpolationFunction(xInterval, yInterval, groupToInterval(mVector),
					interpolationData.get(2).get(i)));

		}
		return data;
	}

	/**
	 * Generate and solve a tridiagonal linear system of equations to obtain the
	 * mVector. This method takes two lists of data points representing intervals
	 * and performs calculations to generate a tridiagonal matrix and solve the
	 * linear system of equations to obtain the mVector.
	 *
	 * @param x A list of X interval data.
	 * @param y A list of Y interval data.
	 * @return An ArrayList of Double representing the mVector.
	 */
	private static ArrayList<Double> generateAndSolveTriDiagonal(ArrayList<ArrayList<Double>> x,
			ArrayList<ArrayList<Double>> y) {
		ArrayList<ArrayList<Double>> cof = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> tempMat = new ArrayList<ArrayList<Double>>();
		List<Double> vector = new ArrayList<Double>();
		int colLen = x.size() - 1;
		int rowLen = colLen + 2;

		List<List<Double>> matrixA = IntStream.range(0, colLen) //
				.mapToObj(i -> new ArrayList<Double>(Collections.nCopies(rowLen, 0d))).collect(Collectors.toList());

		for (int i = 0; i < x.size(); i++) {
			if (i < x.size() - 1) {
				cof = generateCof(x.get(i), x.get(i + 1), y.get(i), y.get(i + 1));
				vector.add(cof.get(1).get(0));
				tempMat.add(cof.get(0));
			}

		}

		for (int i = 0; i < tempMat.size(); i++) {

			for (int j = 0; j < tempMat.get(0).size(); j++) {

				matrixA.get(i).set(j + i, tempMat.get(i).get(j));

			}

		}
		for (int i = 0; i < matrixA.size(); i++) {
			matrixA.get(i).remove(0);
			matrixA.get(i).remove(matrixA.get(i).size() - 1);
		}

		return (ArrayList<Double>) linearEquationSolver(matrixA, vector);

	}

	/**
	 * Generate and calculate coefficients for interpolation within specified
	 * intervals. This method calculates coefficients for interpolation based on the
	 * provided interval data. The coefficients and a vector 'y' are computed and
	 * returned in an ArrayList of ArrayLists of Double.
	 *
	 * @param intervalX1 The first X interval.
	 * @param intervalX2 The second X interval.
	 * @param intervalY1 The first Y interval.
	 * @param intervalY2 The second Y interval.
	 * @return An ArrayList of ArrayLists of Double containing coefficients and the
	 *         'y' vector.
	 */

	private static ArrayList<ArrayList<Double>> generateCof(ArrayList<Double> intervalX1, ArrayList<Double> intervalX2,
			ArrayList<Double> intervalY1, ArrayList<Double> intervalY2) {
		ArrayList<Double> cof = new ArrayList<>();
		final ArrayList<Double> vectorY = new ArrayList<>();
		double temp1 = intervalX1.get(1) - intervalX1.get(0);
		double temp2 = intervalX2.get(1) - intervalX1.get(0);
		double temp3 = intervalX2.get(1) - intervalX2.get(0);
		double temp4 = intervalY2.get(1) - intervalY2.get(0);
		double temp5 = intervalY1.get(1) - intervalY1.get(0);
		double cof1 = temp1 / 6.0;
		double cof2 = temp2 / 3.0;
		double cof3 = temp3 / 6.0;
		final double y = (temp4 / temp3) - (temp5 / temp1);
		cof.add(cof1);
		cof.add(cof2);
		cof.add(cof3);
		vectorY.add(y);
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		result.add(cof);
		result.add(vectorY);
		return result;
	}

	private static ArrayList<Double> linearEquationSolver(List<List<Double>> matA, List<Double> vectB) {
		ArrayList<Double> x = new ArrayList<Double>();
		double[][] tempMat = new double[matA.size()][matA.get(0).size()];
		double[] tempVect = new double[vectB.size()];
		for (int i = 0; i < matA.size(); i++) {
			tempVect[i] = vectB.get(i);
			for (int j = 0; j < matA.get(0).size(); j++) {
				tempMat[i][j] = matA.get(i).get(j);

			}
		}

		RealMatrix matrixA = new Array2DRowRealMatrix(tempMat, false);
		DecompositionSolver solver = new LUDecomposition(matrixA).getSolver();
		RealVector vect = new ArrayRealVector(tempVect, false);
		RealVector solution = solver.solve(vect);

		for (int i = 0; i < vectB.size(); i++) {
			x.add(solution.getEntry(i));
		}
		x.add(0, 0.0);
		x.add(0.0);

		return x;

	}

	/**
	 * Groups the elements of a list into intervals of a specified size. This method
	 * takes a list of elements and groups them into intervals, each containing two
	 * elements: the element at the current index and the element at a fixed offset
	 * determined by the `groupSize`. The last interval may contain fewer elements
	 * if there are not enough remaining elements to form a complete interval.
	 *
	 * @param arr The list of elements to be grouped into intervals.
	 * @return An ArrayList of intervals, each containing two elements.
	 */
	private static ArrayList<ArrayList<Double>> groupToInterval(ArrayList<Double> arr) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < arr.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			final int groupSize = 1;
			if (i + groupSize < arr.size()) {
				temp.add(arr.get(i));
				temp.add(arr.get(i + groupSize));
				result.add(temp);
			}

		}
		return result;
	}

	/**
	 * Identifies the interval to which an interpolation value belongs. This method
	 * determines which interval in the provided list of intervals contains the
	 * specified interpolation value. Each interval is defined by its start and end
	 * values. The method checks if the interpolation value falls within any of
	 * these intervals.
	 *
	 * @param x                  A list of intervals, each represented as an
	 *                           ArrayList of two values (start and end).
	 * @param interpolationValue The value to be identified within the intervals.
	 * @return The index of the interval that contains the interpolation value, or
	 *         -1 if not found.
	 */

	private static int identifyInterval(ArrayList<ArrayList<Double>> x, double interpolationValue) {
		for (int i = 0; i < x.size(); i++) {
			if (x.get(i).get(0) <= interpolationValue && x.get(i).get(x.get(i).size() - 1) >= interpolationValue) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Computes the interpolated value for a given interpolation point using cubic
	 * interpolation. This method calculates the interpolated value for a specified
	 * interpolation point using cubic interpolation. It takes lists of x intervals,
	 * y intervals, and m values to perform the interpolation.
	 *
	 * @param xInter             A list of intervals for x values, each represented
	 *                           as an ArrayList of two values (start and end).
	 * @param yInt               A list of intervals for corresponding y values,
	 *                           each represented as an ArrayList of two values
	 *                           (start and end).
	 * @param mInter             A list of m values corresponding to the intervals
	 *                           for cubic interpolation.
	 * @param interpolationValue The value to be interpolated.
	 * @return The interpolated value at the specified interpolation point using
	 *         cubic interpolation.
	 */

	private static double interpolationFunction(ArrayList<ArrayList<Double>> xInter, ArrayList<ArrayList<Double>> yInt,
			ArrayList<ArrayList<Double>> mInter, double interpolationValue) {
		int index = identifyInterval(xInter, interpolationValue);
		double temp1 = 0.0;
		double temp2 = 0.0;
		double temp3 = 0.0;
		temp1 = (double) xInter.get(index).get(1) - interpolationValue;
		temp2 = (double) interpolationValue - xInter.get(index).get(0);
		temp3 = xInter.get(index).get(1) - xInter.get(index).get(0);
		double res0 = ((Math.pow(temp1, 3) * mInter.get(index).get(0) + Math.pow(temp2, 3) * mInter.get(index).get(1))
				/ (6 * temp3));
		double res1 = ((temp1) * yInt.get(index).get(0) + temp2 * yInt.get(index).get(1)) / (temp3);
		double res2 = (temp3 * (temp1 * mInter.get(index).get(0) + temp2 * mInter.get(index).get(1))) / 6;
		return res0 + res1 - res2;

	}

	/**
	 * Prepares data for interpolation by separating valid data points and
	 * identifying interpolating points. This method processes a list of data to
	 * prepare it for interpolation. It separates valid data points (non-NaN) into x
	 * and y values and identifies the positions of interpolating points (NaN or
	 * null values). The result is structured as a list of three ArrayLists, where
	 * the first ArrayList contains x values, the second contains corresponding y
	 * values, and the third contains positions of interpolating points.
	 *
	 * @param allData The list of data to be processed for interpolation.
	 * @return A list of three ArrayLists: x values, y values, and positions of
	 *         interpolating points.
	 */

	private static ArrayList<ArrayList<Double>> getInterpolatingData(ArrayList<Double> allData) {

		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> xVal = new ArrayList<Double>();
		ArrayList<Double> yVal = new ArrayList<Double>();
		ArrayList<Double> interpolatingPoints = new ArrayList<Double>();
		for (int i = 0; i < allData.size(); i++) {
			if (Double.isNaN(allData.get(i)) || allData.get(i) == null) {
				interpolatingPoints.add((double) i);

			} else {
				xVal.add((double) i);
				yVal.add((double) allData.get(i));
			}
			result.add(xVal);
			result.add(yVal);
			result.add(interpolatingPoints);

		}

		return result;

	}

	/**
	 * Check if the provided data contains at least four non-null and non-NaN values
	 * for interpolation. This method examines the input data and determines if
	 * there are at least four consecutive non-null and non-NaN values, indicating
	 * the possibility of interpolation.
	 *
	 * @param data The input data to be checked for interpolation.
	 * @return `true` if there are at least four consecutive non-null and non-NaN
	 *         values for interpolation, `false` otherwise.
	 */

	public static boolean canInterpolate(ArrayList<Double> data) {
		ArrayList<Double> check = new ArrayList<Double>();
		for (int i = 0; i < data.size(); i++) {
			if (Double.isNaN(data.get(i)) || data.get(i) == null) {
				// Do nothing
			} else {
				check.add(data.get(i));
			}
			if (check.size() >= 4) {
				return true;
			} else {
				// Do nothing
			}
		}
		return false;
	}
}