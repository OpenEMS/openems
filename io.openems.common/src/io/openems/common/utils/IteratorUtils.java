package io.openems.common.utils;

import java.util.Iterator;

public final class IteratorUtils {

	public record IndexedEntry<T>(int index, T value) {

	}

	/**
	 * Wraps an {@link Iterable} to provide indexed access to its elements during
	 * iteration.
	 *
	 * <p>
	 * This helper method eliminates the need for manual counter variables (e.g.,
	 * {@code i++}) inside for-each loops. It provides an {@link IndexedEntry} for
	 * each iteration, containing both the zero-based index and the element's value.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>
	 * for (var entry : IteratorUtils.indexedIterable(myList)) {
	 * 	int index = entry.index();
	 * 	T value = entry.value();
	 * }
	 * </pre>
	 *
	 * @param <T>      the type of elements in the collection
	 * @param iterable the original {@link Iterable} to be indexed (e.g., List, Set)
	 * @return a new {@link Iterable} yielding {@link IndexedEntry} objects
	 */
	public static <T> Iterable<IndexedEntry<T>> indexedIterable(Iterable<T> iterable) {
		return () -> new Iterator<>() {

			private int indexCounter = 0;
			private final Iterator<T> iterator = iterable.iterator();

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public IndexedEntry<T> next() {
				return new IndexedEntry<>(this.indexCounter++, this.iterator.next());
			}
		};
	}

	private IteratorUtils() {

	}

}