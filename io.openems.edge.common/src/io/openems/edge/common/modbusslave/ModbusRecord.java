package io.openems.edge.common.modbusslave;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class ModbusRecord {

	private final int offset;
	private final ModbusType type;

	private String componentId = null;

	public ModbusRecord(int offset, ModbusType type) {
		this.offset = offset;
		this.type = type;
	}

	public int getOffset() {
		return this.offset;
	}

	public ModbusType getType() {
		return this.type;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getComponentId() {
		return this.componentId;
	}

	public abstract String getName();

	public abstract String getValueDescription();

	public Unit getUnit() {
		return Unit.NONE;
	}

	public abstract byte[] getValue(OpenemsComponent component);

	public abstract void writeValue(OpenemsComponent component, int index, byte byte1, byte byte2);

	public abstract AccessMode getAccessMode();
}
