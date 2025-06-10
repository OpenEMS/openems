package io.openems.edge.bridge.modbus.api;

import java.util.Objects;

/**
 * Describes a Channel that has a read-and-write-mapping to two Modbus
 * Registers.
 */
public class ChannelMetaInfoReadAndWrite extends ChannelMetaInfo {

	/**
	 * Holds the Address for Modbus Write Register.
	 */
	private final int writeAddress;

	public ChannelMetaInfoReadAndWrite(int readAddress, int writeAaddress) {
		super(readAddress);
		this.writeAddress = writeAaddress;
	}

	@Override
	public String toString() {
		var b = new StringBuilder();
		b.append("READ:0x").append(Integer.toHexString(this.address));
		b.append(" | WRITE:0x").append(Integer.toHexString(this.writeAddress));
		return b.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.address, this.writeAddress);
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
		var other = (ChannelMetaInfoReadAndWrite) obj;
		return this.address == other.address && this.writeAddress == other.writeAddress;
	}

}
