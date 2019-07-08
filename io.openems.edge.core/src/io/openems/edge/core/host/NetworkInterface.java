package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

public class NetworkInterface {

	public static class Inet4AddressWithNetmask {
		private final Inet4Address inet4Address;
		private final int netmask;

		public Inet4AddressWithNetmask(Inet4Address inet4Address, int netmask) {
			super();
			this.inet4Address = inet4Address;
			this.netmask = netmask;
		}

		@Override
		public String toString() {
			return this.inet4Address.getHostAddress() + "/" + netmask;
		}

		public static Inet4AddressWithNetmask fromString(String value) throws OpenemsException {
			String[] arr = value.split("/");
			try {
				return new Inet4AddressWithNetmask((Inet4Address) Inet4Address.getByName(arr[0]),
						Integer.parseInt(arr[1]));
			} catch (NumberFormatException | UnknownHostException | IndexOutOfBoundsException e) {
				throw new OpenemsException(
						"Unable to parse Inet4Address with netmask from [" + value + "]: " + e.getMessage());
			}
		}

		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Inet4AddressWithNetmask other = (Inet4AddressWithNetmask) obj;
			return Objects.equals(this.toString(), other.toString());
		}
	}

	private final String name;

	private Boolean dhcp;
	private Boolean linkLocalAddressing;
	private Inet4Address gateway;
	private Inet4Address dns;
	private Set<Inet4AddressWithNetmask> addresses;

	public NetworkInterface(String name, Boolean dhcp, Boolean linkLocalAddressing, Inet4Address gateway,
			Inet4Address dns, Set<Inet4AddressWithNetmask> addresses) throws OpenemsException {
		this.name = name;
		this.dhcp = dhcp;
		this.linkLocalAddressing = linkLocalAddressing;
		this.gateway = gateway;
		this.dns = dns;
		this.addresses = addresses;
	}

	public String getName() {
		return name;
	}

	/**
	 * <pre>
	 * {
	 *   "dhcp": boolean,
	 *   "linkLocalAddressing": boolean,
	 *   "gateway": string,
	 *   "dns": string,
	 *   "addresses": string[]
	 * }
	 * </pre>
	 */
	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		if (this.dhcp == null) {
			result.add("dhcp", JsonNull.INSTANCE);
		} else {
			result.addProperty("dhcp", this.dhcp.booleanValue());
		}
		if (this.linkLocalAddressing == null) {
			result.add("linkLocalAddressing", JsonNull.INSTANCE);
		} else {
			result.addProperty("linkLocalAddressing", this.linkLocalAddressing.booleanValue());
		}
		if (this.gateway == null) {
			result.add("gateway", JsonNull.INSTANCE);
		} else {
			result.addProperty("gateway", this.gateway.getHostAddress());
		}
		if (this.dns == null) {
			result.add("dns", JsonNull.INSTANCE);
		} else {
			result.addProperty("dns", this.gateway.getHostAddress());
		}
		JsonArray addresses = new JsonArray();
		for (Inet4AddressWithNetmask address : this.addresses) {
			addresses.add(address.toString());
		}
		result.add("addresses", addresses);
		return result;
	}

}
