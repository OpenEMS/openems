package io.openems.edge.battery.fenecon.home;

public enum BatteryInverterPort {
	PORT_1(1), //
	;

	public final int port;

	private BatteryInverterPort(int port) {
		this.port = port;
	}
}
