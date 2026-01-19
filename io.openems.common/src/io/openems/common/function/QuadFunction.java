package io.openems.common.function;

import java.util.function.BiFunction;

/**
 * This interface is similar to the java.util interface {@link BiFunction}.
 * Difference is, that it allows to pass to the apply() method two more
 * parameter.
 *
 * @param <A> the apply methods first argument type
 * @param <B> the apply methods second argument type
 * @param <C> the apply methods third argument type
 * @param <D> the apply methods fourth argument type
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface QuadFunction<A, B, C, D, R> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param a the first function argument
	 * @param b the second function argument
	 * @param c the third function argument
	 * @param d the fourth function argument
	 * @return the function result
	 */
	public R apply(A a, B b, C c, D d);

}
