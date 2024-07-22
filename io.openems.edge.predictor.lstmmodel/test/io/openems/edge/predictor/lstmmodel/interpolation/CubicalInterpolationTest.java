package io.openems.edge.predictor.lstmmodel.interpolation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class CubicalInterpolationTest {

	@Test
	public void testCanInterpolate() {
		ArrayList<Double> validData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, null, Double.NaN));
		CubicalInterpolation inter = new CubicalInterpolation(validData);

		assertTrue(inter.canInterpolate());

		ArrayList<Double> invalidData = new ArrayList<>(Arrays.asList(1.0, null, 3.0, Double.NaN));
		inter.setData(invalidData);
		assertFalse(inter.canInterpolate());

		ArrayList<Double> exactlyFourData = new ArrayList<>(Arrays.asList(1.0, 2.0, null, 4.0, 5.0));
		inter.setData(exactlyFourData);
		assertTrue(inter.canInterpolate());

		ArrayList<Double> allNullOrNaNData = new ArrayList<>(Arrays.asList(null, Double.NaN, null, Double.NaN));
		inter.setData(allNullOrNaNData);
		assertFalse(inter.canInterpolate());

		ArrayList<Double> emptyData = new ArrayList<>();
		inter.setData(emptyData);
		assertFalse(inter.canInterpolate());
	}

	@Test

	public void testInterpolate() {

		ArrayList<Double> validData = new ArrayList<>(Arrays.asList(2.0, 4.0, Double.NaN, 8.0));
		ArrayList<Double> expectedResult = new ArrayList<>(Arrays.asList(2.0, 4.0, 6.0, 8.0));

		CubicalInterpolation inter = new CubicalInterpolation(validData);

		ArrayList<Double> interpolatedData = inter.compute();
		assertEquals(interpolatedData, expectedResult);

	}

}
