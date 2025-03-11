package io.openems.edge.evse.api;

public enum SingleThreePhase {
	SINGLE(1), //
	THREE(3);

	public final int count;

	private SingleThreePhase(int count) {
		this.count = count;
	}
}