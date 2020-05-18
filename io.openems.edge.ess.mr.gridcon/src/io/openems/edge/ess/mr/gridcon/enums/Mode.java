package io.openems.edge.ess.mr.gridcon.enums;

public enum Mode {
	CURRENT_CONTROL(true), //
	VOLTAGE_CONTROL(false);

	public final boolean value;

	private Mode(boolean value) {
		this.value = value;
	}
}