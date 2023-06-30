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

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Gets the value description.
	 * 
	 * @return the value description
	 */
	public abstract String getValueDescription();

	public Unit getUnit() {
		return Unit.NONE;
	}

	/**
	 * Gets the value.
	 * 
	 * @param component the actual {@link OpenemsComponent}
	 * @return the value as byte array
	 */
	public abstract byte[] getValue(OpenemsComponent component);

	/**
	 * Sets the write value.
	 * 
	 * @param index the buffer index
	 * @param byte1 the first byte
	 * @param byte2 the second byte
	 */
	public abstract void writeValue(int index, byte byte1, byte byte2);

	/**
	 * Gets the {@link AccessMode}.
	 * 
	 * @return the {@link AccessMode}
	 */
	public abstract AccessMode getAccessMode();
}
