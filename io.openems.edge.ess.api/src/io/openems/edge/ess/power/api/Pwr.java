package io.openems.edge.ess.power.api;

public enum Pwr {
	ACTIVE("P"), REACTIVE("Q");

	private final String symbol;

	Pwr(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return this.symbol;
	}
}
