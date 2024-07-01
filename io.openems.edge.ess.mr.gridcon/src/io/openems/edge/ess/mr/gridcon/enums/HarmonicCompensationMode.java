package io.openems.edge.ess.mr.gridcon.enums;

public enum HarmonicCompensationMode {

	DISABLED(false, false), // = 0
	DELTA(true, false), // = 1
	Y(false, true), // = 2
	DELTA_AND_Y(true, true) // = 3
	;

	private boolean bit1;
	private boolean bit2;

	private HarmonicCompensationMode(boolean bit1, boolean bit2) {
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
