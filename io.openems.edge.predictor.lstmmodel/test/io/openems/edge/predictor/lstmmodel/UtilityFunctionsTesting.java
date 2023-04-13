package io.openems.edge.predictor.lstmmodel;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.util.Lstm;

public class UtilityFunctionsTesting {

	@Test
	public void test1() {

		Double[] y = { 100.0, 180.0, 260.0, 310.0, 40.0, 535.0, 695.0 };
		ArrayList<Double> testArray = new ArrayList<Double>(Arrays.asList(y));

		int expeceted = 4;
		assertEquals(expeceted, Lstm.findGlobalMinima(testArray));

	}

	@Test
	public void test2() {

		Double[] y = { 40.0, 535.0, 100.0, 180.0, 260.0, 310.0, 40.0, 535.0, 695.0 };
		ArrayList<Double> testArray = new ArrayList<Double>(Arrays.asList(y));

		int expeceted = 0;
		assertEquals(expeceted, Lstm.findGlobalMinima(testArray));

	}

	@Test
	public void test3() {

		Double[] y = { -3.123116986766261E-4, -1.2832842798005117E-4, 6.0946484976676096E-5, 2.555009806433084E-4,
				4.553225622149215E-4, 6.603982885842186E-4, 8.707147671392912E-4, 0.0010862581464099463, };
		ArrayList<Double> testArray = new ArrayList<Double>(Arrays.asList(y));

		int expeceted = 2;
		assertEquals(expeceted, Lstm.findGlobalMinima(testArray));

	}

	@Test
	public void test4() {

		Double[] y = { 6.0946484976676096E-5, 2.555009806433084E-4, 3.123116986766261E-4, 1.2832842798005117E-4,
				6.0946484976676096E-5, 2.555009806433084E-4, 4.553225622149215E-4, 6.603982885842186E-4,
				8.707147671392912E-4, 0.0010862581464099463 };
		ArrayList<Double> testArray = new ArrayList<Double>(Arrays.asList(y));

		int expeceted = 0;
		assertEquals(expeceted, Lstm.findGlobalMinima(testArray));

	}

}
