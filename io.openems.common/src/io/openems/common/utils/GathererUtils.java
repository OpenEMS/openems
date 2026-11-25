package io.openems.common.utils;

import java.util.stream.Gatherer;

public final class GathererUtils {

	/**
	 * Creates a {@link Gatherer} which passes only those elements downstream which
	 * are an instance of the given type, already cast to that type.
	 * 
	 * @param <I>  the type of the input elements
	 * @param <O>  the type to filter and cast to
	 * @param type the {@link Class} to check the elements against
	 * @return the {@link Gatherer}
	 */
	public static <I, O> Gatherer<I, Void, O> filterIsInstance(Class<O> type) {
		return Gatherer.of(Gatherer.Integrator.ofGreedy((_, element, downstream) -> {
			if (type.isInstance(element)) {
				return downstream.push(type.cast(element));
			}
			return !downstream.isRejecting();
		}));
	}

	private GathererUtils() {
	}
}
