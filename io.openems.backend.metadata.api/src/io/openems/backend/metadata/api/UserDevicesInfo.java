package io.openems.backend.metadata.api;

import java.util.HashMap;
import java.util.Map;

public class UserDevicesInfo {

	private final User user;
	private final Map<Integer, Edge> devices = new HashMap<>();

	public UserDevicesInfo(User user) {
		super();
		this.user = user;
	}
	
	public void addDevice(Edge device) {
		this.devices.put(device.getId(), device);
	}
	
	public User getUser() {
		return user;
	}
	
	public Map<Integer, Edge> getDevices() {
		return devices;
	}

	@Override
	public String toString() {
		return "UserDevicesInfo [user=" + user + ", devices=" + devices + "]";
	}
}
