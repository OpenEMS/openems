package io.openems.edge.bridge.modbus.api;

import java.util.Objects;

/**
 * Describes a Channel that has a read-mapping to a Modbus Coil.
 */
public class ChannelMetaInfoBit extends ChannelMetaInfo {

	/**
	 * Holds the index of the bit inside the Register if applicable.
	 */
	private final int bit;

	public ChannelMetaInfoBit(int address, int bit) {
		super(address);
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
		ChannelMetaInfoBit other = (ChannelMetaInfoBit) obj;
		return this.address == other.address && this.bit == other.bit;
	}

}
