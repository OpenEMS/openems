package io.openems.common.utils;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils {

	/**
	 * Converts a Dictionary to a Stream of Map entries.
	 *
	 * @param dictionary the Dictionary to be converted
	 * @param <K>        the type of keys in the Dictionary
	 * @param <V>        the type of values in the Dictionary
	 * @return a Stream containing all the key-value pairs from the Dictionary as
	 *         Map entries
	 */
	public static <K, V> Stream<Entry<K, V>> dictionaryToStream(Dictionary<K, V> dictionary) {
		return enumerationToStream(dictionary.keys()).map(key -> Map.entry(key, dictionary.get(key)));
	}

	/**
	 * Converts an Enumeration to a Stream.
	 * 
	 * @param <T>  the type of elements in the Enumeration
	 * @param keys the Enumeration to be converted
	 * @return a Stream containing all elements from the Enumeration
	 */
	public static <T> Stream<T> enumerationToStream(Enumeration<T> keys) {
		return Collections.list(keys).stream();
	}

	/**
	 * Returns a {@link Collector} that accumulates input elements into a
	 * {@link Stream} containing only distinct elements according to a key extracted
	 * from each element.
	 *
	 * <p>
	 * Distinctness is determined by applying the provided {@code keyExtractor}
	 * function to each input element. If multiple elements produce the same key,
	 * the first element is taken.
	 * </p>
	 *
	 * <h3>Example</h3>
	 *
	 * <pre>{@code
	 * Stream<Person> people = ...;
	 * Stream<Person> distinctByEmail = people.collect(
	 *     distinctByKey(Person::getEmail)
	 * );
	 * }</pre>
	 *
	 * @param <T>          the type of input elements
	 * @param keyExtractor a function that extracts the key used to determine
	 *                     distinctness
	 * @return a {@code Collector} that collects elements into a {@code Stream<T>}
	 *         with unique keys
	 * @throws NullPointerException if {@code keyExtractor} or {@code mergeFunction}
	 *                              is null
	 * @implNote Internally, this collector builds a {@link java.util.Map Map} where
	 *           keys are derived using {@code keyExtractor} and values are the
	 *           elements themselves. After collection, it converts the map's values
	 *           into a {@link Stream}.
	 *
	 * @see java.util.stream.Collectors#toMap(Function, Function, BinaryOperator)
	 * @see java.util.stream.Collector
	 * @see StreamUtils#distinctByKey(Function, BinaryOperator)
	 */
	public static <T> Collector<T, ?, Stream<T>> distinctByKey(Function<? super T, ?> keyExtractor) {
		return distinctByKey(keyExtractor, (t, t2) -> t);
	}

	/**
	 * Returns a {@link Collector} that accumulates input elements into a
	 * {@link Stream} containing only distinct elements according to a key extracted
	 * from each element.
	 * 
	 * <p>
	 * Distinctness is determined by applying the provided {@code keyExtractor}
	 * function to each input element. If multiple elements produce the same key,
	 * the provided {@code mergeFunction} is used to resolve the conflict and select
	 * which element is retained.
	 * </p>
	 *
	 * <h3>Example</h3>
	 * 
	 * <pre>{@code
	 * Stream<Person> people = ...;
	 * Stream<Person> distinctByEmail = people.collect(
	 *     distinctByKey(Person::getEmail, (p1, p2) -> p1)
	 * );
	 * }</pre>
	 *
	 * @param <T>           the type of input elements
	 * @param keyExtractor  a function that extracts the key used to determine
	 *                      distinctness
	 * @param mergeFunction a function that merges two values with the same key
	 *                      (e.g., choosing one or combining them)
	 * @return a {@code Collector} that collects elements into a {@code Stream<T>}
	 *         with unique keys
	 * @throws NullPointerException if {@code keyExtractor} or {@code mergeFunction}
	 *                              is null
	 * @implNote Internally, this collector builds a {@link java.util.Map Map} where
	 *           keys are derived using {@code keyExtractor} and values are the
	 *           elements themselves. After collection, it converts the map's values
	 *           into a {@link Stream}.
	 *
	 * @see java.util.stream.Collectors#toMap(Function, Function, BinaryOperator)
	 * @see java.util.stream.Collector
	 * @see StreamUtils#distinctByKey(Function)
	 */
	public static <T> Collector<T, ?, Stream<T>> distinctByKey(Function<? super T, ?> keyExtractor,
			BinaryOperator<T> mergeFunction) {
		return Collectors.collectingAndThen(Collectors.toMap(keyExtractor, Function.identity(), mergeFunction),
				tMap -> tMap.values().stream());
	}

}
