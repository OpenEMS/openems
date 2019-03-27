package io.openems.common.exceptions;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * This interface is similar to the java.util interface {@link Consumer}.
 * Difference is, that it allows the accept() method to throw an
 * {@link OpenemsNamedException}.
 *
 * @param <T> the accept methods argument type
 */
@FunctionalInterface
public interface CheckedConsumer<T> {

	public void accept(T t) throws OpenemsNamedException;

}
