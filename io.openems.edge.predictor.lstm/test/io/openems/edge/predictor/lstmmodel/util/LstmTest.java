package io.openems.edge.predictor.lstmmodel.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class LstmTest {
	@Test
	public void findGlobalMinima() {

		double[] errorList = { 1, 2, 3, 4, 5, 6, 3, 4, 0, 0.05, -5, 10 };
		int val = Lstm.findGlobalMinima(UtilityConversion.to1DArrayList(errorList));
		int expectedIndex = 8; //
		assertEquals(val, expectedIndex);

		ArrayList<Double> testData1 = new ArrayList<>(Arrays.asList(2.5, -3.7, 1.8, -4.2, 5.1));
		int expectedIndex1 = 2; //
		int actualIndex1 = Lstm.findGlobalMinima(testData1);
		assertEquals(expectedIndex1, actualIndex1);

		ArrayList<Double> testData2 = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0, 0.0));
		int expectedIndex2 = 0; //
		int actualIndex2 = Lstm.findGlobalMinima(testData2);
		assertEquals(expectedIndex2, actualIndex2);

		ArrayList<Double> testData3 = new ArrayList<>(Arrays.asList(5.5, -2.2, 3.3, -4.4));
		int expectedIndex3 = 1;
		int actualIndex3 = Lstm.findGlobalMinima(testData3);
		assertEquals(expectedIndex3, actualIndex3);

	}

}
