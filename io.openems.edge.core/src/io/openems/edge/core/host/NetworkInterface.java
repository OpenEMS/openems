package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ConfigurationProperty;
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

	/**
	 * Helper class for wrapping an IPv4 address together with its netmask.
	 */
	public static class Inet4AddressWithNetmask {
		private final Inet4Address inet4Address;
		private final int netmask;

		public Inet4AddressWithNetmask(Inet4Address inet4Address, int netmask) {
			this.inet4Address = inet4Address;
			this.netmask = netmask;
		}

		@Override
		public String toString() {
			return this.inet4Address.getHostAddress() + "/" + this.netmask;
		}

		/**
		 * Parse a string in the form "192.168.100.100/24" to an IPv4 address.
		 *
		 * @param value the string
		 * @return the new {@link Inet4AddressWithNetmask}
		 * @throws OpenemsException on error
		 */
		public static Inet4AddressWithNetmask fromString(String value) throws OpenemsException {
			var arr = value.split("/");
			try {
				return new Inet4AddressWithNetmask((Inet4Address) InetAddress.getByName(arr[0]),
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
			}
			if (obj == null) {
				return false;
			} else if (this.getClass() != obj.getClass()) {
				return false;
			} else {
				var other = (Inet4AddressWithNetmask) obj;
				return Objects.equals(this.toString(), other.toString());
			}
		}
	}

	/**
	 * Parses a JsonObject to a {@link java.net.NetworkInterface} object.
	 *
	 * @param name the name of the network interface, e.g. "eth0"
	 * @param j    the JsonObject
	 * @return the new {@link java.net.NetworkInterface}
	 * @throws OpenemsNamedException on error
	 */
	public static NetworkInterface<?> from(String name, JsonObject j) throws OpenemsNamedException {
		// Gateway
		ConfigurationProperty<Inet4Address> gateway;
		{
			ConfigurationProperty<String> gatewayString = ConfigurationProperty
					.fromJsonElement(JsonUtils.getOptionalSubElement(j, "gateway"), JsonUtils::getAsString);
			if (gatewayString.isSet()) {
				if (gatewayString.getValue() == null || gatewayString.getValue().trim().isEmpty()) {
					gateway = ConfigurationProperty.asNull();
				} else {
					gateway = ConfigurationProperty.fromJsonElement(
							Optional.of(new JsonPrimitive(gatewayString.getValue())), JsonUtils::getAsInet4Address);
				}
			} else {
				gateway = ConfigurationProperty.asNotSet();
			}
		}

		// DNS
		ConfigurationProperty<Inet4Address> dns;
		{
			ConfigurationProperty<String> dnsString = ConfigurationProperty
					.fromJsonElement(JsonUtils.getOptionalSubElement(j, "dns"), JsonUtils::getAsString);
			if (dnsString.isSet()) {
				if (dnsString.getValue() == null || dnsString.getValue().trim().isEmpty()) {
					dns = ConfigurationProperty.asNull();
				} else {
					dns = ConfigurationProperty.fromJsonElement(Optional.of(new JsonPrimitive(dnsString.getValue())),
							JsonUtils::getAsInet4Address);
				}
			} else {
				dns = ConfigurationProperty.asNotSet();
			}
		}

		// Addresses
		ConfigurationProperty<Set<Inet4AddressWithNetmask>> addresses;
		{
			ConfigurationProperty<JsonArray> addressesArray = ConfigurationProperty
					.fromJsonElement(JsonUtils.getOptionalSubElement(j, "addresses"), JsonUtils::getAsJsonArray);
			if (addressesArray.isSet()) {
				Set<Inet4AddressWithNetmask> value = new HashSet<>();
				for (JsonElement address : addressesArray.getValue()) {
					value.add(Inet4AddressWithNetmask.fromString(JsonUtils.getAsString(address)));
				}
				addresses = ConfigurationProperty.of(value);
			} else {
				addresses = ConfigurationProperty.asNotSet();
			}
		}

		ConfigurationProperty<Boolean> dhcp = ConfigurationProperty
				.fromJsonElement(JsonUtils.getOptionalSubElement(j, "dhcp"), JsonUtils::getAsBoolean);
		ConfigurationProperty<Boolean> linkLocalAddressing = ConfigurationProperty
				.fromJsonElement(JsonUtils.getOptionalSubElement(j, "linkLocalAddressing"), JsonUtils::getAsBoolean);

		return new NetworkInterface<Void>(name, dhcp, linkLocalAddressing, gateway, dns, addresses, null);
	}

	private final String name;
	private ConfigurationProperty<Boolean> dhcp;
	private ConfigurationProperty<Boolean> linkLocalAddressing;
	private ConfigurationProperty<Inet4Address> gateway;
	private ConfigurationProperty<Inet4Address> dns;
	private ConfigurationProperty<Set<Inet4AddressWithNetmask>> addresses;

	/**
	 * An arbitrary attachment to this NetworkInterface. Can be used to store e.g. a
	 * configuration file path.
	 */
	private final A attachment;

	public NetworkInterface(String name, ConfigurationProperty<Boolean> dhcp,
			ConfigurationProperty<Boolean> linkLocalAddressing, ConfigurationProperty<Inet4Address> gateway,
			ConfigurationProperty<Inet4Address> dns, ConfigurationProperty<Set<Inet4AddressWithNetmask>> addresses,
			A attachment) throws OpenemsException {
		this.name = name;
		this.dhcp = dhcp;
		this.linkLocalAddressing = linkLocalAddressing;
		this.gateway = gateway;
		this.dns = dns;
		this.addresses = addresses;
		this.attachment = attachment;
	}

	/**
	 * Gets the network interface name.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the network interface DHCP option.
	 *
	 * @return the DHCP option; true for enabled; false for disabled
	 */
	public ConfigurationProperty<Boolean> getDhcp() {
		return this.dhcp;
	}

	/**
	 * Gets the network interface LinkLocalAddressing option.
	 *
	 * @return the LinkLocalAddressing option; true for enabled; false for disabled
	 */
	public ConfigurationProperty<Boolean> getLinkLocalAddressing() {
		return this.linkLocalAddressing;
	}

	/**
	 * Gets the network interface Gateway.
	 *
	 * @return the Gateway
	 */
	public ConfigurationProperty<Inet4Address> getGateway() {
		return this.gateway;
	}

	/**
	 * Gets the network interface DNS server.
	 *
	 * @return the DNS server
	 */
	public ConfigurationProperty<Inet4Address> getDns() {
		return this.dns;
	}

	/**
	 * Gets the network interface addresses.
	 *
	 * @return the addresses
	 */
	public ConfigurationProperty<Set<Inet4AddressWithNetmask>> getAddresses() {
		return this.addresses;
	}

	/**
	 * Gets the network interface attachment.
	 *
	 * <p>
	 * An arbitrary attachment to this NetworkInterface. Can be used to store e.g. a
	 * configuration file path.
	 *
	 * @return the attachment
	 */
	public A getAttachment() {
		return this.attachment;
	}

	/**
	 * Gets the configured addresses including the default addresses if any.
	 *
	 * @return all addresses
	 */
	public ConfigurationProperty<Set<Inet4AddressWithNetmask>> getAddressesIncludingDefaults() {
		if (this.name.equals("eth0") || !this.addresses.isSet()) {
			Set<Inet4AddressWithNetmask> value = new HashSet<>(this.addresses.getValue());
			// add default eth0 network address
			value.add(DEFAULT_ETH0_ADDRESS);
			return ConfigurationProperty.of(value);
		}
		return this.addresses;
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
		var result = new JsonObject();
		if (this.dhcp.isSet()) {
			result.addProperty("dhcp", this.dhcp.getValue());
		}
		if (this.linkLocalAddressing.isSet()) {
			result.addProperty("linkLocalAddressing", this.linkLocalAddressing.getValue());
		}
		if (this.gateway.isSet()) {
			result.addProperty("gateway", this.gateway.getValue().getHostAddress());
		}
		if (this.dns.isSet()) {
			result.addProperty("dns", this.dns.getValue().getHostAddress());
		}
		if (this.getAddresses().isSet()) {
			var jAddresses = new JsonArray();
			for (Inet4AddressWithNetmask address : this.getAddresses().getValue()) {
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
		var isChanged = false;
		if (change.getDhcp().isSet()) {
			if (change.getDhcp() == null) {
				this.dhcp = ConfigurationProperty.asNotSet();
			} else {
				this.dhcp = change.getDhcp();
			}
			isChanged = true;
		}
		if (change.getLinkLocalAddressing().isSet()) {
			if (change.getLinkLocalAddressing() == null) {
				this.linkLocalAddressing = ConfigurationProperty.asNotSet();
			} else {
				this.linkLocalAddressing = change.getLinkLocalAddressing();
			}
			isChanged = true;
		}
		if (change.getGateway().isSet()) {
			if (change.getGateway() == null) {
				this.gateway = ConfigurationProperty.asNotSet();
			} else {
				this.gateway = change.getGateway();
			}
			isChanged = true;
		}
		if (change.getDns().isSet()) {
			if (change.getDns() == null) {
				this.dns = ConfigurationProperty.asNotSet();
			} else {
				this.dns = change.getDns();
			}
			isChanged = true;
		}
		if (change.getAddresses().isSet()) { // uses original addresses without additional default network addresses
			if (change.getAddresses() == null) {
				this.addresses = ConfigurationProperty.asNotSet();
			} else {
				this.addresses = change.getAddressesIncludingDefaults();
			}
			isChanged = true;
		}
		return isChanged;
	}

}
