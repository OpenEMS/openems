package io.openems.edge.common.type;

import static org.junit.Assert.*;

import org.junit.Test;

public class TypeUtilsTest {

	@Test
	public void testAverage() {
		// no values
		assertEquals(null, TypeUtils.average());

		// null values
		assertEquals(null, TypeUtils.average(null, null, null));

		// int value
		assertEquals(2, Math.round(TypeUtils.average(1, 2, 3)));

		// float values
		assertEquals(2.5f, TypeUtils.average(2, 3), 0.001);

		// mixed values
		assertEquals(2.5f, TypeUtils.average(2, null, 3), 0.001);
	}

	@Test
	public void testAverageRounded() {
		// no values
		assertEquals(null, TypeUtils.averageRounded());

		// null values
		assertEquals(null, TypeUtils.averageRounded(null, null, null));

		// int value
		assertEquals(Integer.valueOf(2), TypeUtils.averageRounded(1, 2, 3));

		// float values
		assertEquals(Integer.valueOf(3), TypeUtils.averageRounded(2, 3));

		// mixed values
		assertEquals(Integer.valueOf(3), TypeUtils.averageRounded(2, null, 3));
	}

}
