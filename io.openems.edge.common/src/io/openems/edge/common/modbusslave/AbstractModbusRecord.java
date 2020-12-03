package io.openems.edge.common.modbusslave;

import io.openems.common.channel.Unit;

public abstract class AbstractModbusRecord implements ModbusRecord {

	private final int offset;
	private final ModbusType type;

	public AbstractModbusRecord(int offset, ModbusType type) {
		this.offset = offset;
		this.type = type;
	}

	public int getOffset() {
		return this.offset;
	}

	public ModbusType getType() {
		return this.type;
	}

	public Unit getUnit() {
		return Unit.NONE;
	}
}
