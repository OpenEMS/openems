package io.openems.edge.controller.evcs;

public enum ChargeMode {
	
	DEFAULT(0), FORCE_CHARGE(0);
	
	private int minPower;

	private ChargeMode(int minPower) {
		this.minPower = minPower;
	}
	

	public int getMinPower() {
		return minPower;
	}

	public ChargeMode setMinPower(int minPower) {
		this.minPower = minPower;
		return this;
	}
}
