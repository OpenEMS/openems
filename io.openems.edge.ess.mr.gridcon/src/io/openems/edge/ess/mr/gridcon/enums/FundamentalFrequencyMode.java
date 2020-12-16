package io.openems.edge.ess.mr.gridcon.enums;

public enum FundamentalFrequencyMode {

	DISABLED(false, false), // = 0
	Q_REF(true, false), // = 1
	PFC_COS_PHI(false, true), // = 2
	V_REF(true, true) // = 3
	;

	private boolean bit1;
	private boolean bit2;

	private FundamentalFrequencyMode(boolean bit1, boolean bit2) {
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
