package io.openems.common.exceptions;

import java.util.function.Supplier;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * This interface is similar to the java.util interface {@link Supplier}.
 * Difference is, that it allows the get() method to throw an
 * {@link OpenemsNamedException}.
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

	public T get() throws OpenemsNamedException;

}
