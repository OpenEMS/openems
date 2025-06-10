package io.openems.edge.predictor.lstm.utillities;

import static io.openems.edge.predictor.lstm.utilities.UtilityConversion.getMinIndex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class UtilityConversionTest {

	@Test
	public void testGetMinIndex() {
		double[] arr = { 3.5, 2.0, 5.1, 1.2, 4.8 };
		assertEquals(3, getMinIndex(arr));
	}

	@Test
	public void testGetMinIndexEmptyArray() {
		double[] arr = {};
		assertThrows(IllegalArgumentException.class, () -> getMinIndex(arr));
	}

	@Test
	public void testGetMinIndexNullArray() {
		double[] arr = null;
		assertThrows(IllegalArgumentException.class, () -> getMinIndex(arr));
	}

}
