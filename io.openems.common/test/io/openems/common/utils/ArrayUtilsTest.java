package io.openems.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class ArrayUtilsTest {

	@Test
	public void testConcat() {
		final var firstArray = new Integer[] { 1, 2, 3 };
		final var secondArray = new Integer[] { 4, 5 };
		final var thirdArray = new Integer[] {};
		final var fourthArray = new Integer[] { 6 };

		var concattedArray = ArrayUtils.concat(firstArray, secondArray);
		assertEquals(firstArray.length + secondArray.length, concattedArray.length);
		assertEquals(firstArray[0], concattedArray[0]);
		assertEquals(firstArray[1], concattedArray[1]);
		assertEquals(firstArray[2], concattedArray[2]);
		assertEquals(secondArray[0], concattedArray[3]);
		assertEquals(secondArray[1], concattedArray[4]);

		concattedArray = ArrayUtils.concatAll(firstArray, secondArray, thirdArray, fourthArray);
		assertEquals(firstArray.length + secondArray.length + fourthArray.length, concattedArray.length);
		assertEquals(firstArray[0], concattedArray[0]);
		assertEquals(firstArray[1], concattedArray[1]);
		assertEquals(firstArray[2], concattedArray[2]);
		assertEquals(secondArray[0], concattedArray[3]);
		assertEquals(secondArray[1], concattedArray[4]);
		assertEquals(fourthArray[0], concattedArray[5]);

		var firstList = new ArrayList<Integer>();
		firstList.add(5);
		firstList.add(6);

		var secondList = new ArrayList<Integer>();
		secondList.add(7);
		secondList.add(8);

		var thirdList = new ArrayList<Integer>();
		thirdList.add(9);

		concattedArray = ArrayUtils.concatLists(Integer[]::new, firstList, secondList, new ArrayList<>(), thirdList);
		assertEquals(firstList.get(0), concattedArray[0]);
		assertEquals(firstList.get(1), concattedArray[1]);
		assertEquals(secondList.get(0), concattedArray[2]);
		assertEquals(secondList.get(1), concattedArray[3]);
		assertEquals(thirdList.get(0), concattedArray[4]);

		concattedArray = ArrayUtils.concatAll();
		assertEquals(0, concattedArray.length);
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