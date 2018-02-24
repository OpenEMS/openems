package io.openems.common.types;

import java.util.Optional;

/**
 * Helper class to store tuple of device name and role
 *
 * @author stefan.feilmeier
 *
 */
public class DeviceImpl implements Comparable<DeviceImpl>, Device {
	private final String name;
	private final String comment;
	private final String producttype;
//	private final Role role;
	private boolean online = false;

	public DeviceImpl(String name, String comment, String producttype, String role) {
		this.name = name;
		this.comment = comment;
		this.producttype = producttype;
//		this.role = Role.getRole(role);
	}

	public String getName() {
		return name;
	}

//	public Role getRole() {
//		return role;
//	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isOnline() {
		return online;
	}

	public String getComment() {
		return comment;
	}

	public String getProducttype() {
		return producttype;
	}

	@Override
	public int compareTo(DeviceImpl other) {
		return (this.name + this.comment + this.producttype +  this.online)
				.compareTo(other.name + other.comment + other.producttype + other.online);
	}

	@Override
	public Optional<Integer> getIdOpt() {
		return Device.parseNumberFromName(this.getName());
	}
}
