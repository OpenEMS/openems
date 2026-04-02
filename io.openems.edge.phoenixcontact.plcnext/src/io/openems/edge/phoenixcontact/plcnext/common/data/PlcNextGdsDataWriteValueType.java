package io.openems.edge.phoenixcontact.plcnext.common.data;

/**
 * Defines available variable types of variables to be written to controller
 */
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
