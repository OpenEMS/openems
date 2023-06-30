package io.openems.common.function;

import java.util.function.Consumer;

/**
 * This interface is similar to the java.util interface {@link Consumer}.
 * Difference is, that it allows the accept() method to throw a defined
 * {@link Exception}.
 *
 * @param <T> the accept methods argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param t the input argument
	 * @throws E on error
	 */
	public void accept(T t) throws E;

}
