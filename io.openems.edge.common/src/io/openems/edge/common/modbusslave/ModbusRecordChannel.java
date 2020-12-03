package io.openems.edge.common.modbusslave;

import java.util.function.Consumer;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents one record in a ModbusSlave implementation that maps to a Channel.
 */
public interface ModbusRecordChannel extends ModbusRecord {

	/**
	 * Gets the actual value as a byte-array of the appropriate length for the
	 * ModbusType.
	 * 
	 * @param component the applied OpenemsComponent
	 * @return the byte-array
	 */
	public byte[] getValue(OpenemsComponent component);

	/**
	 * Gets the Channel-Id of this record.
	 * 
	 * @return the Channel-Id
	 */
	public ChannelId getChannelId();

	/**
	 * Set the callback for writes to this record.
	 * 
	 * @param onWriteValueCallback the callback
	 */
	public void onWriteValue(Consumer<Object> onWriteValueCallback);

	/**
	 * Writes a value to this ModbusRecordChannel. Actual handling is forwarded to
	 * the onWriteValueCallback.
	 * 
	 * @param index the buffer index for Modbus Word
	 * @param byte1 the first byte
	 * @param byte2 the second byte
	 */
	public void writeValue(int index, byte byte1, byte byte2);

	/**
	 * Sets the Component-ID of this record.
	 * 
	 * @param componentId the Component-ID
	 */
	public void setComponentId(String componentId);

	/**
	 * Gets the Component-ID of this record
	 * 
	 * @return the Component-ID
	 */
	public String getComponentId();
}
