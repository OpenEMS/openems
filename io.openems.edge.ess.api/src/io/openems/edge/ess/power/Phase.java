package io.openems.edge.ess.power;

public enum Phase {
	L1(0), L2(2), L3(4);

	private final int offset;

	private Phase(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}
}
