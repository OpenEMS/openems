package io.openems.edge.core.appmanager;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.core.host.Inet4AddressWithSubnetmask;
import io.openems.edge.core.host.NetworkInterface.IpMasqueradeSetting;
import io.openems.edge.core.host.Routes;

public class InterfaceConfiguration {

	/**
	 * The name of the network interface, e.g. {@code eth0}.
	 */
	public final String interfaceName;

	/**
	 * The required IP addresses on the interface.
	 */
	private final List<Inet4AddressWithSubnetmask> ips = new LinkedList<>();
	private Boolean ipv4Forwarding;
	private IpMasqueradeSetting ipMasquerade;

	private Boolean dhcp;
	private String dns;
	private int dhcpRouteMetric;
	private String gateway;
	private Boolean gatewayOnLink;
	private Boolean createIfNotExist;
	private final List<Routes> routes = new LinkedList<>();

	public InterfaceConfiguration(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	/**
	 * Adds a route to this interface configuration.
	 *
	 * @param route the {@link Routes} object to add
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration addRoute(Routes route) {
		this.routes.add(route);
		return this;
	}

	/**
	 * Sets the DHCP configuration.
	 *
	 * @param dhcp {@code true} to enable DHCP, {@code false} to disable
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setDhcp(Boolean dhcp) {
		this.dhcp = dhcp;
		return this;
	}

	/**
	 * Sets the DNS server address.
	 *
	 * @param dns the DNS server address
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setDns(String dns) {
		this.dns = dns;
		return this;
	}

	/**
	 * Sets the DHCP route metric.
	 *
	 * @param dhcpRouteMetric the route metric value to use when DHCP is enabled
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setDhcpRouteMetric(int dhcpRouteMetric) {
		this.dhcpRouteMetric = dhcpRouteMetric;
		return this;
	}

	/**
	 * Sets the default gateway.
	 *
	 * @param gateway the gateway IP address
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setGateway(String gateway) {
		this.gateway = gateway;
		return this;
	}

	/**
	 * Sets whether the gateway is on-link.
	 *
	 * @param gatewayOnLink {@code true} if the gateway is on-link, {@code false}
	 *                      otherwise
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setGatewayOnLink(Boolean gatewayOnLink) {
		this.gatewayOnLink = gatewayOnLink;
		return this;
	}

	/**
	 * Creates an interface if the interface not exist.
	 *
	 * @param createIfNotExist {@code true} if the interface need to be created
	 *                         {@code false} otherwise
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setCreateIfNotExist(Boolean createIfNotExist) {
		this.createIfNotExist = createIfNotExist;
		return this;
	}

	/**
	 * Sets the IP masquerade setting.
	 *
	 * @param ipMasquerade the {@link IpMasqueradeSetting}
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setIpMasquerade(IpMasqueradeSetting ipMasquerade) {
		this.ipMasquerade = ipMasquerade;
		return this;
	}

	/**
	 * Sets the IPv4 forwarding setting.
	 *
	 * @param ipv4Forwarding {@code true} to enable IPv4 forwarding, {@code false}
	 *                       to disable
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration setIpv4Forwarding(Boolean ipv4Forwarding) {
		this.ipv4Forwarding = ipv4Forwarding;
		return this;
	}

	/**
	 * Adds an IP address to the list.
	 *
	 * @param ip the {@link Inet4AddressWithSubnetmask} to add
	 * @return this instance for method chaining
	 */
	public InterfaceConfiguration addIp(Inet4AddressWithSubnetmask ip) {
		this.ips.add(ip);
		return this;
	}

	/**
	 * Adds an IP address using
	 * {@link Inet4AddressWithSubnetmask#fromString(String)}.
	 *
	 * @param ip the IP address string
	 * @return this instance for method chaining
	 * @throws OpenemsException if parsing fails
	 */
	public InterfaceConfiguration addIp(String ip) throws OpenemsException {
		this.ips.add(Inet4AddressWithSubnetmask.fromString(ip));
		return this;
	}

	/**
	 * Adds a labeled IPv4 address using
	 * {@link Inet4AddressWithSubnetmask#fromString(String, String)} to parse the
	 * given IP address and subnet mask information.
	 *
	 * <p>
	 * The label must contain between 1-15 characters. If the label length is
	 * invalid or the IP address cannot be parsed, an exception will be thrown.
	 * </p>
	 *
	 * @param label the identifier for this IP address; must be 1-15 characters long
	 * @param ip    the IPv4 address string (including subnet mask if required by
	 *              the parser)
	 * @return this instance for method chaining
	 * @throws OpenemsException         if the IP address cannot be parsed
	 * @throws IllegalArgumentException if the label does not contain 1-15
	 *                                  characters
	 */
	public InterfaceConfiguration addIp(String label, String ip) throws OpenemsException {
		if (label.length() > 15) {
			throw new IllegalArgumentException("label length must be in 1..15");
		}
		this.ips.add(Inet4AddressWithSubnetmask.fromString(label, ip));
		return this;
	}

	public List<Routes> getRoutes() {
		return this.routes;
	}

	public Boolean getDhcp() {
		return this.dhcp;
	}

	public String getDns() {
		return this.dns;
	}

	public int getDhcpRouteMetric() {
		return this.dhcpRouteMetric;
	}

	public String getGateway() {
		return this.gateway;
	}

	public Boolean getGatewayOnLink() {
		return this.gatewayOnLink;
	}

	public Boolean getCreateIfNotExist() {
		return this.createIfNotExist;
	}

	public List<Inet4AddressWithSubnetmask> getIps() {
		return this.ips;
	}

	public IpMasqueradeSetting getIpMasquerade() {
		return this.ipMasquerade;
	}

	public Boolean getIpv4Forwarding() {
		return this.ipv4Forwarding;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.interfaceName, this.ips);
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
		var other = (InterfaceConfiguration) obj;
		return Objects.equals(this.interfaceName, other.interfaceName) && Objects.equals(this.ips, other.ips);
	}

