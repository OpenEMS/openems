package io.openems.edge.evse.api;

public enum SingleThreePhase {
	SINGLE_PHASE(1), //
	THREE_PHASE(3);

	public final int count;

	private SingleThreePhase(int count) {
		this.count = count;
	}
}