package io.openems.edge.bridge.modbus.sunspec;

/**
 * Represents a SunSpec Model.
 */
public interface SunSpecModel {

	/**
	 * The name of the SunSpec Model.
	 *
	 * <p>
	 * It is expected to be "S_&lt;Block-ID&gt;", e.g. for the common Block-ID "1"
	 * the expected name is "S_1".
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
		return Integer.parseInt(this.name().substring(2));
	}

	/**
	 * The Label.
	 * 
	 * @return the Label
	 */
	public String label();

	/**
	 * The Points.
	 * 
	 * @return an array of {@link SunSpecPoint}s
	 */
	public SunSpecPoint[] points();

}
