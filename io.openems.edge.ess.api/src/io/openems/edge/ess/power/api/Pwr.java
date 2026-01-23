package io.openems.edge.ess.power.api;

public enum Pwr {
	ACTIVE("P"), //
	REACTIVE("Q");

	public final String symbol;

	private Pwr(String symbol) {
		this.symbol = symbol;
	}
}
