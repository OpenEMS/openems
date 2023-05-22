package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.core.host.Inet4AddressWithSubnetmask;

public class InterfaceConfiguration {

	/**
	 * e. g. eth0.
	 */
	public final String interfaceName;

	/**
	 * The required ip's on the interface.
	 */
	private final List<Inet4AddressWithSubnetmask> ips = new LinkedList<>();

	public InterfaceConfiguration(String interfaceName) {
		this.interfaceName = interfaceName;
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
	 * @param label the label of the ip
	 * @param ip    the {@link Inet4AddressWithSubnetmask} to add
	 * @return this
	 * @throws OpenemsException if
	 *                          {@link Inet4AddressWithSubnetmask#fromString(String)}
	 *                          throws an error
	 */
	public InterfaceConfiguration addIp(String label, String ip) throws OpenemsException {
		this.ips.add(Inet4AddressWithSubnetmask.fromString(label, ip));
		return this;
	}

	public List<Inet4AddressWithSubnetmask> getIps() {
		return this.ips;
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
		return "InterfaceConfiguration [interfaceName=" + this.interfaceName + ", ips=" + this.ips + "]";
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
					.collect(Collectors.toList());
			if (existingInterfaces.isEmpty()) {
				t.add(u);
				return;
			}
			var newInterface = new InterfaceConfiguration(u.interfaceName);
			newInterface.getIps().addAll(existingInterfaces.stream() //
					.flatMap(i -> i.getIps().stream()) //
					.collect(Collectors.toList()));
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
		Objects.requireNonNull(listToRemove).stream() //
				.forEach(i -> {
					var otherInterfaces = other.stream() //
							.filter(oi -> oi.interfaceName.equals(i.interfaceName)) //
							.collect(Collectors.toList());
					if (otherInterfaces.isEmpty()) {
						return;
					}
					otherInterfaces.stream() //
							.flatMap(t -> t.getIps().stream()) //
							.forEach(i.getIps()::remove);
				});
	}

}