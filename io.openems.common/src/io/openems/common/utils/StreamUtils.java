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
		Enumeration<K> keys = dictionary.keys();
		return Collections.list(keys).stream().map(key -> Map.entry(key, dictionary.get(key)));
	}
}
