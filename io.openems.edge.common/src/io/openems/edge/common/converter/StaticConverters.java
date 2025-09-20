package io.openems.edge.common.converter;

import java.util.function.Function;

public class StaticConverters {

	/**
	 * Converts only positive values from Element to Channel.
	 */
	public static final Function<Object, Object> KEEP_POSITIVE = value -> {
		return switch (value) {
		case null -> null;
		case Boolean b -> b;
		case String s -> s;
		case Short s -> s > 0 ? s : 0;
		case Integer i -> i > 0 ? i : 0;
		case Long l -> l > 0 ? l : 0;
		case Float f -> f > 0 ? f : 0;
		case Double d -> d > 0 ? d : 0;
		default ->
			throw new IllegalArgumentException("Converter KEEP_POSITIVE does not accept the type of [" + value + "]");
		};
	};

	/**
	 * Invert a value.
	 */
	public static final Function<Object, Object> INVERT = value -> {
		return switch (value) {
		case null -> null;
		case Boolean b -> !b;
		case String s -> s; // impossible
		case Short s -> s * -1;
		case Integer i -> i * -1;
		case Long l -> l * -1;
		case Float f -> f * -1;
		case Double d -> d * -1;
		default -> throw new IllegalArgumentException("Converter INVERT does not accept the type of [" + value + "]");
		};
	};
}
