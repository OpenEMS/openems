package io.openems.common.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.collect.TreeBasedTable;

public final class CollectorUtils {

	private CollectorUtils() {
	}

	/**
	 * Creates a {@link Collector} which collects the given input to a {@link Map}
	 * where the value is also a {@link Map}.
	 * 
	 * @param <INPUT>         the input of the collection
	 * @param <FIRST_KEY>     the key of the first map
	 * @param <SECOND_KEY>    the key of the second map
	 * @param <VALUE>         the value of the second map
	 * @param firstKeyMapper  the mapper-function of the first key
	 * @param secondKeyMapper the mapper-function of the second key
	 * @param valueMapper     the mapper-function of the value
	 * @return the {@link Collector}
	 */
	public static final <INPUT, FIRST_KEY, SECOND_KEY, VALUE> //
	Collector<INPUT, ?, Map<FIRST_KEY, Map<SECOND_KEY, VALUE>>> toDoubleMap(//
			Function<INPUT, FIRST_KEY> firstKeyMapper, //
			Function<INPUT, SECOND_KEY> secondKeyMapper, //
			Function<INPUT, VALUE> valueMapper //
	) {
		return Collectors.groupingBy(firstKeyMapper, Collectors.toMap(secondKeyMapper, valueMapper));
	}

	/**
	 * Creates a {@link Collector} which collects the given input to a
	 * {@link TreeBasedTable}.
	 * 
	 * @param <KEY>   the type of the first map key
	 * @param <KEY2>  the type of the second map key
	 * @param <VALUE> the type of the value
	 * @return the {@link Collector}
	 */
	public static final <KEY extends Comparable<KEY>, KEY2 extends Comparable<KEY2>, VALUE> //
	Collector<Entry<KEY, Map<KEY2, VALUE>>, ?, TreeBasedTable<KEY, KEY2, VALUE>> toTreeBasedTable() {
		return Collector.of(TreeBasedTable::create, (t, u) -> {
			for (var entry : u.getValue().entrySet()) {
				t.put(u.getKey(), entry.getKey(), entry.getValue());
			}
		}, (t, u) -> {
			t.putAll(u);
			return t;
		});
	}

	/**
	 * Returns a {@link Collector} that accumulates input elements into a
	 * {@link SortedMap}. The keys and values are produced by applying the provided
	 * mapping functions to the input elements.
	 * 
	 * <p>
	 * If the mapped keys contain duplicates, an {@link IllegalStateException} will
	 * be thrown.
	 *
	 * @param <T>           the type of input elements to the reduction operation
	 * @param <K>           the output type of the key mapping function
	 * @param <V>           the output type of the value mapping function
	 * @param keyMapper     a mapping function to produce keys
	 * @param valueMapper   a mapping function to produce values
	 * @param mergeFunction a merge function to resolve collisions between values
	 *                      associated with the same key
	 * @return a {@code Collector} which collects elements into a {@code SortedMap}
	 */
	public static <T, K, V> Collector<T, ?, ? extends SortedMap<K, V>> toSortedMap(//
			Function<? super T, ? extends K> keyMapper, //
			Function<? super T, ? extends V> valueMapper, //
			BinaryOperator<V> mergeFunction //
	) {
		return toTreeMap(keyMapper, valueMapper, mergeFunction);
	}

	/**
	 * Returns a {@link Collector} that accumulates input elements into a
	 * {@link NavigableMap}. The keys and values are produced by applying the
	 * provided mapping functions to the input elements.
	 * 
	 * <p>
	 * If the mapped keys contain duplicates, an {@link IllegalStateException} will
	 * be thrown.
	 *
	 * @param <T>           the type of input elements to the reduction operation
	 * @param <K>           the output type of the key mapping function
	 * @param <V>           the output type of the value mapping function
	 * @param keyMapper     a mapping function to produce keys
	 * @param valueMapper   a mapping function to produce values
	 * @param mergeFunction a merge function to resolve collisions between values
	 *                      associated with the same key
	 * @return a {@code Collector} which collects elements into a
	 *         {@code NavigableMap}
	 */
	public static <T, K, V> Collector<T, ?, ? extends NavigableMap<K, V>> toNavigableMap(
			Function<? super T, ? extends K> keyMapper, //
			Function<? super T, ? extends V> valueMapper, //
			BinaryOperator<V> mergeFunction //
	) {
		return toTreeMap(keyMapper, valueMapper, mergeFunction);
	}

	/**
	 * Returns a {@link Collector} that accumulates input elements into a
	 * {@link TreeMap}. The keys and values are produced by applying the provided
	 * mapping functions to the input elements.
	 *
	 * <p>
	 * If the mapped keys contain duplicates, an {@link IllegalStateException} will
	 * be thrown.
	 *
	 * @param <T>           the type of input elements to the reduction operation
	 * @param <K>           the output type of the key mapping function
	 * @param <V>           the output type of the value mapping function
	 * @param keyMapper     a mapping function to produce keys
	 * @param valueMapper   a mapping function to produce values
	 * @param mergeFunction a merge function to resolve collisions between values
	 *                      associated with the same key
	 * @return a {@code Collector} which collects elements into a
	 *         {@code NavigableMap}
	 */
	public static <T, K, V> Collector<T, ?, TreeMap<K, V>> toTreeMap(//
			Function<? super T, ? extends K> keyMapper, //
			Function<? super T, ? extends V> valueMapper, //
			BinaryOperator<V> mergeFunction //
	) {
		return Collectors.toMap(keyMapper, valueMapper, mergeFunction, TreeMap::new);
	}

}
