package io.openems.edge.ess.power.api;

public enum Relationship {
	/**
	 * Equals: x = y
	 */
	EQUALS("="),
	/**
	 * Greater or equals: x >= y
	 */
	GREATER_OR_EQUALS(">="),
	/**
	 * Less or equals: x <= y
	 */
	LESS_OR_EQUALS("<=");

	private final String operator;

	private Relationship(String operator) {
		this.operator = operator;
	}

	public String getOperator() {
		return operator;
	}
}
