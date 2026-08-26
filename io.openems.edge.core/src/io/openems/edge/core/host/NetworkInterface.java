package io.openems.edge.core.host;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.net.Inet4Address;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ConfigurationProperty;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a configurable network interface on the Edge device.
 * 
 * <p>
 * This class provides access to network configuration parameters such as DHCP,
 * static IP addresses, gateway, DNS, routing, IPv4 forwarding, and IP
 * masquerading. It also supports JSON serialization and de-serialization for
 * integration with OpenEMS configuration systems.
 * </p>
 *
 * @param <A> an optional attachment type, e.g. a configuration file path or
 *            additional metadata.
 */
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
	 * Returns a {@link JsonSerializer} for a {@link NetworkInterface}.
	 * 
	 * @param name the name of the interface
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<NetworkInterface<?>> serializer(String name) {
		return jsonObjectSerializer(json -> {
			return new NetworkInterface<Void>(//
					name, //
					json.getOptionalBoolean("dhcp") //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalBoolean("linkLocalAddressing") //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalStringParsed("gateway", new Inet4AddressWithSubnetmask.StringParserInet4Address()) //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalStringParsed("dns", new Inet4AddressWithSubnetmask.StringParserInet4Address()) //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalSet("addresses", Inet4AddressWithSubnetmask.serializer())
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalInt("dhpcRouteMetric") //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalBoolean("ipv4Forwarding") //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalEnum("ipMasquerade", IpMasqueradeSetting.class) //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalSet("destination", Inet4AddressWithSubnetmask.serializer())
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalBoolean("gatewayOnLink") //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					json.getOptionalSet("routes", Routes.serializer()) //
							.map(ConfigurationProperty::of) //
							.orElseGet(ConfigurationProperty::asNotSet), //
					null);
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.onlyIf(obj.getDhcp().isSetAndNotNull(), t -> {
						t.addProperty("dhcp", obj.getDhcp().getValue());
					}) //
					.onlyIf(obj.getLinkLocalAddressing().isSetAndNotNull(), t -> {
						t.addProperty("linkLocalAddressing", obj.getLinkLocalAddressing().getValue());
					}) //
					.onlyIf(obj.getGateway().isSetAndNotNull(), t -> {
						t.addProperty("gateway", obj.getGateway().getValue().getHostAddress());
					}) //
					.onlyIf(!obj.getDhcpRouteMetric().isNull(), t -> {
						t.addProperty("dhpcRouteMetric", obj.getDhcpRouteMetric().getValue());
					}) //
					.onlyIf(!obj.getDns().isNull(), t -> {
						t.addProperty("dns", obj.getDns().getValue().getHostAddress());
					}) //
					.onlyIf(obj.getAddresses().isSet(), t -> {
						t.add("addresses", Inet4AddressWithSubnetmask.serializer().toSetSerializer()
								.serialize(obj.getAddresses().getValue()));
					}) //
					.onlyIf(obj.getIpv4Forwarding().isSetAndNotNull(), t -> {
						t.addProperty("ipv4Forwarding", obj.getIpv4Forwarding().getValue());
					}) //
					.onlyIf(obj.getIpMasquerade().isSetAndNotNull(), t -> {
						t.addProperty("ipMasquerade", obj.getIpMasquerade().getValue());
					}) //
					.onlyIf(obj.getDestination().isSetAndNotNull(),
							t -> t.add("destination",
									Inet4AddressWithSubnetmask.serializer().toSetSerializer()
											.serialize(obj.getDestination().getValue()))) //
					.onlyIf(obj.getGatewayOnLink().isSetAndNotNull(),
							t -> t.addProperty("gatewayOnLink", obj.getGatewayOnLink().getValue())) //
					.onlyIf(obj.getRoutes().isSet(), t -> { //
						t.add("routes", Routes.serializer().toSetSerializer().serialize(obj.getRoutes().getValue()));
					}) //
					.build();
		});
	}

	/**
	 * Parses a {@link JsonElement} into a {@link NetworkInterface} instance.
	 *
	 * @param name the name of the network interface (e.g. "eth0")
	 * @param j    the JSON object containing configuration
	 * @return a new {@link NetworkInterface} instance
	 * @throws OpenemsNamedException if parsing fails
	 */
	public static NetworkInterface<?> from(String name, JsonElement j) throws OpenemsNamedException {
		return serializer(name).deserialize(j);
	}

	private final String name;
	private ConfigurationProperty<Boolean> dhcp;
	private ConfigurationProperty<Boolean> linkLocalAddressing;
	private ConfigurationProperty<Inet4Address> gateway;
	private ConfigurationProperty<Inet4Address> dns;
	private ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> addresses;
	private ConfigurationProperty<Integer> dhcpRouteMetric;
	private ConfigurationProperty<Boolean> ipv4Forwarding;
	private ConfigurationProperty<IpMasqueradeSetting> ipMasquerade;
	private ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> destination;
	private ConfigurationProperty<Boolean> gatewayOnLink;
	private ConfigurationProperty<Set<Routes>> routes;

	public enum IpMasqueradeSetting {
		NO("no"), //
		IP_V4("ipv4"), //
		IP_V6("ipv6"), //
		BOTH("both"), //
		;

		public final String settingValue;

		private IpMasqueradeSetting(String settingValue) {
			this.settingValue = settingValue;
		}

		/**
		 * Finds the {@link IpMasqueradeSetting} where the
		 * {@link IpMasqueradeSetting#settingValue} matches the provided settings value.
		 * 
		 * @param settingValue the setting value to match
		 * @return the found {@link IpMasqueradeSetting} or null
		 */
		public static IpMasqueradeSetting findBySettingValue(String settingValue) {
			return Stream.of(IpMasqueradeSetting.values()) //
					.filter(t -> t.settingValue.equals(settingValue)) //
					.findAny().orElse(null);
		}

	}

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
			ConfigurationProperty<Integer> dhpcRouteMetric, //
			ConfigurationProperty<Boolean> ipv4Forwarding, //
			ConfigurationProperty<IpMasqueradeSetting> ipMasquerade, //
			ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> destination, //
			ConfigurationProperty<Boolean> gatewayOnLink, //
			ConfigurationProperty<Set<Routes>> routes, A attachment //
	) {
		this.name = name;
		this.dhcp = dhcp;
		this.linkLocalAddressing = linkLocalAddressing;
		this.gateway = gateway;
		this.dns = dns;
		this.attachment = attachment;
		this.addresses = addresses;
		this.dhcpRouteMetric = dhpcRouteMetric;
		this.ipv4Forwarding = ipv4Forwarding;
		this.ipMasquerade = ipMasquerade;
		this.destination = destination;
		this.gatewayOnLink = gatewayOnLink;
		this.routes = routes != null ? routes : ConfigurationProperty.asNotSet();
	}

	public ConfigurationProperty<Set<Routes>> getRoutes() {
		return this.routes;
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
	 * Gets the network interface ipv4 forwarding option.
	 * 
	 * @return the ipv4 forwarding option; true for enabled; false for disabled
	 */
	public ConfigurationProperty<Boolean> getIpv4Forwarding() {
		return this.ipv4Forwarding;
	}

	/**
	 * Gets the network interface ip masquerade option.
	 * 
	 * @return the ip masquerade option
	 * @see IpMasqueradeSetting
	 */
	public ConfigurationProperty<IpMasqueradeSetting> getIpMasquerade() {
		return this.ipMasquerade;
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
	 * Gets the network interface dhcp RouteMetric.
	 *
	 * @return the Metric
	 */
	public ConfigurationProperty<Integer> getDhcpRouteMetric() {
		return this.dhcpRouteMetric;
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
	 * Sets the IP v4 forwarding option.
	 * 
	 * @param ipv4Forwarding the option to set
	 */
	public void setIpv4Forwarding(ConfigurationProperty<Boolean> ipv4Forwarding) {
		this.ipv4Forwarding = ipv4Forwarding;
	}

	/**
	 * Sets the ip masquerade option.
	 * 
	 * @param ipMasquerade the option to set
	 */
	public void setIpMasquerade(ConfigurationProperty<IpMasqueradeSetting> ipMasquerade) {
		this.ipMasquerade = ipMasquerade;
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
	 * Gets the destination network configuration.
	 * 
	 * @return the destination property
	 */
	public ConfigurationProperty<Set<Inet4AddressWithSubnetmask>> getDestination() {
		return this.destination;
	}

	/**
	 * Gets whether the gateway is directly reachable.
	 * 
	 * @return the gatewayOnLink property
	 */
	public ConfigurationProperty<Boolean> getGatewayOnLink() {
		return this.gatewayOnLink;
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
	 * Sets the routes.
	 * 
	 * @param routes the routes to set
	 */
	public void setRoutes(ConfigurationProperty<Set<Routes>> routes) {
		this.routes = routes;
	}

	public void setDhcp(ConfigurationProperty<Boolean> dhcp) {
		this.dhcp = dhcp;
	}

	public void setDns(ConfigurationProperty<Inet4Address> dns) {
		this.dns = dns;
	}

	public void setDhcpRouteMetric(ConfigurationProperty<Integer> dhcpRouteMetric) {
		this.dhcpRouteMetric = dhcpRouteMetric;
	}

	/**
	 * Sets the gateway configuration.
	 *
	 * @param gateway the gateway property
	 */
	public void setGateway(ConfigurationProperty<Inet4Address> gateway) {
		this.gateway = gateway;
	}

	/**
	 * Sets whether the gateway is directly reachable.
	 *
	 * @param gatewayOnLink the gatewayOnLink property
	 */
	public void setGatewayOnLink(ConfigurationProperty<Boolean> gatewayOnLink) {
		this.gatewayOnLink = gatewayOnLink;
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
	public JsonElement toJson() {
		return serializer(this.name).serialize(this);
	}

	/**
	 * Updates the interface from a NetworkInterfaceChange object.
	 *
	 * @param change the object containing the changes
	 * @return true if values changed
	 */
	public boolean updateFrom(NetworkInterface<?> change) {
		var isChanged = false;
		if (change.getRoutes().isSet() && !change.getRoutes().equals(this.routes)) {
			this.routes = change.getRoutes();
			isChanged = true;
		}
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
		if (change.getDhcpRouteMetric().isSet()) {
			this.dhcpRouteMetric = change.getDhcpRouteMetric();
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
		if (!change.getIpv4Forwarding().equals(this.ipv4Forwarding)) {
			this.ipv4Forwarding = change.getIpv4Forwarding();
			isChanged = true;
		}
		if (!change.getIpMasquerade().equals(this.ipMasquerade)) {
			this.ipMasquerade = change.getIpMasquerade();
			isChanged = true;
		}
		if (!change.getDestination().equals(this.destination)) {
			this.destination = change.getDestination();
			isChanged = true;
		}
		if (!change.getGatewayOnLink().equals(this.gatewayOnLink)) {
			this.gatewayOnLink = change.getGatewayOnLink();
			isChanged = true;
		}
		return isChanged;
	}

}
