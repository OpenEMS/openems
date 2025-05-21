package io.openems.common.utils;

import java.util.Map;
import java.util.Map.Entry;
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

}
