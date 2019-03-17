package io.openems.edge.ess.mr.gridcon.enums;

public enum InverterCount {
	ONE(1),
	TWO(2),
	THREE(3);
	
	private int count;
	
	private InverterCount(int count) {
		this.count = count;
	}
	
	public int getCount() {
		return this.count;
	}
}
