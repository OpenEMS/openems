package io.openems.edge.predictor.lstmmodel.utilities;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class UtilityConversionTesting {

	@Test
	public void test() {

		Integer[] input = { 30132761, 30498548, 29822883 };
		List<Integer> toBeConverted = new ArrayList<Integer>(Arrays.asList(input));

		Double[] output = { 3.0132761E7, 3.0498548E7, 2.9822883E7 };
		List<Double> outputList = new ArrayList<Double>(Arrays.asList(output));

		assertEquals(UtilityConversion.convertListIntegerToListDouble(toBeConverted), outputList);

	}

	@Test
	public void test1() {

		Integer[] input = { 30132761, 30498548, null, 29822883 };
		List<Integer> toBeConverted = new ArrayList<Integer>(Arrays.asList(input));

		Double[] output = { 3.0132761E7, 3.0498548E7, 0.0, 2.9822883E7 };
		List<Double> outputList = new ArrayList<Double>(Arrays.asList(output));

		assertEquals(UtilityConversion.convertListIntegerToListDouble(toBeConverted), outputList);

	}

}
