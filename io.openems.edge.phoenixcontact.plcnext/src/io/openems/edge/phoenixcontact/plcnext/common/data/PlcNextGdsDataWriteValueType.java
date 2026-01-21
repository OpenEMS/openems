package io.openems.edge.phoenixcontact.plcnext.common.data;

public enum PlcNextGdsDataWriteValueType {
	VARIABLE("Variable"), //
	CONSTANT("Constant");

	private final String identifier;

	private PlcNextGdsDataWriteValueType(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return this.identifier;
	}
}
