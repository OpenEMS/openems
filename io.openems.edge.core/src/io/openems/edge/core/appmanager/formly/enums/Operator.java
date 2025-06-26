package io.openems.edge.core.appmanager.formly.enums;

public enum Operator {
	// Equals
	EQ("=="), //
	// Not-Equals
	NEQ("!="), //
	// Greater-Than-Equals
	GTE(">="), //
	// Greater-Than
	GT(">"), //
	// Lower-Than-Equals
	LTE("<="), //
	// Lower-Than
	LT("<"), //
	;

	private final String operation;

	private Operator(String operation) {
		this.operation = operation;
	}

	public String getOperation() {
		return this.operation;
	}
}