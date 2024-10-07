package io.openems.edge.predictor.lstmmodel.interpolation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class LinearInterpolationTest {
	@Test
	public void determineInterpolatingPointsTest() {

		ArrayList<Double> data = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, Double.NaN, 6.0, 7.0, 8.0,
				Double.NaN, Double.NaN, 11.0, 12.0, 13.0, Double.NaN, Double.NaN, Double.NaN, 17.0, Double.NaN, 19.0));

		ArrayList<ArrayList<Integer>> expectedResults = new ArrayList<>(
				Arrays.asList(new ArrayList<>(Arrays.asList(3, 5)), new ArrayList<>(Arrays.asList(7, 10)),
						new ArrayList<>(Arrays.asList(12, 16)), new ArrayList<>(Arrays.asList(16, 18))));

		ArrayList<ArrayList<Integer>> result = LinearInterpolation.determineInterpolatingPoints(data);
		assertEquals(result, expectedResults);

	}

	@Test
	public void computeInterpolationTest() {

		ArrayList<Double> data = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, Double.NaN, 6.0, 7.0, 8.0,
				Double.NaN, Double.NaN, 11.0, 12.0, 13.0, Double.NaN, Double.NaN, Double.NaN, 17.0, Double.NaN, 19.0));

		ArrayList<Double> expectedResult = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0,
				10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0));
		ArrayList<Double> results = LinearInterpolation.interpolate(data);
		// System.out.println(results);

		assertEquals(results, expectedResult);

	}

	@Test
	public void combineTest() {

		ArrayList<Double> data = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0,
				12.0, 13.0, Double.NaN, Double.NaN, Double.NaN, 17.0, 18.0, 19.0));

		ArrayList<Double> interpoltedValue = new ArrayList<>(Arrays.asList(14.0, 15.0, 16.0));

		ArrayList<Double> expectedResult = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0,
				10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0));

		ArrayList<Double> result = LinearInterpolation.combine(data, interpoltedValue, 12, 16);
		// System.out.println(result);
		assertEquals(result, expectedResult);

	}

	@Test
	public void computeInterPolation() {
		int xval1 = 12;
		int xValue2 = 16;
		double yvalue1 = 13;
		double yvalue2 = 17;
		ArrayList<Double> expectedResult = new ArrayList<>(Arrays.asList(14.0, 15.0, 16.0));

		ArrayList<Double> result = LinearInterpolation.computeInterpolation(xval1, xValue2, yvalue1, yvalue2);

		assertEquals(result, expectedResult);

	}

}
