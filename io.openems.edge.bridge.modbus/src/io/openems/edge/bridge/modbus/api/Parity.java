package io.openems.edge.bridge.modbus.api;

public enum Parity {
	NONE("none"),
	EVEN("even"),
	ODD("odd"),
	MARK("mark"),
	SPACE("space")
	;
	
	Parity(String parity) {
		this.parity = parity;
	}
	private String parity;
	public String getParity() {
		return parity;
	}		
}
