package io.openems.edge.io.shelly.shellypro2pm;

public enum TerminalEnum {
	RELAY_1(0), //
	RELAY_2(1);

	private TerminalEnum(int shellyIndex) {
		this.shellyIndex = shellyIndex;
	}

	private final int shellyIndex;

	public int getShellyIndex() {
		return this.shellyIndex;
	}
}
