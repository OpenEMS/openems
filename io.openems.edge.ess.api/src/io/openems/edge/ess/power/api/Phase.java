package io.openems.edge.ess.power.api;

public enum Phase {
	ALL(""), L1("L1"), L2("L2"), L3("L3");

	private final String symbol;

	private Phase(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return this.symbol;
	}
}
