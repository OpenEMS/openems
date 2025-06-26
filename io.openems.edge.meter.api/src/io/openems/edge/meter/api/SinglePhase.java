package io.openems.edge.meter.api;

public enum SinglePhase {
	L1("L1"), //
	L2("L2"), //
	L3("L3");

	private final String symbol;

	private SinglePhase(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return this.symbol;
	}

}
