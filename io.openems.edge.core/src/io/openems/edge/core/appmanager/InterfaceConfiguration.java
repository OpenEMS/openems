package io.openems.edge.core.appmanager;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.core.host.Inet4AddressWithSubnetmask;
import io.openems.edge.core.host.NetworkInterface.IpMasqueradeSetting;

public class InterfaceConfiguration {

	/**
	 * e. g. eth0.
	 */
	public final String interfaceName;

	/**
	 * The required ip's on the interface.
	 */
	private final List<Inet4AddressWithSubnetmask> ips = new LinkedList<>();
	private Boolean ipv4Forwarding;
	private IpMasqueradeSetting ipMasquerade;

	public InterfaceConfiguration(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	/**
	 * Sets the IP-Masquerade setting.
	 * 
	 * @param ipMasquerade the {@link IpMasqueradeSetting}
	 * @return this
	 */
	public InterfaceConfiguration setIpMasquerade(IpMasqueradeSetting ipMasquerade) {
		this.ipMasquerade = ipMasquerade;
		return this;
	}

	/**
	 * Sets the IPv4 Forwarding.
	 * 
	 * @param ipv4Forwarding the setting
	 * @return this
	 */
	public InterfaceConfiguration setIpv4Forwarding(Boolean ipv4Forwarding) {
		this.ipv4Forwarding = ipv4Forwarding;
		return this;
	}

	/**
	 * Adds an ip to the list.
	 * 
	 * @param ip the {@link Inet4AddressWithSubnetmask} to add
	 * @return this
	 */
	public InterfaceConfiguration addIp(Inet4AddressWithSubnetmask ip) {
		this.ips.add(ip);
		return this;
	}

	/**
	 * Adds an ip to the list with using
	 * {@link Inet4AddressWithSubnetmask#fromString(String)}.
	 * 
	 * @param ip the {@link Inet4AddressWithSubnetmask} to add
	 * @return this
	 * @throws OpenemsException if
	 *                          {@link Inet4AddressWithSubnetmask#fromString(String)}
	 *                          throws an error
	 */
	public InterfaceConfiguration addIp(String ip) throws OpenemsException {
		this.ips.add(Inet4AddressWithSubnetmask.fromString(ip));
		return this;
	}

	/**
	 * Adds an ip to the list with using
	 * {@link Inet4AddressWithSubnetmask#fromString(String, String)}.
	 * 
	 * @param label the label with a length of 1..15 characters
	 * @param ip    the {@link Inet4AddressWithSubnetmask} to add
	 * @return this
	 * @throws OpenemsException if
	 *                          {@link Inet4AddressWithSubnetmask#fromString(String)}
	 *                          throws an error
	 */
	public InterfaceConfiguration addIp(String label, String ip) throws OpenemsException {
		if (label.length() > 15) {
			throw new IllegalArgumentException("label length must be in 1..15");
		}
		this.ips.add(Inet4AddressWithSubnetmask.fromString(label, ip));
		return this;
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
		return "InterfaceConfiguration [interfaceName=" + this.interfaceName + ", ips="
				+ this.ips.stream().map(Object::toString).collect(joining(", "))//
				+ ", ipv4Forwarding=" + this.ipv4Forwarding //
				+ ", ipMasquerade=" + this.ipMasquerade + "]";
	}

	/**
	 * Summarizes the duplicated interfaces into one.
	 * 
	 * @param interfaceConfiguration the configurations to summarize
	 * @return the interfaces
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
			var newInterface = new InterfaceConfiguration(u.interfaceName);
			newInterface.getIps().addAll(existingInterfaces.stream() //
					.flatMap(i -> i.getIps().stream()) //
					.collect(Collectors.toList()));

			newInterface.setIpv4Forwarding(existingInterfaces.stream() //
					.map(InterfaceConfiguration::getIpv4Forwarding) //
					.filter(Objects::nonNull) //
					.reduce((a, b) -> {
						if (a != b) {
							throw new RuntimeException("Ipv4 Forwarding got multiple values: " + a + " and " + b);
						}
						return b;
					}).orElse(null));

			newInterface.setIpMasquerade(existingInterfaces.stream() //
					.map(InterfaceConfiguration::getIpMasquerade) //
					.filter(Objects::nonNull) //
					.reduce((a, b) -> {
						if (a != b) {
							throw new RuntimeException("Ip Masquerade got multiple values: " + a + " and " + b);
						}
						return b;
					}).orElse(null));
		};
		return Objects.requireNonNull(interfaceConfiguration).stream() //
				.collect(ArrayList::new, flatAdd, (t, u) -> {
					u.stream().forEach(i -> flatAdd.accept(t, i));
				});
	}

	/**
	 * Removes ip's from the {@code listToRemove} if the ip-address is in both
	 * lists.
	 * 
	 * @param listToRemove the list to remove the ip's
	 * @param other        the other interfaces
	 */
	public static void removeDuplicatedIps(//
			final List<InterfaceConfiguration> listToRemove, //
			final List<InterfaceConfiguration> other //
	) {
		Objects.requireNonNull(listToRemove);
		Objects.requireNonNull(other);

		for (var interfaceConfiguration : listToRemove) {
			var otherInterfaces = other.stream() //
					.filter(oi -> oi.interfaceName.equals(interfaceConfiguration.interfaceName)) //
					.collect(Collectors.toList());
			if (otherInterfaces.isEmpty()) {
				continue;
			}
			otherInterfaces.stream() //
					.flatMap(t -> t.getIps().stream()) //
					.forEach(interfaceConfiguration.getIps()::remove);

			if (interfaceConfiguration.getIpv4Forwarding() != null) {
				if (otherInterfaces.stream()
						.anyMatch(t -> t.getIpv4Forwarding() == interfaceConfiguration.getIpv4Forwarding())) {
					interfaceConfiguration.setIpv4Forwarding(null);
				}
			}
			if (interfaceConfiguration.getIpMasquerade() != null) {
				if (otherInterfaces.stream()
						.anyMatch(t -> t.getIpMasquerade() == interfaceConfiguration.getIpMasquerade())) {
					interfaceConfiguration.setIpMasquerade(null);
				}
			}
		}
	}

}