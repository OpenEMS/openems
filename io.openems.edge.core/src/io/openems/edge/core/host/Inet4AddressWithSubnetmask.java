package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import io.openems.common.exceptions.OpenemsException;

/**
 * Helper class for wrapping an IPv4 address together with its subnetmask in
 * CIDR format.
 */
public class Inet4AddressWithSubnetmask {

	/**
	 * Parse a string in the form "192.168.100.100/24" to an IPv4 address. Label is
	 * set to an empty string with this factory method.
	 *
	 * @param value the string in the form "192.168.100.100/24"
	 * @return the new {@link Inet4AddressWithSubnetmask}
	 * @throws OpenemsException on error
	 */
	public static Inet4AddressWithSubnetmask fromString(String value) throws OpenemsException {
		return fromString("", value);
	}

	/**
	 * Parse a string in the form "192.168.100.100/24" to an IPv4 address.
	 *
	 * @param label a label string
	 * @param value the string in the form "192.168.100.100/24"
	 * @return the new {@link Inet4AddressWithSubnetmask}
	 * @throws OpenemsException on error
	 */
	public static Inet4AddressWithSubnetmask fromString(String label, String value) throws OpenemsException {
		var arr = value.split("/");
		try {
			return new Inet4AddressWithSubnetmask(label, (Inet4Address) InetAddress.getByName(arr[0]),
					Integer.parseInt(arr[1]));
		} catch (NumberFormatException | UnknownHostException | IndexOutOfBoundsException e) {
			throw new OpenemsException(
					"Unable to parse Inet4Address with netmask from [" + value + "]: " + e.getMessage());
		}
	}

	/**
	 * Converts the Subnetmask to a CIDR number.
	 * 
	 * <p>
	 * e. g. Converts "255.255.255.0" to "24".
	 * </p>
	 * 
	 * @param subnetmask the subnetmask.
	 * @return the CIDR number.
	 */
	public static int getCidrFromSubnetmask(Inet4Address subnetmask) throws OpenemsException {
		byte[] netmaskBytes = subnetmask.getAddress();
		int cidr = 0;
		boolean zero = false;
		for (byte b : netmaskBytes) {
			int mask = 0x80;

			for (int i = 0; i < 8; i++) {
				int result = b & mask;
				if (result == 0) {
					zero = true;
				} else if (zero) {
					throw new OpenemsException("Unable to parse subnetmask [" + subnetmask + "]");
				} else {
					cidr++;
				}
				mask >>>= 1;
			}
		}
		return cidr;
	}

	private final String label;
	private final Inet4Address inet4Address;
	private final int subnetmask;

	public Inet4AddressWithSubnetmask(String label, Inet4Address inet4Address, int subnetmask) {
		this.label = label;
		this.inet4Address = inet4Address;
		this.subnetmask = subnetmask;
	}

	public String getLabel() {
		return this.label;
	}

	public Inet4Address getInet4Address() {
		return this.inet4Address;
	}

	/**
	 * Gets the Subnetmask in CIDR format, e.g. "24" for "255.255.255.0".
	 * 
	 * @return the subnetmask
	 */
	public int getSubnetmaskAsCidr() {
		return this.subnetmask;
	}

	/**
	 * Gets the Subnetmask in string format, e.g. "255.255.255.0" for "24".
	 * 
	 * @return the subnetmask
	 */
	public final String getSubnetmaskAsString() {
		var netmaskString = new StringBuilder();

		for (var i = 0; i < 4; i++) {
			var blockSize = Math.min(8, this.subnetmask - 8 * i);
			var number = 255;
			if (blockSize != 8 && blockSize > 0) {
				if (blockSize == 1) {
					number -= 1;
				}
				for (var j = 0; j < 8 - blockSize; j++) {
					number -= Math.pow(2, j);
				}
			} else if (blockSize <= 0) {
				number = 0;
			}

			netmaskString.append(Integer.toString(number));
			if (i != 3) {
				netmaskString.append(".");
			}
		}

		return netmaskString.toString();
	}

	@Override
	public String toString() {
		return this.inet4Address.getHostAddress() + "/" + this.subnetmask;
	}

	/**
	 * Determines if this address and the given address are in the same network.
	 *
	 * @param other the other {@link Inet4AddressWithSubnetmask}
	 * @return true if they are in the same network
	 */
	public boolean isInSameNetwork(Inet4AddressWithSubnetmask other) {
		var ipBytesFirst = this.inet4Address.getAddress();
		var ipBytesSecond = other.inet4Address.getAddress();
		byte[] maskBytes;
		try {
			maskBytes = InetAddress.getByName(this.getSubnetmaskAsString()).getAddress();
		} catch (UnknownHostException e) {
			return false;
		}

		for (var i = 0; i < ipBytesFirst.length; i++) {
			if ((ipBytesFirst[i] & maskBytes[i]) != (ipBytesSecond[i] & maskBytes[i])) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
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
		} else {
			var other = (Inet4AddressWithSubnetmask) obj;
			return Objects.equals(this.toString(), other.toString());
		}
	}
}