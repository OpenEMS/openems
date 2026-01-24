package io.openems.edge.evse.chargepoint.alpitronic.enums;

public enum Connector {
	SLOT_0(100), //
	SLOT_1(200), //
	SLOT_2(300), //
	SLOT_3(400);

	public final int modbusOffset;

	private Connector(int modbusOffset) {
		this.modbusOffset = modbusOffset;
	}
}
