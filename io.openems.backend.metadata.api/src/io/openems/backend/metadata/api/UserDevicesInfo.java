package io.openems.backend.metadata.api;

import com.google.common.collect.Multimap;

import io.openems.common.types.DeviceImpl;

public class UserDevicesInfo {

	private int userId;
	private String userName;
	private Multimap<String, DeviceImpl> deviceMap;
	
	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setDevices(Multimap<String, DeviceImpl> deviceMap) {
		this.deviceMap = deviceMap;
	}

	public int getUserId() {
		return userId;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public Multimap<String, DeviceImpl> getDeviceMap() {
		return deviceMap;
	}	
}
