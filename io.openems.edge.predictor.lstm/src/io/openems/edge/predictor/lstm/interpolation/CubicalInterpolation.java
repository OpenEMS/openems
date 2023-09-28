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
	static ArrayList<ArrayList<Double>> interpolationData = new ArrayList<ArrayList<Double>>();
	ArrayList<Double> data = new ArrayList<Double>();
	ArrayList<ArrayList<Double>> temp01 = new ArrayList<ArrayList<Double>>();
	ArrayList<ArrayList<Double>> AInv = new ArrayList<ArrayList<Double>>();
	ArrayList<Double> result = new ArrayList<Double>();
	static int groupSize = 1;// Do not change this

	static public ArrayList<Double> Interpolate(ArrayList<Double> data) {
		interpolationData = getInterpolatingData(data);

//		

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

	private static ArrayList<Double> generateAndSolveTriDiagonal(ArrayList<ArrayList<Double>> x,
			ArrayList<ArrayList<Double>> y) {
		ArrayList<ArrayList<Double>> cof = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> tempMat = new ArrayList<ArrayList<Double>>();
		List<Double> vector = new ArrayList<Double>();
		List<Double> mVector = new ArrayList<Double>();
		int colLen = x.size() - 1;
		int rowLen = colLen + 2;

		List<List<Double>> A = IntStream.range(0, colLen) //
				.mapToObj(i -> new ArrayList<Double>(Collections.nCopies(rowLen, 0d))).collect(Collectors.toList());

		for (int i = 0; i < x.size(); i++) {
			if (i < x.size() - 1) {
				cof = generateCof(x.get(i), x.get(i + 1), y.get(i), y.get(i + 1));
				vector.add(cof.get(1).get(0));
				tempMat.add(cof.get(0));
			}

			else {
				// Do nothing
			}

		}

		for (int i = 0; i < tempMat.size(); i++) {

			for (int j = 0; j < tempMat.get(0).size(); j++) {

				A.get(i).set(j + i, tempMat.get(i).get(j));

			}

		}
		for (int i = 0; i < A.size(); i++) {
			A.get(i).remove(0);
			A.get(i).remove(A.get(i).size() - 1);
		}

		mVector = linearEquationSolver(A, vector);
		mVector.add(0, 0.0);
		mVector.add(0.0);

		// ArrayList<ArrayList<Double>> mVectorGrouped =
		// groupToInterval((ArrayList<Double>) mVector);
		return (ArrayList<Double>) mVector;

	}

	private static ArrayList<ArrayList<Double>> generateCof(ArrayList<Double> intervalX1, ArrayList<Double> intervalX2,
			ArrayList<Double> intervalY1, ArrayList<Double> intervalY2) {
		ArrayList<Double> cof = new ArrayList<>();
		ArrayList<Double> Y = new ArrayList<>();
		double temp1 = intervalX1.get(1) - intervalX1.get(0);
		double temp2 = intervalX2.get(1) - intervalX1.get(0);
		double temp3 = intervalX2.get(1) - intervalX2.get(0);
		double temp4 = intervalY2.get(1) - intervalY2.get(0);
		double temp5 = intervalY1.get(1) - intervalY1.get(0);
		double cof1 = temp1 / 6.0;
		double cof2 = temp2 / 3.0;
		double cof3 = temp3 / 6.0;
		double y = (temp4 / temp3) - (temp5 / temp1);
		cof.add(cof1);
		cof.add(cof2);
		cof.add(cof3);
		Y.add(y);
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		result.add(cof);
		result.add(Y);
		return result;
	}

	static ArrayList<Double> linearEquationSolver(List<List<Double>> matA, List<Double> vectB) {
		ArrayList<Double> x = new ArrayList<Double>();
		double[][] tempMat = new double[matA.size()][matA.get(0).size()];
		double[] tempVect = new double[vectB.size()];
		for (int i = 0; i < matA.size(); i++) {
			tempVect[i] = vectB.get(i);
			for (int j = 0; j < matA.get(0).size(); j++) {
				tempMat[i][j] = matA.get(i).get(j);

			}
		}

		RealMatrix MatA = new Array2DRowRealMatrix(tempMat, false);
		DecompositionSolver solver = new LUDecomposition(MatA).getSolver();
		RealVector vect = new ArrayRealVector(tempVect, false);
		RealVector solution = solver.solve(vect);

		for (int i = 0; i < vectB.size(); i++) {
			x.add(solution.getEntry(i));
		}

		return x;

	}

	public static ArrayList<ArrayList<Double>> groupToInterval(ArrayList<Double> arr) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < arr.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			if (i + groupSize < arr.size()) {
				temp.add(arr.get(i));
				temp.add(arr.get(i + groupSize));
				result.add(temp);
			}

		}
		return result;
	}

	public static int identifyInterval(ArrayList<ArrayList<Double>> x, double interpolationValue) {
		for (int i = 0; i < x.size(); i++) {
			if (x.get(i).get(0) <= interpolationValue && x.get(i).get(x.get(i).size() - 1) >= interpolationValue) {
				return i;

			}
		}
		return -1;

	}

	public static double interpolationFunction(ArrayList<ArrayList<Double>> xInter, ArrayList<ArrayList<Double>> yInt,
			ArrayList<ArrayList<Double>> mInter, double interpolationValue) {
		int index = identifyInterval(xInter, interpolationValue);
		double result = 0.0;
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
		result = res0 + res1 - res2;

		return result;

	}

	public static ArrayList<ArrayList<Double>> getInterpolatingData(ArrayList<Double> allData) {
		// int numberOfDataPointsToConsider=10;
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

	static boolean canInterpolate(ArrayList<Double> data) {
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