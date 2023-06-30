package io.openems.edge.core.host;

import java.net.Inet4Address;
import java.util.HashSet;
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
	public static final Inet4AddressWithSubnetmask DEFAULT_ETH0_ADDRESS;
	private static final String DEFAULT_ETH0_LABEL = "fallback";
	private static final String DEFAULT_ETH0_IP_NETMASK = "192.168.100.100/24";

	static {
		try {
			DEFAULT_ETH0_ADDRESS = Inet4AddressWithSubnetmask.fromString(DEFAULT_ETH0_LABEL, DEFAULT_ETH0_IP_NETMASK);
		} catch (OpenemsException e) {
			throw new RuntimeException("Failed to create DEFAULT_ETH0_ADDRESS instance in static block.", e);
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
		ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> addresses;
		{
			ConfigurationProperty<JsonArray> addressesArray = ConfigurationProperty
					.fromJsonElement(JsonUtils.getOptionalSubElement(j, "addresses"), JsonUtils::getAsJsonArray);
			if (addressesArray.isSet()) {
				var value = new HashSet<Inet4AddressWithSubnetmask>();
				for (JsonElement element : addressesArray.getValue()) {
					var label = JsonUtils.getAsString(element, "label");
					var address = JsonUtils.getAsInet4Address(element, "address");
					var subnetmask = JsonUtils.getAsInet4Address(element, "subnetmask");
					var cidr = Inet4AddressWithSubnetmask.getCidrFromSubnetmask(subnetmask);
					value.add(new Inet4AddressWithSubnetmask(label, address, cidr));
				}
				addresses = ConfigurationProperty.of(value);
			} else {
				addresses = ConfigurationProperty.asNotSet();
			}
		}

		// DHCP
		ConfigurationProperty<Boolean> dhcp = ConfigurationProperty
				.fromJsonElement(JsonUtils.getOptionalSubElement(j, "dhcp"), JsonUtils::getAsBoolean);

		// linkLocalAddressing
		ConfigurationProperty<Boolean> linkLocalAddressing = ConfigurationProperty
				.fromJsonElement(JsonUtils.getOptionalSubElement(j, "linkLocalAddressing"), JsonUtils::getAsBoolean);

		return new NetworkInterface<Void>(name, dhcp, linkLocalAddressing, gateway, dns, addresses, null);
	}

	private final String name;
	private ConfigurationProperty<Boolean> dhcp;
	private ConfigurationProperty<Boolean> linkLocalAddressing;
	private ConfigurationProperty<Inet4Address> gateway;
	private ConfigurationProperty<Inet4Address> dns;
	private ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> addresses;

	/**
	 * An arbitrary attachment to this NetworkInterface. Can be used to store e.g. a
	 * configuration file path.
	 */
	private final A attachment;

	public NetworkInterface(String name, //
			ConfigurationProperty<Boolean> dhcp, //
			ConfigurationProperty<Boolean> linkLocalAddressing, //
			ConfigurationProperty<Inet4Address> gateway, //
			ConfigurationProperty<Inet4Address> dns, //
			ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> addresses, //
			A attachment) throws OpenemsException {
		this.name = name;
		this.dhcp = dhcp;
		this.linkLocalAddressing = linkLocalAddressing;
		this.gateway = gateway;
		this.dns = dns;
		this.attachment = attachment;
		this.addresses = addresses;
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
	 * Gets the addresses configured in the network.
	 * 
	 * @return the addresses mapped with label.
	 */
	public ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> getAddresses() {
		return this.addresses;
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
	public ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> getAddressesIncludingDefaults() {
		if (this.name.equals("eth0") || !this.getAddresses().isSet()) {
			var value = new HashSet<Inet4AddressWithSubnetmask>(this.getAddresses().getValue());
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
	 *   "addresses": [{ 
	 *     "label": string, 
	 *     "address": string, 
	 *     "subnetmask": string 
	 *   }]
	 * }
	 * </pre>
	 *
	 * @return configuration as JSON
	 * @throws OpenemsNamedException on error.
	 */
	public JsonObject toJson() {
		var result = JsonUtils.buildJsonObject() //
				.onlyIf(this.dhcp.isSet(), //
						b -> b.addProperty("dhcp", this.dhcp.getValue()))
				.onlyIf(this.linkLocalAddressing.isSet(), //
						b -> b.addProperty("linkLocalAddressing", this.linkLocalAddressing.getValue()))
				.onlyIf(!this.gateway.isNull(), //
						b -> b.addProperty("gateway", this.gateway.getValue().getHostAddress()))
				.onlyIf(!this.dns.isNull(), //
						b -> b.addProperty("dns", this.dns.getValue().getHostAddress()))
				.onlyIf(this.addresses.isSet(), //
						b -> {
							var arr = JsonUtils.buildJsonArray();
							for (var address : this.addresses.getValue()) {
								arr.add(JsonUtils.buildJsonObject() //
										.addProperty("label", address.getLabel())
										.addProperty("address", address.getInet4Address().getHostAddress())
										.addProperty("subnetmask", address.getSubnetmaskAsString()).build());
							}
							b.add("addresses", arr.build());
						});
		return result.build();
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
		if (change.getAddresses().isSet()) {
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
