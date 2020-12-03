package io.openems.edge.common.modbusslave;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;

/**
 * Represents one record in a ModbusSlave implementation.
 */
public interface ModbusRecord {

	/**
	 * Gets the Name of this Record.
	 * 
	 * @return the Name
	 */
	public String getName();

	/**
	 * Gets the Modbus Address offset.
	 * 
	 * @return the offset
	 */
	public int getOffset();

	/**
	 * Gets the ModbusType of this record.
	 * 
	 * @return the ModbusType
	 */
	public ModbusType getType();

	/**
	 * Gets the value description for this record.
	 * 
	 * <p>
	 * Example: "Range between 0 and 100 %"
	 * 
	 * @return the value description
	 */
	public String getValueDescription();

	/**
	 * Gets the actual value as a byte-array of the appropriate length for the
	 * ModbusType.
	 * 
	 * @return the byte-array
	 */
	public byte[] getValue();

	/**
	 * Gets the AccessMode of this record.
	 * 
	 * @return the AccessMode
	 */
	public AccessMode getAccessMode();

	/**
	 * Gets the Unit of this record.
	 * 
	 * @return the Unit
	 */
	public Unit getUnit();

}
