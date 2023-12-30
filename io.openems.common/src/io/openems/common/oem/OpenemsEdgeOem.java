package io.openems.common.oem;

public interface OpenemsEdgeOem {

	// NOTE: Following values are adopted from SunSpec "Common Model"
	// definition. Please refer to SunSpec documentation for details.

	/**
	 * The manufacturer of the device that is running OpenEMS Edge.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 *
	 * @return the value
	 */
	public String getManufacturer();

	/**
	 * The model identifier of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 * 
	 * @return the value
	 */
	public String getManufacturerModel();

	/**
	 * The options of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 * 
	 * @return the value
	 */
	public String getManufacturerOptions();

	/**
	 * The version of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 * 
	 * @return the value
	 */
	public String getManufacturerVersion();

	/**
	 * The serial number of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 * 
	 * @return the value
	 */
	public String getManufacturerSerialNumber();

	/**
	 * The Energy-Management-System serial number of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 * 
	 * @return the value
	 */
	public String getManufacturerEmsSerialNumber();

	/**
	 * The Websocket URL for OpenEMS Backend.
	 * 
	 * <p>
	 * Note: this value usually starts with "ws://" or "wss://"
	 * 
	 * @return the value
	 */
	public String getBackendApiUrl();

	/**
	 * The measurement Tag used to write data to InfluxDB.
	 * 
	 * <p>
	 * Note: this value defaults to "edge"
	 * 
	 * @return the value
	 */
	public default String getInfluxdbTag() {
		return "edge";
	}

	public record SystemUpdateParams(String packageName, String latestVersionUrl, String updateScriptUrl, String updateScriptParams) {
	}

	/**
	 * The parameters for the integrated OpenEMS Edge System-Update feature.
	 * 
	 * <p>
	 * See 'Host' in 'io.openems.edge.common' for details
	 * 
	 * @return the record
	 */
	public SystemUpdateParams getSystemUpdateParams();
}
