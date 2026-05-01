package io.openems.common.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;

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

	/**
	 * Creates a new array by concatenating multiple arrays of the same type.
	 *
	 * @param <T>    the component type of the arrays
	 * @param arrays all arrays to concat
	 * @return a new array containing all elements from all input arrays
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concatAll(T[]... arrays) {
		if (arrays.length == 0) {
			var underlyingType = arrays.getClass().getComponentType().getComponentType();
			return (T[]) Array.newInstance(underlyingType, 0);
		}

		var finalSize = Arrays.stream(arrays).mapToInt(x -> x.length).sum();
		T[] result = Arrays.copyOf(arrays[0], finalSize);
		int resultIndex = arrays[0].length;

		for (int arrayIndex = 1; arrayIndex < arrays.length; arrayIndex++) {
			var array = arrays[arrayIndex];
			System.arraycopy(array, 0, result, resultIndex, array.length);
			resultIndex += array.length;
		}

		return result;
	}

	/**
	 * Creates a new array that contains all data from provided collections.
	 *
	 * @param <T>       the component type of the list and the resulting array
	 * @param generator function that creates a new array with the component type of
	 *                  the list and the specified size
	 * @param lists     Collections that should be combined
	 * @return a new array containing all elements from all provided collections
	 */
	@SafeVarargs
	public static <T> T[] concatLists(IntFunction<T[]> generator, Collection<T>... lists) {
		var size = Arrays.stream(lists).filter(Objects::nonNull).mapToInt(Collection::size).sum();
		T[] result = generator.apply(size);
		int resultIndex = 0;

		for (var list : lists) {
			if (list == null) {
				continue;
			}

			for (var element : list) {
				result[resultIndex] = element;
				resultIndex++;
			}
		}

		return result;
	}

}
