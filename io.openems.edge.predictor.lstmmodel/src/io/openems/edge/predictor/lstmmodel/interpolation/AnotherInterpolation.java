package io.openems.edge.predictor.lstmmodel.interpolation;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

public class AnotherInterpolation {

	public static void main(String[] args) {

		double x[] = { 1.00, 2.00, 3.00, 4.00 };
		double y[] = { 1.0, 0.5, 0.333, 0.25 };

		SplineInterpolator inter = new SplineInterpolator();

		System.out.println(inter.interpolate(x, y).toString());

	}

}
