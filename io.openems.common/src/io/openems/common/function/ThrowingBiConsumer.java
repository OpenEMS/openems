package io.openems.common.function;

import java.util.function.BiConsumer;

/**
 * This interface is similar to the java.util interface {@link BiConsumer}.
 * Difference is, that it allows the accept() method to throw a defined
 * {@link Exception}.
 *
 * @param <T> the accept methods first argument type
 * @param <U> the accept methods second argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Exception> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param t the first input argument
	 * @param u the second input argument
	 * @throws E on error
	 */
	public void accept(T t, U u) throws E;

}
