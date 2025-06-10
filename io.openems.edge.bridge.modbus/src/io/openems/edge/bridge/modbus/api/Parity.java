package io.openems.edge.bridge.modbus.api;

import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;

public enum Parity {
	NONE(AbstractSerialConnection.NO_PARITY), //
	ODD(AbstractSerialConnection.ODD_PARITY), //
	EVEN(AbstractSerialConnection.EVEN_PARITY), //
	MARK(AbstractSerialConnection.MARK_PARITY), //
	SPACE(AbstractSerialConnection.SPACE_PARITY);

	Parity(int value) {
		this.value = value;
	}

	private int value;

	public int getValue() {
		return this.value;
	}
}