	@Override
	public String toString() {
		return "InterfaceConfiguration ["//
				+ "interfaceName=" + this.interfaceName//
				+ ", ips=" + this.ips.stream().map(Object::toString).collect(joining(", "))//
				+ ", ipv4Forwarding=" + this.ipv4Forwarding//
				+ ", ipMasquerade=" + this.ipMasquerade//
				+ ", dhcp=" + this.dhcp //
				+ ", dns=" + this.dns //
				+ ", dhcpRouteMetric=" + this.dhcpRouteMetric//
				+ ", gateway=" + this.gateway //
				+ ", gatewayOnLink=" + this.gatewayOnLink//
				+ ", createIfNotExist=" + this.createIfNotExist//
				+ ", routes=" + this.routes.stream().map(Object::toString).collect(joining(", "))//
				+ "]";
	}

	/**
	 * Combines multiple {@link InterfaceConfiguration} objects with the same
	 * interface name into a single summarized configuration.
	 *
	 * @param interfaceConfiguration the list of configurations to summarize
	 * @return a list of summarized {@link InterfaceConfiguration} objects
	 */
	public static List<InterfaceConfiguration> summarize(//
			final List<InterfaceConfiguration> interfaceConfiguration //
	) {
		BiConsumer<List<InterfaceConfiguration>, InterfaceConfiguration> flatAdd = (t, u) -> {
			var existingInterfaces = t.stream() //
					.filter(i -> i.interfaceName.equals(u.interfaceName)) //
					.toList();
			if (existingInterfaces.isEmpty()) {
				t.add(u);
				return;
			}
			t.removeAll(existingInterfaces);
			var newInterface = new InterfaceConfiguration(u.interfaceName);
			newInterface.getIps().addAll(existingInterfaces.stream() //
					.flatMap(i -> i.getIps().stream()) //
					.toList());
			newInterface.getIps().addAll(u.getIps());

			newInterface.setIpv4Forwarding(existingInterfaces.stream() //
					.map(InterfaceConfiguration::getIpv4Forwarding) //
					.filter(Objects::nonNull) //
					.reduce((a, b) -> {
						if (a != b) {
							throw new RuntimeException(
									"IPv4 Forwarding has multiple conflicting values: " + a + " and " + b);
						}
						return b;
					}).orElse(null));

			newInterface.setIpMasquerade(existingInterfaces.stream() //
					.map(InterfaceConfiguration::getIpMasquerade) //
					.filter(Objects::nonNull) //
					.reduce((a, b) -> {
						if (a != b) {
							throw new RuntimeException(
									"IP Masquerade has multiple conflicting values: " + a + " and " + b);
						}
						return b;
					}).orElse(null));

			// Use the new values for DHCP, DNS, Route Metric, Gateway, and GatewayOnLink
			newInterface.setDhcp(u.getDhcp());
			newInterface.setDns(u.getDns());
			newInterface.setDhcpRouteMetric(u.getDhcpRouteMetric());
			newInterface.setGateway(u.getGateway());
			newInterface.setGatewayOnLink(u.getGatewayOnLink());

			newInterface.getRoutes().addAll(existingInterfaces.stream() //
					.flatMap(i -> i.getRoutes().stream()) //
					.toList());
			newInterface.getRoutes().addAll(u.getRoutes());

			t.add(newInterface);
		};
		return Objects.requireNonNull(interfaceConfiguration).stream() //
				.collect(ArrayList::new, flatAdd, (t, u) -> {
					u.forEach(i -> flatAdd.accept(t, i));
				});
	}

