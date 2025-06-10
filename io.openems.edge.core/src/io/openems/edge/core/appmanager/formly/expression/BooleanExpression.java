package io.openems.edge.core.appmanager.formly.expression;

import io.openems.edge.core.appmanager.formly.Exp;
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

	/**
	 * Creates a combined {@link StringExpression} of the current
	 * {@link BooleanExpression} and {@link StringExpression}, which returns the
	 * first {@link StringExpression} if the given {@link BooleanExpression} returns
	 * true otherwise the second {@link StringExpression} gets returned.
	 * 
	 * @param ifTrue  the {@link StringExpression} to use when the statement returns
	 *                true
	 * @param ifFalse the {@link StringExpression} to use when the statement returns
	 *                false
	 * @return the final {@link StringExpression}
	 * 
	 * @see Exp#ifElse(BooleanExpression, StringExpression, StringExpression)
	 */
	public StringExpression ifElse(StringExpression ifTrue, StringExpression ifFalse) {
		return Exp.ifElse(this, ifTrue, ifFalse);
	}

	/**
	 * Creates a combined {@link Variable} of the current {@link BooleanExpression}
	 * and {@link Variable}, which returns the first {@link Variable} if the given
	 * {@link BooleanExpression} returns true otherwise the second {@link Variable}
	 * gets returned.
	 * 
	 * @param ifTrue  the {@link Variable} to use when the statement returns true
	 * @param ifFalse the {@link Variable} to use when the statement returns false
	 * @return the final {@link Variable}
	 * 
	 * @see Exp#ifElse(BooleanExpression, Variable, Variable)
	 */
	public Variable ifElse(Variable ifTrue, Variable ifFalse) {
		return Exp.ifElse(this, ifTrue, ifFalse);
	}

	/**
	 * Creates a combined {@link BooleanExpression} of the current
	 * {@link BooleanExpression} and {@link BooleanExpression}, which returns the
	 * first {@link BooleanExpression} if the given {@link BooleanExpression}
	 * returns true otherwise the second {@link BooleanExpression} gets returned.
	 * 
	 * @param ifTrue  the {@link BooleanExpression} to use when the statement
	 *                returns true
	 * @param ifFalse the {@link BooleanExpression} to use when the statement
	 *                returns false
	 * @return the final {@link BooleanExpression}
	 * 
	 * @see Exp#ifElse(BooleanExpression, BooleanExpression, BooleanExpression)
	 */
	public BooleanExpression ifElse(BooleanExpression ifTrue, BooleanExpression ifFalse) {
		return Exp.ifElse(this, ifTrue, ifFalse);
	}

}