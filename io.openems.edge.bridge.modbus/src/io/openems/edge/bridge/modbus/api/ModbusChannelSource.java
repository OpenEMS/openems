package io.openems.edge.bridge.modbus.api;

import java.util.Objects;

public class ModbusChannelSource {

	/**
	 * This can be used in {@link AbstractOpenemsModbusComponent} to ignore the
	 * check for duplicated source mappings.
	 */
	public static final IgnoreDuplicatedSource IGNORE_DUPLICATED_SOURCE = new IgnoreDuplicatedSource();

	protected static class IgnoreDuplicatedSource {
	}

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

	@Override
	public int hashCode() {
		return Objects.hash(this.address, this.bit);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ModbusChannelSource other = (ModbusChannelSource) obj;
		return this.address == other.address && this.bit == other.bit;
	}

}
