package io.openems.edge.predictor.lstm.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Suffle {
	private double[][] data;
	private double[] target;

	public Suffle(double[][] data1, double[] target1) {
		this.data = new double[data1.length][data1[0].length];
		this.target = new double[target1.length];

		for (int i = 0; i < data1.length; i++) {

			for (int j = 0; j < data1[0].length; j++) {

				this.data[i][j] = data1[i][j];
				// System.out.print(data[i][j]);
				this.target[i] = target1[i];
			}

		}

		this.suffelIt();

	}

	/**
	 * Shuffles the data and target arrays to randomize the order of elements. This
	 * method shuffles the data and target arrays simultaneously, ensuring that the
	 * corresponding data and target values remain aligned.
	 */

	public void suffelIt() {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		double[][] suffledData = new double[this.data.length][this.data[0].length];

		double[] suffledTarget = new double[this.data.length];
		for (int i = 0; i < this.data.length; i++) {
			temp.add(i);
		}

		Random r = new Random(100);
		Collections.shuffle(temp, r);
		for (int i = 0; i < this.data.length; i++) {

			suffledData[i] = (this.data[temp.get(i)]);

			suffledTarget[i] = this.target[temp.get(i)];

		}
		this.data = suffledData;
		this.target = suffledTarget;

	}

	public double[] getTarget() {
		return this.target;
	}

	public double[][] getData() {
		return this.data;
	}

}
