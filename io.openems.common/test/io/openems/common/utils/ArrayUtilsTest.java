package io.openems.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArrayUtilsTest {

	@Test
	public void testConcat() {
		String[] first = { "a", "b" };
		String[] second = { "c", "d" };
		String[] expected = { "a", "b", "c", "d" };
		String[] result = ArrayUtils.concat(first, second);
		assertArrayEquals(expected, result);

		assertThrows(Exception.class, () -> {
			ArrayUtils.concat(first, null);
		});
		assertThrows(Exception.class, () -> {
			ArrayUtils.concat(null, second);
		});
	}

	@Test
	public void testContainsIgnoreNull() {
		assertTrue(ArrayUtils.containsIgnoreNull(new String[] { "test" }, "test"));
		assertFalse(ArrayUtils.containsIgnoreNull(new String[] { "test" }, "false"));

		assertFalse(ArrayUtils.containsIgnoreNull(new String[] { "test", null }, null));
		assertFalse(ArrayUtils.containsIgnoreNull(new String[] { "test", null }, "false"));
		assertFalse(ArrayUtils.containsIgnoreNull(null, "false"));
		assertFalse(ArrayUtils.containsIgnoreNull(null, null));
	}

}