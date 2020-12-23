package io.openems.edge.common.type;

import static org.junit.Assert.assertEquals;

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

	@Test
	public void testMin() {
		assertEquals(25, (int) TypeUtils.min(null, 25, null, 40, null));
		assertEquals(null, TypeUtils.min(null, null, null));
		assertEquals(null, TypeUtils.min());
		assertEquals(17, (int)TypeUtils.min(null, 17, 25, 40));
		assertEquals(34, (int)TypeUtils.min(null, 34, 40));
	}

}
