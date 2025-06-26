package io.openems.edge.bridge.modbus.api;

import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;

public enum Stopbit {
	ONE(AbstractSerialConnection.ONE_STOP_BIT), //
	ONE_POINT_FIVE(AbstractSerialConnection.ONE_POINT_FIVE_STOP_BITS), //
	TWO(AbstractSerialConnection.TWO_STOP_BITS); //

	Stopbit(int value) {
		this.value = value;
	}

	private int value;

	public int getValue() {
		return this.value;
	}
}
