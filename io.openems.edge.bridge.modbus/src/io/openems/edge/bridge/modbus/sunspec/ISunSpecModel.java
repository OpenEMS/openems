package io.openems.edge.bridge.modbus.sunspec;

/**
 * Represents a SunSpec Model
 */
public interface ISunSpecModel {

	/**
	 * The name of the SunSpec Model.
	 * 
	 * It is expected to be "S_<Block-ID>", e.g. for the common Block-ID "1" the
	 * expected name is "S_1".
	 * 
	 * @return the name as String
	 */
	public String name();

	/**
	 * Gets the SunSpec Block-ID as integer.
	 * 
	 * @return the Block-ID
	 */
	public default int getBlockId() {
		return Integer.valueOf(this.name().substring(2));
	}

	public String label();

	public SunSpecPoint[] points();

}
