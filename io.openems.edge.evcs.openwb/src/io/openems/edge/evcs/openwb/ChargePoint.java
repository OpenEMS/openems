package io.openems.edge.evcs.openwb;

public enum ChargePoint {
	CP0(0), //
	CP1(1);

	public final int value;

	private ChargePoint(int value) {
		this.value = value;
	}
}
