package io.openems.common.exceptions;

import java.util.function.Function;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * This interface is similar to the java.util interface {@link Function}.
 * Difference is, that it allows the apply() method to throw an
 * {@link OpenemsNamedException}.
 *
 * @param <T> the apply methods argument type
 * @param <T> the apply methods return type
 */
@FunctionalInterface
public interface CheckedFunction<T, R> {

	public R apply(T t) throws OpenemsNamedException;

}
