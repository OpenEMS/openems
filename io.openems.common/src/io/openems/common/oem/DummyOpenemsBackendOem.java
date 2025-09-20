package io.openems.common.oem;

/**
 * A default {@link DummyOpenemsBackendOem} for OpenEMS Backend.
 */
public class DummyOpenemsBackendOem implements OpenemsBackendOem {

	@Override
	public String getInfluxdbTag() {
		return "edge";
	}

}