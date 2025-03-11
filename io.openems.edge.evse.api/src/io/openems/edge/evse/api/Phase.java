package io.openems.edge.evse.api;

public enum Phase {
	ALL(""), L1("L1"), L2("L2"), L3("L3");

	public final String symbol;

	private Phase(String symbol) {
		this.symbol = symbol;
	}
}
