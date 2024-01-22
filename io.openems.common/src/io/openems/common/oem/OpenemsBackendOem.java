package io.openems.common.oem;

public interface OpenemsBackendOem {

	/**
	 * Gets the App-Center Master-Key.
	 * 
	 * @return the value
	 */
	public default String getAppCenterMasterKey() {
		return "DUMMY_MASTER_KEY";
	}

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
}
