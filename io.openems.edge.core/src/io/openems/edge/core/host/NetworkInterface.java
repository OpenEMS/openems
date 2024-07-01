package io.openems.edge.core.host;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsInet4Address;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getOptionalSubElement;

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
		var gateway = parseInet4Address(j, "gateway");
		var metric = parseInteger(j, "metric");
		var dns = parseInet4Address(j, "dns");
		var addresses = parseAddresses(j);
		var dhcp = parseBoolean(j, "dhcp");
		var linkLocalAddressing = parseBoolean(j, "linkLocalAddressing");

		return new NetworkInterface<Void>(name, dhcp, linkLocalAddressing, gateway, dns, addresses, metric, null);
	}

	private static ConfigurationProperty<Inet4Address> parseInet4Address(JsonObject j, String member)
			throws OpenemsNamedException {
		ConfigurationProperty<String> gatewayString = ConfigurationProperty
				.fromJsonElement(getOptionalSubElement(j, member), JsonUtils::getAsString);
		if (gatewayString.isSet()) {
			if (gatewayString.getValue() == null || gatewayString.getValue().trim().isEmpty()) {
				return ConfigurationProperty.asNull();
			} else {
				return ConfigurationProperty.fromJsonElement(//
						Optional.of(new JsonPrimitive(gatewayString.getValue())), JsonUtils::getAsInet4Address);
			}
		} else {
			return ConfigurationProperty.asNotSet();
		}
	}

	private static ConfigurationProperty<Integer> parseInteger(JsonObject j, String member)
			throws OpenemsNamedException {
		ConfigurationProperty<Integer> metricElement = ConfigurationProperty
				.fromJsonElement(getOptionalSubElement(j, member), JsonUtils::getAsInt);
		if (metricElement.isSet()) {
			if (metricElement.getValue() == null) {
				return ConfigurationProperty.asNull();
			} else {
				return ConfigurationProperty.fromJsonElement(//
						Optional.of(new JsonPrimitive(metricElement.getValue())), JsonUtils::getAsInt);
			}
		} else {
			return ConfigurationProperty.asNotSet();
		}
	}

	private static ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> parseAddresses(JsonObject j)
			throws OpenemsNamedException {
		ConfigurationProperty<JsonArray> addressesArray = ConfigurationProperty
				.fromJsonElement(getOptionalSubElement(j, "addresses"), JsonUtils::getAsJsonArray);
		if (addressesArray.isSet()) {
			var value = new HashSet<Inet4AddressWithSubnetmask>();
			for (JsonElement element : addressesArray.getValue()) {
				var label = getAsString(element, "label");
				var address = getAsInet4Address(element, "address");
				var subnetmask = getAsInet4Address(element, "subnetmask");
				var cidr = Inet4AddressWithSubnetmask.getCidrFromSubnetmask(subnetmask);
				value.add(new Inet4AddressWithSubnetmask(label, address, cidr));
			}
			return ConfigurationProperty.of(value);
		} else {
			return ConfigurationProperty.asNotSet();
		}
	}

	private static ConfigurationProperty<Boolean> parseBoolean(JsonObject j, String member)
			throws OpenemsNamedException {
		return ConfigurationProperty.fromJsonElement(getOptionalSubElement(j, member), JsonUtils::getAsBoolean);
	}

	private final String name;
	private ConfigurationProperty<Boolean> dhcp;
	private ConfigurationProperty<Boolean> linkLocalAddressing;
	private ConfigurationProperty<Inet4Address> gateway;
	private ConfigurationProperty<Inet4Address> dns;
	private ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> addresses;
	private ConfigurationProperty<Integer> metric;

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
			ConfigurationProperty<Integer> metric, //
			A attachment) throws OpenemsException {
		this.name = name;
		this.dhcp = dhcp;
		this.linkLocalAddressing = linkLocalAddressing;
		this.gateway = gateway;
		this.dns = dns;
		this.attachment = attachment;
		this.addresses = addresses;
		this.metric = metric;
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
	 * Gets the network interface metric.
	 *
	 * @return the Metric
	 */
	public ConfigurationProperty<Integer> getMetric() {
		return this.metric;
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
	 *   "metric": number,
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
		var result = buildJsonObject() //
				.onlyIf(this.dhcp.isSet(), //
						b -> b.addProperty("dhcp", this.dhcp.getValue()))
				.onlyIf(this.linkLocalAddressing.isSet(), //
						b -> b.addProperty("linkLocalAddressing", this.linkLocalAddressing.getValue()))
				.onlyIf(!this.gateway.isNull(), //
						b -> b.addProperty("gateway", this.gateway.getValue().getHostAddress()))
				.onlyIf(!this.metric.isNull(), //
						b -> b.addProperty("metric", this.metric.getValue().intValue()))
				.onlyIf(!this.dns.isNull(), //
						b -> b.addProperty("dns", this.dns.getValue().getHostAddress()))
				.onlyIf(this.addresses.isSet(), //
						b -> {
							var arr = buildJsonArray();
							for (var address : this.addresses.getValue()) {
								arr.add(buildJsonObject() //
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
			this.dhcp = change.getDhcp();
			isChanged = true;
		}
		if (change.getLinkLocalAddressing().isSet()) {
			this.linkLocalAddressing = change.getLinkLocalAddressing();
			isChanged = true;
		}
		if (change.getGateway().isSet()) {
			this.gateway = change.getGateway();
			isChanged = true;
		}
		if (change.getMetric().isSet()) {
			this.metric = change.getMetric();
			isChanged = true;
		}
		if (change.getDns().isSet()) {
			this.dns = change.getDns();
			isChanged = true;
		}
		if (change.getAddresses().isSet()) {
			this.addresses = change.getAddressesIncludingDefaults();
			isChanged = true;
		}
		return isChanged;
	}

}
