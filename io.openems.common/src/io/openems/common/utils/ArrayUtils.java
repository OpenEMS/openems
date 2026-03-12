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

	/**
	 * Checks if an array contains a specific element, ignoring null values.
	 *
	 * @param <T>     the component type of the array
	 * @param array   the array to search
	 * @param element the element to find
	 * @return true if the array contains the element (ignoring nulls), false
	 *         otherwise
	 * @implNote For comparison {@link Object#equals(Object)} is used
	 */
	public static <T> boolean containsIgnoreNull(T[] array, T element) {
		if (array == null || element == null) {
			return false;
		}
		for (T item : array) {
			if (item != null && item.equals(element)) {
				return true;
			}
		}
		return false;
	}

}
