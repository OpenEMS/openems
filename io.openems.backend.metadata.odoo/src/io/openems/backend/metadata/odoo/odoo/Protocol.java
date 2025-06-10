package io.openems.backend.metadata.odoo.odoo;

public enum Protocol {
	HTTPS("https"), HTTP("http");

	public final String expression;

	private Protocol(String expression) {
		this.expression = expression;
	}
}
