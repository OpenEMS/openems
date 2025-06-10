package io.openems.common.function;

import java.util.function.BiFunction;

/**
 * This interface is similar to the java.util interface {@link BiFunction}.
 * Difference is, that it allows the apply() method to throw a defined
 * {@link Exception}.
 *
 * @param <T> the apply methods first argument type
 * @param <U> the apply methods second argument type
 * @param <R> the type of the result of the function
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Exception> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @return the function result
	 * @throws E on error
	 */
	public R apply(T t, U u) throws E;

}
