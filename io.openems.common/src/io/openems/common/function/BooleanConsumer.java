package io.openems.common.function;

import java.util.function.Consumer;

/**
 * This interface is similar to the java.util interface {@link Consumer}.
 * Difference is, that it limits the type to a primitive, non-nullable boolean.
 */
@FunctionalInterface
public interface BooleanConsumer {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param value the input argument
	 */
	public void accept(boolean value);

}
