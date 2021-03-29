package io.openems.common.function;

import java.util.function.BiConsumer;

/**
 * This interface is similar to the java.util interface {@link BiConsumer}.
 * Difference is, that it allows the accept() method to throw a defined
 * {@link Exception}.
 *
 * @param <T> the apply methods first argument type
 * @param <U> the apply methods second argument type
 * @param <R> the type of the result of the function
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Exception> {

	public R apply(T t, U u) throws E;

}
