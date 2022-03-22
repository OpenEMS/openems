package io.openems.edge.ess.power.api;

public class Coefficient {

	protected final int index;
	protected final String essId;
	protected final Phase phase;
	protected final Pwr pwr;

	public Coefficient(int index, String essId, Phase phase, Pwr pwr) {
		this.index = index;
		this.essId = essId;
		this.phase = phase;
		this.pwr = pwr;
	}

	@Override
	public String toString() {
		return this.essId + this.pwr.getSymbol() + this.phase.getSymbol();
	}

	public int getIndex() {
		return this.index;
	}

	public String getEssId() {
		return this.essId;
	}

	public Phase getPhase() {
		return this.phase;
	}

	public Pwr getPwr() {
		return this.pwr;
	}
}
