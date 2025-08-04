package io.openems.common.utils;

import java.util.Arrays;

public class ArrayUtils {

	/**
	 * Creates a new array by concatenating two arrays of the same type.
	 *
	 * @param <T>    the component type of the arrays
	 * @param first  the first array
	 * @param second the second array
	 * @return a new array containing all elements from both input arrays
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

}
