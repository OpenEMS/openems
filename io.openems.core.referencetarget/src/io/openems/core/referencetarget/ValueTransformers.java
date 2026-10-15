package io.openems.core.referencetarget;

import java.util.Map;

public final class ValueTransformers {

	private static final Map<String, ValueTransformer> BY_NAME = Map.of(//
			"componentIdFromChannel", new ComponentIdFromChannelTransformer() //
	);

	private ValueTransformers() {
	}

	/**
	 * Gets a transformer by its name.
	 *
	 * @param name the transformer name used in an expression
	 * @return the matching transformer
	 * @throws IllegalArgumentException if the name is unknown
	 */
	public static ValueTransformer byName(String name) {
		final var transformer = BY_NAME.get(name);
		if (transformer == null) {
			throw new IllegalArgumentException("Unknown transformer: " + name);
		}
		return transformer;
	}
}