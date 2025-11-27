package io.openems.edge.io.phoenixcontact.gds;

public enum PlcNextGdsDataAspect {
	READ_TEST_VALUE("read_test_value"),
	
	WRITE_TEST_VALUE("wride_test_value");
	
	private final String identifier;
	
	private PlcNextGdsDataAspect(String identifier) {
		this.identifier = identifier;
	}
	
	public String getIdentifier() {
		return identifier;
	}
}
