package io.openems.common.function;

import java.util.function.Supplier;

/**
 * This interface is similar to the java.util interface {@link Supplier}.
 * Difference is, that it allows the get() method to throw a defined
 * {@link Exception}.
 *
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

	/**
	 * Gets a result.
	 *
	 * @return a result
	 * @throws E on error
	 */
	public T get() throws E;

}
