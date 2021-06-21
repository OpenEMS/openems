package io.openems.edge.bridge.modbus.api;

public class ModbusChannelSource {

	/**
	 * Holds the Start-Address of the Modbus Register.
	 */
	private final int address;

	/**
	 * Holds the index of the bit inside the Register if applicable.
	 */
	private final int bit;

	public ModbusChannelSource(int address) {
		this.address = address;
		this.bit = -1;
	}

	public ModbusChannelSource(int address, int bit) {
		this.address = address;
		this.bit = bit;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("0x").append(Integer.toHexString(this.address));
		if (this.bit >= 0) {
			b.append("|bit").append(String.format("%02d", this.bit));
		}
		return b.toString();
	}

}
