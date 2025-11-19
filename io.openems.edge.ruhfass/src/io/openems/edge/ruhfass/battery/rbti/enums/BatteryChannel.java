package io.openems.edge.ruhfass.battery.rbti.enums;

public enum BatteryChannel {
	ONE(0), //
	TWO(0x1000), //
	THREE(0x2000), //
	FOUR(0x3000);

	public final int offset;

	private BatteryChannel(int offset) {
		this.offset = offset;
	}
}
