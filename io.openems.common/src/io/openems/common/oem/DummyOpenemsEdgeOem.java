package io.openems.common.oem;

/**
 * A default {@link OpenemsEdgeOem} for OpenEMS Edge.
 */
public class DummyOpenemsEdgeOem implements OpenemsEdgeOem {

	@Override
	public String getManufacturer() {
		return "OpenEMS Association e.V.";
	}

	@Override
	public String getManufacturerModel() {
		return "OpenEMS";
	}

	@Override
	public String getManufacturerOptions() {
		return "";
	}

	@Override
	public String getManufacturerVersion() {
		return "";
	}

	@Override
	public String getManufacturerSerialNumber() {
		return "";
	}

	@Override
	public String getManufacturerEmsSerialNumber() {
		return "";
	}

	@Override
	public String getBackendApiUrl() {
		return "ws://localhost:8081";
	}

	@Override
	public String getInfluxdbTag() {
		return "edge";
	}

	@Override
	public SystemUpdateParams getSystemUpdateParams() {
		return new SystemUpdateParams(null, null, null);
	}
}