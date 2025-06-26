package io.openems.edge.bridge.modbus.api;

import java.util.Objects;

/**
 * Describes a Channel that has a read- or read-and-write-mapping to one Modbus
 * Register.
 */
public class ChannelMetaInfo {

	/**
	 * Holds the Address for Modbus Read Register.
	 */
	protected final int address;

	public ChannelMetaInfo(int address) {
		this.address = address;
	}

	@Override
	public String toString() {
		var b = new StringBuilder();
		b.append("0x").append(Integer.toHexString(this.address));
		return b.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.address);
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
		var other = (ChannelMetaInfo) obj;
		return this.address == other.address;
	}

}
