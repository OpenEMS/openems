package io.openems.edge.battery.fenecon.home;

public enum BatteryInverterPort {
	PORT_1(1), //
	PORT_2(2), //
	;

	public final int port;

	private BatteryInverterPort(int port) {
		this.port = port;
	}
}
