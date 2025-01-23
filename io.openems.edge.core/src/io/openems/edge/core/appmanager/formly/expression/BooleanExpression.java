package io.openems.edge.core.appmanager.formly.expression;

import io.openems.edge.core.appmanager.formly.enums.Operator;

public record BooleanExpression(String expression) {

	/**
	 * Creates a {@link BooleanExpression} which checks the first {@link Variable}
	 * against the second {@link Variable} with the given {@link Operator}.
	 * 
	 * @param v1 the first {@link Variable}
	 * @param op the {@link Operator} to use while checking
	 * @param v2 the second {@link Variable}
	 * @return the created {@link BooleanExpression}
	 */
	public static BooleanExpression of(Variable v1, Operator op, Variable v2) {
		return new BooleanExpression(v1.variable() + " " + op.getOperation() + " " + v2.variable());
	}

	/**
	 * Combines the current {@link BooleanExpression} with an or with the given
	 * {@link BooleanExpression}.
	 * 
	 * @param other the other {@link BooleanExpression} to combine
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression or(BooleanExpression other) {
		return new BooleanExpression(this.expression() + " || " + other.expression());
	}

	/**
	 * Combines the current {@link BooleanExpression} with an and with the given
	 * {@link BooleanExpression}.
	 * 
	 * @param other the other {@link BooleanExpression} to combine
	 * @return the created {@link BooleanExpression}
	 */
	public BooleanExpression and(BooleanExpression other) {
		return new BooleanExpression(this.expression() + " && " + other.expression());
	}

}