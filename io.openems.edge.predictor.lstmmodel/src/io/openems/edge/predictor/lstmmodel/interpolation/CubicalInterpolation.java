package io.openems.edge.predictor.lstmmodel.interpolation;

import java.util.ArrayList;
import java.util.Arrays;
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

	ArrayList<ArrayList<ArrayList<Double>>> interpolatingInterval = new ArrayList<ArrayList<ArrayList<Double>>>();
	ArrayList<Double> data = new ArrayList<Double>();
	ArrayList<ArrayList<Double>> temp01 = new ArrayList<ArrayList<Double>>();
	ArrayList<ArrayList<Double>> matrixAInverted = new ArrayList<ArrayList<Double>>();
	// Do not change this
	int groupSize = 1;

	public CubicalInterpolation() {

	}

	public void interpolate() {

		ArrayList<Double> x = new ArrayList<Double>(Arrays.asList(1.00, 2.00, 3.00, 4.00));
		ArrayList<Double> y = new ArrayList<Double>(Arrays.asList(1.0, 0.5, 0.333, 0.25));
		ArrayList<ArrayList<Double>> xInterval = this.groupToInterval(x);
		ArrayList<ArrayList<Double>> yInterval = this.groupToInterval(y);
		ArrayList<Double> mVector = this.generateAndSolveTriDiagonal(xInterval, yInterval);
		double result = this.interpolationFunction(xInterval, yInterval, this.groupToInterval(mVector), 2.55);
		System.out.println("result : " + result);
		this.interpolatingInterval = LinearInterpolation.determineInterpolatingPoints(this.data);

	}

	private ArrayList<Double> generateAndSolveTriDiagonal(//
			ArrayList<ArrayList<Double>> x, ArrayList<ArrayList<Double>> y) {
		ArrayList<ArrayList<Double>> cof = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> tempMat = new ArrayList<ArrayList<Double>>();
		List<Double> vector = new ArrayList<Double>();
		int colLen = x.size() - 1;
		int rowLen = colLen + 2;

		List<List<Double>> array = IntStream.range(0, colLen) //
				.mapToObj(i -> new ArrayList<Double>(Collections.nCopies(rowLen, 0d))) //
				.collect(Collectors.toList());

		for (int i = 0; i < x.size(); i++) {
			if (i < x.size() - 1) {
				cof = this.generateCof(x.get(i), x.get(i + 1), y.get(i), y.get(i + 1));
				vector.add(cof.get(1).get(0));
				tempMat.add(cof.get(0));
			} else {
				// Do nothing}
			}
		}

		for (int i = 0; i < tempMat.size(); i++) {
			for (int j = 0; j < tempMat.get(0).size(); j++) {
				array.get(i).set(j + i, tempMat.get(i).get(j));
			}

		}
		for (int i = 0; i < array.size(); i++) {
			array.get(i).remove(0);
			array.get(i).remove(array.get(i).size() - 1);
		}
		// solving system of linear equation
		List<Double> mVector = this.linearEquationSolver(array, vector);
		// updating the m vector
		mVector.add(0, 0.0);
		mVector.add(0.0);
		System.out.println("mVector");
		System.out.println(mVector);

		ArrayList<ArrayList<Double>> mVectorGrouped = this.groupToInterval((ArrayList<Double>) mVector);
		System.out.println(mVectorGrouped);
		return (ArrayList<Double>) mVector;

	}

	private ArrayList<ArrayList<Double>> generateCof(ArrayList<Double> intervalX1, //
			ArrayList<Double> intervalX2, //
			ArrayList<Double> intervalY1, //
			ArrayList<Double> intervalY2) {
		ArrayList<Double> cof = new ArrayList<>();
		double temp1 = intervalX1.get(1) - intervalX1.get(0);
		double temp2 = intervalX2.get(1) - intervalX1.get(0);
		double temp3 = intervalX2.get(1) - intervalX2.get(0);
		double temp4 = intervalY2.get(1) - intervalY2.get(0);
		double temp5 = intervalY1.get(1) - intervalY1.get(0);
		double cof1 = temp1 / 6.0;
		double cof2 = temp2 / 3.0;
		double cof3 = temp3 / 6.0;

		cof.add(cof1);
		cof.add(cof2);
		cof.add(cof3);
		double y = (temp4 / temp3) - (temp5 / temp1);
		ArrayList<Double> intermediatelist = new ArrayList<>();
		intermediatelist.add(y);
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		result.add(cof);
		result.add(intermediatelist);
		return result;
	}

	// Attempting to solve for the
	// linear equation in the
	// form A*X=B
	ArrayList<Double> linearEquationSolver(List<List<Double>> matA, List<Double> vectB) {
		ArrayList<Double> x = new ArrayList<Double>();
		// using library Apache math
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

		System.out.print(x);
		return x;

	}

	public ArrayList<ArrayList<Double>> groupToInterval(ArrayList<Double> arr) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < arr.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			if (i + this.groupSize < arr.size()) {
				temp.add(arr.get(i));
				temp.add(arr.get(i + this.groupSize));
				result.add(temp);
			}

		}
		return result;
	}

	public int identifyInterval(ArrayList<ArrayList<Double>> x, double interpolationValue) {
		for (int i = 0; i < x.size(); i++) {
			if (x.get(i).get(0) <= interpolationValue && x.get(i).get(x.get(i).size() - 1) >= interpolationValue) {
				return i;
			}
		}
		return -1;
	}

	public double interpolationFunction(ArrayList<ArrayList<Double>> xInter, ArrayList<ArrayList<Double>> yInt,
			ArrayList<ArrayList<Double>> mInter, double interpolationValue) {

		int index = this.identifyInterval(xInter, interpolationValue);
		double temp1 = 0.0;
		double temp2 = 0.0;
		double temp3 = 0.0;
		temp1 = (double) xInter.get(index).get(1) - interpolationValue;
		temp2 = (double) interpolationValue - xInter.get(index).get(0);
		temp3 = xInter.get(index).get(1) - xInter.get(index).get(0);
		double res0 = (//
		(Math.pow(temp1, 3) * mInter.get(index).get(0) //
				+ Math.pow(temp2, 3) * mInter.get(index).get(1))//
				/ (6 * temp3));
		double res1 = ((temp1) * yInt.get(index).get(0) + temp2 * yInt.get(index).get(1)) / (temp3);
		double res2 = (temp3 * (temp1 * mInter.get(index).get(0) + temp2 * mInter.get(index).get(1))) / 6;
		double result = res0 + res1 - res2;
		return result;
	}
}
