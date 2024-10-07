package io.openems.edge.predictor.lstmmodel.common;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DataStatisticsTest {

	public static final List<Double> DATALIST = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
	public static final ArrayList<Double> DATA = new ArrayList<>(DATALIST);
	public static final ArrayList<Double> EMPTYDATA = new ArrayList<>();

	@Test
	public void testGetMean() {
		double result = DataStatistics.getMean(DATA);
		assertEquals(3.0, result, 0.0001);
	}

	@Test
	public void testGetMeanEmptyList() {
		double result = DataStatistics.getMean((ArrayList<Double>) EMPTYDATA);
		assertEquals(0.0, result, 0.0001);
	}

	@Test
	public void testGetStandardDeviation() {
		double result = DataStatistics.getStandardDeviation(DATA);
		assertEquals(1.41421, result, 0.0001);
	}

	@Test
	public void testGetStandardDeviationEmptyList() {
		assertEquals(Double.NaN, DataStatistics.getStandardDeviation(EMPTYDATA), 0.0001);
	}

	@Test
	public void testGetStanderDeviation() {
		double result = DataStatistics.getStandardDeviation((ArrayList<Double>) DATA);
		assertEquals(1.41421, result, 0.0001);
	}

	@Test
	public void testGetStanderDeviationEmptyList() {
		assertEquals(Double.NaN, DataStatistics.getStandardDeviation((ArrayList<Double>) EMPTYDATA), 0.0001);
	}

	@Test
	public void testComputeRms() {
		double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
		double[] computed = { 1.1, 2.2, 3.1, 4.2, 5.1 };
		double expectedRms = 0.1483239;
		assertEquals(expectedRms, DataStatistics.computeRms(original, computed), 0.0001);
	}

}
