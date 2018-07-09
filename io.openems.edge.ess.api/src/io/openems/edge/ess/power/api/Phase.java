package io.openems.edge.ess.power.api;

public enum Phase {
	ALL(null), L1(0), L2(2), L3(4);

	Integer offset;

	Phase(Integer offset) {
		this.offset = offset;
	}
	
	public Integer getOffset() {
		return offset;
	}
}
