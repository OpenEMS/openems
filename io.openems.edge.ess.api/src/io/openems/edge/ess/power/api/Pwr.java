package io.openems.edge.ess.power.api;

public enum Pwr {
	ACTIVE(0), REACTIVE(1);

	private final int offset;

	Pwr(int offset) {
		this.offset = offset;
	}
	
	public int getOffset() {
		return offset;
	}
}
