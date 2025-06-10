package io.openems.edge.io.filipowski.analog.mr;

public enum AnalogOutput {
	OUTPUT_1(3000), //
	OUTPUT_2(3001), //
	OUTPUT_3(3002), //
	OUTPUT_4(3003) //
	;

	public final int startAddress;

	private AnalogOutput(int startAddress) {
		this.startAddress = startAddress;
	}
}
