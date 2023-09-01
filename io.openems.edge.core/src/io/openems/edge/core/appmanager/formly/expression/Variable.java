package io.openems.edge.core.appmanager.formly.expression;

import io.openems.edge.core.appmanager.formly.enums.Operator;

public record Variable(String variable) {

	/**
	 * Non-static way to check if the current {@link Variable} value is equal the
	 * given {@link Variable} value.
	 * 
	 * @implNote does the same as
	 *           {@link BooleanExpression#of(Variable, Operator, Variable)
	 *           BooleanExpression.of(this, Operator.EQ, other)}
	 * 
	 * @param other the {@link Variable} to check against
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression equal(Variable other) {
		return BooleanExpression.of(this, Operator.EQ, other);
	}

	/**
	 * Non-static way to check if the current {@link Variable} value is not equal
	 * the given {@link Variable} value.
	 * 
	 * @implNote does the same as
	 *           {@link BooleanExpression#of(Variable, Operator, Variable)
	 *           BooleanExpression.of(this, Operator.NEQ, other)}
	 * 
	 * @param other the {@link Variable} to check against
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression notEqual(Variable other) {
		return BooleanExpression.of(this, Operator.NEQ, other);
	}

	/**
	 * Non-static way to check if the current {@link Variable} value is greater than
	 * the given {@link Variable} value.
	 * 
	 * @implNote does the same as
	 *           {@link BooleanExpression#of(Variable, Operator, Variable)
	 *           BooleanExpression.of(this, Operator.GT, other)}
	 * 
	 * @param other the {@link Variable} to check against
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression greaterThan(Variable other) {
		return BooleanExpression.of(this, Operator.GT, other);
	}

	/**
	 * Non-static way to check if the current {@link Variable} value is greater than
	 * equal the given {@link Variable} value.
	 * 
	 * @implNote does the same as
	 *           {@link BooleanExpression#of(Variable, Operator, Variable)
	 *           BooleanExpression.of(this, Operator.GTE, other)}
	 * 
	 * @param other the {@link Variable} to check against
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression greaterThanEqual(Variable other) {
		return BooleanExpression.of(this, Operator.GTE, other);
	}

	/**
	 * Non-static way to check if the current {@link Variable} value is lower than
	 * the given {@link Variable} value.
	 * 
	 * @implNote does the same as
	 *           {@link BooleanExpression#of(Variable, Operator, Variable)
	 *           BooleanExpression.of(this, Operator.LT, other)}
	 * 
	 * @param other the {@link Variable} to check against
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression lowerThan(Variable other) {
		return BooleanExpression.of(this, Operator.LT, other);
	}

	/**
	 * Non-static way to check if the current {@link Variable} value is lower than
	 * equal the given {@link Variable} value.
	 * 
	 * @implNote does the same as
	 *           {@link BooleanExpression#of(Variable, Operator, Variable)
	 *           BooleanExpression.of(this, Operator.LTE, other)}
	 * 
	 * @param other the {@link Variable} to check against
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression lowerThanEqual(Variable other) {
		return BooleanExpression.of(this, Operator.LTE, other);
	}

	/**
	 * Checks if the current value of the variable is not null.
	 * 
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression notNull() {
		return new BooleanExpression("!!" + this.variable());
	}

	/**
	 * Checks if the current value of the variable is null.
	 * 
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression isNull() {
		return new BooleanExpression("!" + this.variable());
	}

}