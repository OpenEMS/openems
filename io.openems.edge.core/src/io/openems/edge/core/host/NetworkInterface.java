package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class NetworkInterface<A> {

	/**
	 * This default address is always added to eth0.
	 */
	private static final Inet4AddressWithNetmask DEFAULT_ETH0_ADDRESS;

	static {
		try {
			DEFAULT_ETH0_ADDRESS = Inet4AddressWithNetmask.fromString("192.168.100.100/24");
		} catch (OpenemsException e) {
			throw new RuntimeException("Failed to create DEFAULT_ETH0_ADDRESS instance in static block.", e);
		}
	}

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
			if (this == obj) {
				return true;
			} else if (obj == null) {
				return false;
			} else if (getClass() != obj.getClass()) {
				return false;
			} else {
				Inet4AddressWithNetmask other = (Inet4AddressWithNetmask) obj;
				return Objects.equals(this.toString(), other.toString());
			}
		}
	}

	public static NetworkInterface<?> from(String name, JsonObject j) throws OpenemsNamedException {
		Optional<Boolean> dhcp = JsonUtils.getAsOptionalBoolean(j, "dhcp");
		Optional<Boolean> linkLocalAddressing = JsonUtils.getAsOptionalBoolean(j, "linkLocalAddressing");
		Optional<Inet4Address> gateway = JsonUtils.getAsOptionalInet4Address(j, "gateway");
		Optional<Inet4Address> dns = JsonUtils.getAsOptionalInet4Address(j, "dns");
		Optional<JsonArray> addressesArray = JsonUtils.getAsOptionalJsonArray(j, "addresses");
		Optional<Set<Inet4AddressWithNetmask>> addressesOpt;
		if (addressesArray.isPresent()) {
			Set<Inet4AddressWithNetmask> addresses = new HashSet<>();
			for (JsonElement address : addressesArray.get()) {
				addresses.add(Inet4AddressWithNetmask.fromString(JsonUtils.getAsString(address)));
			}
			addressesOpt = Optional.of(addresses);
		} else {
			addressesOpt = Optional.empty();
		}
		return new NetworkInterface<Void>(name, dhcp, linkLocalAddressing, gateway, dns, addressesOpt, null);
	}

	private final String name;
	private Optional<Boolean> dhcp;
	private Optional<Boolean> linkLocalAddressing;
	private Optional<Inet4Address> gateway;
	private Optional<Inet4Address> dns;
	private Optional<Set<Inet4AddressWithNetmask>> addresses;
	private final A attachment;

	public NetworkInterface(String name, Optional<Boolean> dhcp, Optional<Boolean> linkLocalAddressing,
			Optional<Inet4Address> gateway, Optional<Inet4Address> dns,
			Optional<Set<Inet4AddressWithNetmask>> addresses, A attachment) throws OpenemsException {
		this.name = name;
		this.dhcp = dhcp;
		this.linkLocalAddressing = linkLocalAddressing;
		this.gateway = gateway;
		this.dns = dns;
		this.addresses = addresses;
		this.attachment = attachment;
	}

	public String getName() {
		return this.name;
	}

	public Optional<Boolean> getDhcp() {
		return this.dhcp;
	}

	public Optional<Boolean> getLinkLocalAddressing() {
		return this.linkLocalAddressing;
	}

	public Optional<Inet4Address> getGateway() {
		return this.gateway;
	}

	public Optional<Inet4Address> getDns() {
		return this.dns;
	}

	public Optional<Set<Inet4AddressWithNetmask>> getAddresses() {
		if (this.name.equals("eth0")) {
			Set<Inet4AddressWithNetmask> result;
			if (this.addresses.isPresent()) {
				result = new HashSet<>(this.addresses.get());
			} else {
				result = new HashSet<>();
			}
			// add default eth0 network address
			result.add(DEFAULT_ETH0_ADDRESS);
			return Optional.of(result);
		} else {
			return this.addresses;
		}
	}

	/**
	 * Exports this NetworkInterface configuration as JSON.
	 * 
	 * <pre>
	 * {
	 *   "dhcp": boolean,
	 *   "linkLocalAddressing": boolean,
	 *   "gateway": string,
	 *   "dns": string,
	 *   "addresses": string[]
	 * }
	 * </pre>
	 * 
	 * @return configuration as JSON
	 */
	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		if (this.dhcp.isPresent()) {
			result.addProperty("dhcp", this.dhcp.get());
		}
		if (this.linkLocalAddressing.isPresent()) {
			result.addProperty("linkLocalAddressing", this.linkLocalAddressing.get());
		}
		if (this.gateway.isPresent()) {
			result.addProperty("gateway", this.gateway.get().getHostAddress());
		}
		if (this.dns.isPresent()) {
			result.addProperty("dns", this.dns.get().getHostAddress());
		}
		if (this.getAddresses().isPresent()) {
			JsonArray jAddresses = new JsonArray();
			for (Inet4AddressWithNetmask address : this.getAddresses().get()) {
				jAddresses.add(address.toString());
			}
			result.add("addresses", jAddresses);
		}

		return result;
	}

	/**
	 * Updates the interface from a NetworkInterfaceChange object.
	 * 
	 * @param change the object containing the changes
	 * @return true if values changed
	 */
	public boolean updateFrom(NetworkInterface<?> change) {
		boolean isChanged = false;
		if (change.getDhcp().isPresent()) {
			this.dhcp = change.getDhcp();
			isChanged = true;
		}
		if (change.getLinkLocalAddressing().isPresent()) {
			this.linkLocalAddressing = change.getLinkLocalAddressing();
			isChanged = true;
		}
		if (change.getGateway().isPresent()) {
			this.gateway = change.getGateway();
			isChanged = true;
		}
		if (change.getDns().isPresent()) {
			this.dns = change.getDns();
			isChanged = true;
		}
		if (change.addresses.isPresent()) { // uses original addresses without additional default network addresses
			this.addresses = change.getAddresses();
			isChanged = true;
		}
		return isChanged;
	}

	public A getAttachment() {
		return this.attachment;
	}
}
