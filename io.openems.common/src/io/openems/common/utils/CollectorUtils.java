package io.openems.common.utils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

}
