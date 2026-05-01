package io.openems.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.OptionalDouble;

import org.junit.Test;

public class DoubleUtilsTest {

	@Test
	public void testGetOrNull() {
		assertNull(DoubleUtils.getOrNull(OptionalDouble.empty()));
		assertEquals(5., DoubleUtils.getOrNull(OptionalDouble.of(5.)), 0.001);
	}

	@Test
	public void testDoubleToDoubleFunction() {
		assertEquals(10., new DoubleUtils.DoubleToDoubleFunction() {

			@Override
			public double apply(double value) {
				return value * 2;
			}
		}.apply(5.), 0.001);
	}

}
