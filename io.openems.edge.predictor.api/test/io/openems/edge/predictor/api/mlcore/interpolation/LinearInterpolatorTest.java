package io.openems.edge.predictor.api.mlcore.interpolation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LinearInterpolatorTest {

	@Test
	public void testInterpolate_ShouldReturnLinearInterpolation() {
		var interp = new LinearInterpolator(3);
		var values = Arrays.asList(1.0, null, null, 4.0);
		assertEquals(2.0, interp.interpolate(1, values), 1e-6);
		assertEquals(3.0, interp.interpolate(2, values), 1e-6);
	}

	@Test
	public void testInterpolate_ShouldReturnCeilingValue_WhenNoFloorValue() {
		var interp = new LinearInterpolator(2);
		var values = Arrays.asList(null, null, 5.0, 6.0);
		assertEquals(5.0, interp.interpolate(0, values), 1e-6);
		assertEquals(5.0, interp.interpolate(1, values), 1e-6);
	}

	@Test
	public void testInterpolate_ShouldReturnFloorValue_WhenNoCeilingValue() {
		var interp = new LinearInterpolator(2);
		var values = Arrays.asList(3.0, 4.0, null, null);
		assertEquals(4.0, interp.interpolate(2, values), 1e-6);
		assertEquals(4.0, interp.interpolate(3, values), 1e-6);
	}

	@Test
	public void testInterpolate_ShouldReturnNaN_WhenGapTooLarge() {
		var interp = new LinearInterpolator(1);
		var values = Arrays.asList(1.0, null, null, 4.0);
		assertTrue(Double.isNaN(interp.interpolate(1, values)));
		assertTrue(Double.isNaN(interp.interpolate(2, values)));
	}

	@Test
	public void shouldInterpolate_ShouldReturnNaN_WhenNoNeighbors() {
		var interp = new LinearInterpolator(2);
		List<Double> values = Arrays.asList(null, null, null);
		assertTrue(Double.isNaN(interp.interpolate(1, values)));
	}
}
