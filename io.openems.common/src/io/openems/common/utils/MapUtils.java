package io.openems.common.utils;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Function;

public final class MapUtils {

	/**
	 * Maps the keys of a {@link Map}.
	 * 
	 * @param <K1>      the old type of the key
	 * @param <K2>      the new type of the key
	 * @param <V>       the type of the value
	 * @param map       the {@link Map} to map
	 * @param keyMapper the key mapper function
	 * @return a new {@link Map} with the mapped keys
	 */
	public static <K1, K2, V> Map<K2, V> mapKey(Map<K1, V> map, Function<K1, K2> keyMapper) {
		return map.entrySet().stream() //
				.collect(toMap(//
						e -> keyMapper.apply(e.getKey()), //
						Map.Entry::getValue));
	}

	/**
	 * Maps the values of a {@link Map}.
	 * 
	 * @param <K>         the type of the key
	 * @param <V1>        the old type of the value
	 * @param <V2>        the new type of the value
	 * @param map         the {@link Map} to map
	 * @param valueMapper the value mapper function
	 * @return a new {@link Map} with the mapped values
	 */
	public static <K, V1, V2> Map<K, V2> mapValue(Map<K, V1> map, Function<V1, V2> valueMapper) {
		return map.entrySet().stream() //
				.collect(toMap(//
						Map.Entry::getKey, //
						e -> valueMapper.apply(e.getValue())));
	}

	private MapUtils() {
	}
}
