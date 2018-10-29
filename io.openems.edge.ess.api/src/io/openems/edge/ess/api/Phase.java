package io.openems.edge.ess.api;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum Phase implements OptionsEnum {
	L1("L1", 1), L2("L2", 2), L3("L3", 3);

	private final String symbol;
	private final int value;

	private Phase(String symbol, int value) {
		this.symbol = symbol;
		this.value = value;
	}

	public String getSymbol() {
		return symbol;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getOption() {
		return this.symbol;
	}
}