	/**
	 * Removes duplicated IPs and settings from the first list that already exist in
	 * the second list.
	 *
	 * @param listToRemove the list from which to remove duplicates
	 * @param other        the reference list containing elements to remove
	 */
	public static void removeDuplicatedIps(//
			final List<InterfaceConfiguration> listToRemove, //
			final List<InterfaceConfiguration> other //
	) {
		Objects.requireNonNull(listToRemove);
		Objects.requireNonNull(other);

		for (var interfaceConfiguration : listToRemove) {
			// Find interfaces with the same name in the other list
			var otherInterfaces = other.stream() //
					.filter(oi -> oi.interfaceName.equals(interfaceConfiguration.interfaceName)) //
					.toList();
			if (otherInterfaces.isEmpty()) {
				continue;
			}

			// Remove IP addresses that exist in both lists
			var otherIps = otherInterfaces.stream() //
					.flatMap(t -> t.getIps().stream()) //
					.toList();
			interfaceConfiguration.getIps().removeAll(otherIps);

			// Clear IPv4 Forwarding if identical
			if (interfaceConfiguration.getIpv4Forwarding() != null && otherInterfaces.stream()
					.anyMatch(t -> Objects.equals(t.getIpv4Forwarding(), interfaceConfiguration.getIpv4Forwarding()))) {
				interfaceConfiguration.setIpv4Forwarding(null);
			}

			// Clear IP Masquerade if identical
			if (interfaceConfiguration.getIpMasquerade() != null && otherInterfaces.stream()
					.anyMatch(t -> Objects.equals(t.getIpMasquerade(), interfaceConfiguration.getIpMasquerade()))) {
				interfaceConfiguration.setIpMasquerade(null);
			}

			// Clear DHCP setting if identical
			if (interfaceConfiguration.getDhcp() != null && otherInterfaces.stream()
					.anyMatch(t -> Objects.equals(t.getDhcp(), interfaceConfiguration.getDhcp()))) {
				interfaceConfiguration.setDhcp(null);
			}

			// Clear DNS setting if identical
			if (interfaceConfiguration.getDns() != null && otherInterfaces.stream()
					.anyMatch(t -> Objects.equals(t.getDns(), interfaceConfiguration.getDns()))) {
				interfaceConfiguration.setDns(null);
			}

			// Reset DHCP Route Metric if identical
			if (interfaceConfiguration.getDhcpRouteMetric() != 0 && otherInterfaces.stream()
					.anyMatch(t -> t.getDhcpRouteMetric() == interfaceConfiguration.getDhcpRouteMetric())) {
				interfaceConfiguration.setDhcpRouteMetric(0);
			}

			// Clear Gateway if identical
			if (interfaceConfiguration.getGateway() != null && otherInterfaces.stream()
					.anyMatch(t -> Objects.equals(t.getGateway(), interfaceConfiguration.getGateway()))) {
				interfaceConfiguration.setGateway(null);
			}

			// Clear GatewayOnLink if identical
			if (interfaceConfiguration.getGatewayOnLink() != null && otherInterfaces.stream()
					.anyMatch(t -> Objects.equals(t.getGatewayOnLink(), interfaceConfiguration.getGatewayOnLink()))) {
				interfaceConfiguration.setGatewayOnLink(null);
			}

			// Remove routes that exist in both lists
			var otherRoutes = otherInterfaces.stream() //
					.flatMap(t -> t.getRoutes().stream()) //
					.toList();
			interfaceConfiguration.getRoutes().removeAll(otherRoutes);
		}
	}

}