package io.openems.edge.ess.mr.gridcon.enums;

public enum BalancingMode {

	DISABLED(false, false), // = 0
	NEGATIVE_SEQUENCE_COMPENSATION(true, false), // = 1
	ZERO_SEQUENCE_COMPENSATION(false, true), // = 2
	NEGATIVE_AND_ZERO_SEQUENCE_COMPENSATION(true, true) // = 3
	;

	private boolean bit1;
	private boolean bit2;

	private BalancingMode(boolean bit1, boolean bit2) {
		this.bit1 = bit1;
		this.bit2 = bit2;
	}

	public boolean isBit1() {
		return this.bit1;
	}

	public boolean isBit2() {
		return this.bit2;
	}
}
