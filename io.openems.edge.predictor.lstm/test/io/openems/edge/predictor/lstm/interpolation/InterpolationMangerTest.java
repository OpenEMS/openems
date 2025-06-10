package io.openems.edge.predictor.lstm.interpolation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class InterpolationMangerTest {

	private static HyperParameters hyperParameters = new HyperParameters();

	@Test
	public void calculateMeanShouldReturnNaNForEmptyList() {
		ArrayList<Double> emptyList = new ArrayList<>();
		double result = InterpolationManager.calculateMean(emptyList);
		assertEquals(Double.NaN, result, 0.0001);
	}

	@Test
	public void calculateMean_shouldReturnMeanWithoutNaN() {
		ArrayList<Double> dataList = new ArrayList<>(Arrays.asList(1.0, 2.0, Double.NaN, 4.0, 5.0));
		double result = InterpolationManager.calculateMean(dataList);
		assertEquals(3.0, result, 0.0001);
	}

	@Test
	public void testGroup() {

		ArrayList<Double> testData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 3.0, 4.0));
		int group = 3;
		ArrayList<ArrayList<Double>> expectedGroupedData = new ArrayList<>(Arrays.asList(//
				new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0)), //
				new ArrayList<>(Arrays.asList(4.0, 5.0, 6.0)), //
				new ArrayList<>(Arrays.asList(3.0, 4.0))//
		));

		ArrayList<ArrayList<Double>> result = InterpolationManager.group(testData, group);
		assertEquals(expectedGroupedData, result);
	}

	@Test
	public void testUnGroup() {

		ArrayList<ArrayList<Double>> groupedData = new ArrayList<>();
		groupedData.add(new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0)));
		groupedData.add(new ArrayList<>(Arrays.asList(4.0, 5.0, 6.0)));

		ArrayList<Double> expectedResult = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
		ArrayList<Double> result = InterpolationManager.unGroup(groupedData);
		assertEquals(expectedResult, result);
	}

	@Test
	public void testInterpolationManagerCaseLinear() {
		ArrayList<Double> data = new ArrayList<>(Arrays.asList(1.0, null, 3.0, Double.NaN, 5.0));
		ArrayList<Double> expectedData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0));
		InterpolationManager interpolationManager = new InterpolationManager(data, hyperParameters);
		assertEquals(interpolationManager.getInterpolatedData(), expectedData);
	}

	@Test
	public void testInterPolationManagerCaseCubical() {
		ArrayList<Double> data = new ArrayList<>(
				Arrays.asList(1.0, null, 3.0, Double.NaN, 5.0, 6.0, null, 7.0, 8.0, null, Double.NaN, 9.0));
		ArrayList<Double> expectedData = new ArrayList<>(Arrays.asList(1.0, 2.0092714608433737, 3.0, 3.9721856174698793,
				5.0, 6.0, 6.485598644578313, 7.0, 8.0, 8.671770414993308, 8.937416331994646, 9.0));
		InterpolationManager interpolationManager = new InterpolationManager(data, hyperParameters);
		assertEquals(interpolationManager.getInterpolatedData(), expectedData);
	}
}
