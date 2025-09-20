package io.openems.edge.ess.power.api;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;

public class Coefficient {

	protected final int index;
	protected final String essId;
	protected final SingleOrAllPhase phase;
	protected final Pwr pwr;

	public Coefficient(int index, String essId, SingleOrAllPhase phase, Pwr pwr) {
		this.index = index;
		this.essId = essId;
		this.phase = phase;
		this.pwr = pwr;
	}

	@Override
	public String toString() {
		return this.essId + this.pwr.symbol + this.phase.symbol;
	}

	public int getIndex() {
		return this.index;
	}

	public String getEssId() {
		return this.essId;
	}

	public SingleOrAllPhase getPhase() {
		return this.phase;
	}

	public Pwr getPwr() {
		return this.pwr;
	}
}
