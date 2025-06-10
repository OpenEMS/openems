package io.openems.common.function;

import java.util.function.BiConsumer;

/**
 * This interface is similar to the java.util interface {@link BiConsumer}.
 * Difference is, that it allows to pass to the apply() method one more
 * parameter.
 *
 * @param <T> the apply methods first argument type
 * @param <U> the apply methods second argument type
 * @param <V> the apply methods third argument type
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @param v the third function argument
	 */
	public void accept(T t, U u, V v);

}
