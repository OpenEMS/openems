package io.openems.edge.core.appmanager.formly.expression;

import java.util.function.Function;

import io.openems.edge.core.appmanager.formly.Exp;

public record ArrayExpression(String array) {

	/**
	 * Creates a {@link ArrayExpression} of the given values.
	 * 
	 * @param values the values of the array
	 * @return the created {@link ArrayExpression}
	 */
	public static ArrayExpression of(String... values) {
		return new ArrayExpression("[" + String.join(",", values) + "]");
	}

	/**
	 * Creates a variable of the length of the current array.
	 * 
	 * @return the {@link Variable} to obtain the length from
	 */
	public Variable length() {
		return Exp.dynamic(this.array() + ".length");
	}

	/**
	 * Creates a new {@link ArrayExpression} which is filtered by the given filter.
	 * 
	 * @param filter the filter to be applied on every element
	 * @return the new filtered {@link ArrayExpression}
	 */
	public ArrayExpression filter(Function<Variable, BooleanExpression> filter) {
		return new ArrayExpression(this.methodWithVariable("filter", filter));
	}

	/**
	 * Joins the array into a {@link StringExpression} separated by the given
	 * delimiter.
	 * 
	 * @param delimiter the delimiter of the elements
	 * @return the created {@link StringExpression}
	 */
	public StringExpression join(String delimiter) {
		return new StringExpression(this.array() + ".join('" + delimiter + "')");
	}

	/**
	 * Creates a {@link BooleanExpression} which checks if every element matches the
	 * resulting {@link BooleanExpression} of the predicate.
	 * 
	 * @param predicate the function to get the expression to validate every
	 *                  element; the supplied variable represents the current
	 *                  element of the array
	 * @return the final {@link BooleanExpression}
	 */
	public BooleanExpression every(Function<Variable, BooleanExpression> predicate) {
		return new BooleanExpression(this.methodWithVariable("every", predicate));
	}

	/**
	 * Creates a {@link BooleanExpression} which checks if some elements match the
	 * resulting {@link BooleanExpression} of the predicate.
	 * 
	 * @param predicate the function to get the expression to validate every
	 *                  element; the supplied variable represents the current
	 *                  element of the array
	 * @return the final {@link BooleanExpression}
	 */
	public BooleanExpression some(Function<Variable, BooleanExpression> predicate) {
		return new BooleanExpression(this.methodWithVariable("some", predicate));
	}

	private String methodWithVariable(String method, Function<Variable, BooleanExpression> inside) {
		final var variable = Exp.dynamic("i");
		return this.array() //
				+ "." + method //
				+ "(" //
				+ variable.variable() //
				+ " => " //
				+ inside.apply(variable).expression() //
				+ ")";
	}

}