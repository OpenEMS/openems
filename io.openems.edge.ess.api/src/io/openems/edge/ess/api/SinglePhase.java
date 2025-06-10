package io.openems.edge.ess.api;

import io.openems.edge.ess.power.api.Phase;

public enum SinglePhase {
	L1("L1", Phase.L1), //
	L2("L2", Phase.L2), //
	L3("L3", Phase.L3);

	private final String symbol;
	private final Phase powerApiPhase;

	private SinglePhase(String symbol, Phase powerApiPhase) {
		this.symbol = symbol;
		this.powerApiPhase = powerApiPhase;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public Phase getPowerApiPhase() {
		return this.powerApiPhase;
	}

}
