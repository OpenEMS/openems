package io.openems.edge.predictor.lstmmodel.preprocessing;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class CombineFeatureTest {

	@Test
	public void multiplication() {
		double[] featureA = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0 };
		double[] featureB = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0 };
		double[] result = { 1.0, 4.0, 9.0, 16.0, 25.0, 36.0, 49.0, 64.0, 81.0, 100.0, 121.0, 144.0 };

		assertTrue(Arrays.equals(DataModification.elementWiseMultiplication(featureA, featureB), result));

	}

	// @Test
	protected void divisioTest() {
		double[] featureA = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0 };
		double[] featureB = { 1.0, 2.0, 3.0, 0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0 };
		double[] result = { 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
		assertTrue(Arrays.equals(DataModification.elementWiseDiv(featureA, featureB), result));
	}

}
