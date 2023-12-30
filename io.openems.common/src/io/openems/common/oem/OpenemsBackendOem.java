package io.openems.common.oem;

public interface OpenemsBackendOem {

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
