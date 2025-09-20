package io.openems.edge.core.appmanager;

import io.openems.common.function.ThrowingConsumer;

public interface ThrowingOnlyIf<T extends ThrowingOnlyIf<T>> extends Self<T> {

	/**
	 * Executes the consumer only if the statement is true.
	 * 
	 * @param <E>       the type of the exception
	 * @param statement the statement to determine if the consumer should get
	 *                  executed
	 * @param consumer  the consumer to execute if the statement is true
	 * @return this
	 * @throws E if the consumer throws the specified exception
	 */
	public default <E extends Exception> T throwingOnlyIf(boolean statement, ThrowingConsumer<T, E> consumer) throws E {
		if (statement) {
			consumer.accept(this.self());
		}
		return this.self();
	}

}
