package io.openems.edge.controller.api.modbus;

import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;

public enum FlowControlIn {
	DISABLED(AbstractSerialConnection.FLOW_CONTROL_DISABLED), //
	RTS_ENABLED(AbstractSerialConnection.FLOW_CONTROL_RTS_ENABLED), //
	CTS_ENABLED(AbstractSerialConnection.FLOW_CONTROL_CTS_ENABLED), //
	DSR_ENABLED(AbstractSerialConnection.FLOW_CONTROL_DSR_ENABLED), //
	DTR_ENABLED(AbstractSerialConnection.FLOW_CONTROL_DTR_ENABLED), //
	XONXOFF_IN_ENABLED(AbstractSerialConnection.FLOW_CONTROL_XONXOFF_IN_ENABLED);

	FlowControlIn(int value) {
		this.value = value;
	}

	private int value;

	public int getValue() {
		return this.value;
	}
}
