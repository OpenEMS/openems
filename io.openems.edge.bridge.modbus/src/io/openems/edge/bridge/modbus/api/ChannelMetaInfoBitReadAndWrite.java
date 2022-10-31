package io.openems.edge.bridge.modbus.api;

import java.util.Objects;

/**
 * Describes a Channel that has a read-and-write-mapping to two Modbus Bits.
 */
public class ChannelMetaInfoBitReadAndWrite extends ChannelMetaInfoBit {

	/**
	 * Holds the Address for Modbus Write Register.
	 */
	private final int writeAddress;
	private final int writeBit;

	public ChannelMetaInfoBitReadAndWrite(int readAddress, int readBit, int writeAaddress, int writeBit) {
		super(readAddress, readBit);
		this.writeAddress = writeAaddress;
		this.writeBit = writeBit;
	}

	@Override
	public String toString() {
		var b = new StringBuilder();
		b.append("READ:0x").append(Integer.toHexString(this.address));
		if (this.bit >= 0) {
			b.append("|bit").append(String.format("%02d", this.bit));
		}

		b.append(" | WRITE:0x").append(Integer.toHexString(this.writeAddress));
		if (this.writeBit >= 0) {
			b.append("|bit").append(String.format("%02d", this.writeBit));
		}
		return b.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.address, this.bit, this.writeAddress, this.writeBit);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		var other = (ChannelMetaInfoBitReadAndWrite) obj;
		return this.address == other.address && this.bit == other.bit && this.writeAddress == other.writeAddress
				&& this.writeBit == other.writeBit;
	}

}
