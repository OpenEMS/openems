package io.openems.common.utils;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
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

}
