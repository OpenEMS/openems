package io.openems.common.function;

import java.util.function.BiFunction;

/**
 * This interface is similar to the java.util interface {@link BiFunction}.
 * Difference is, that it allows to pass to the apply() method one more
 * parameter.
 *
 * @param <T> the apply methods first argument type
 * @param <U> the apply methods second argument type
 * @param <S> the apply methods third argument type
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface TriFunction<T, U, S, R> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @param s the third function argument
	 * @return the function result
	 */
	public R apply(T t, U u, S s);

}
