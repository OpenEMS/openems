package io.openems.common.function;

import java.util.function.Function;

/**
 * This interface is similar to the java.util interface {@link Function}.
 * Difference is, that it allows the apply() method to throw a defined
 * {@link Exception}.
 *
 * @param <T> the apply methods argument type
 * @param <R> the apply methods return type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 * @throws E on error
	 */
	public R apply(T t) throws E;

}
