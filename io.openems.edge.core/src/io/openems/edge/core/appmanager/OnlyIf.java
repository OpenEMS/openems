package io.openems.edge.core.appmanager;

import java.util.function.Consumer;

public interface OnlyIf<T extends OnlyIf<T>> extends Self<T> {

	/**
	 * Only executes the given consumer if the expression is true.
	 * 
	 * @param expression the expression
	 * @param consumer   the {@link Consumer} to execute
	 * @return {@link Self#self()}
	 */
	public default T onlyIf(boolean expression, Consumer<T> consumer) {
		if (expression) {
			consumer.accept(this.self());
		}
		return this.self();
	}

}
