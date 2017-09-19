package io.openems.backend.browserwebsocket.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.common.session.SessionData;
import io.openems.common.types.Device;

public class BrowserSessionData extends SessionData {
	private String userName = "";
	private Optional<Integer> userId = Optional.empty();
	private Optional<String> odooSessionId = Optional.empty();
	private List<Device> devices = new ArrayList<>();

	public Optional<String> getOdooSessionId() {
		return odooSessionId;
	}

	public void setOdooSessionId(String odooSessionId) {
		this.odooSessionId = Optional.ofNullable(odooSessionId);
	}

	public void setDevices(List<Device> deviceInfos) {
		this.devices = deviceInfos;
	}

	public void setUserId(Integer userId) {
		this.userId = Optional.of(userId);
	}

	public void setUserName(String name) {
		this.userName = name;
	}

	public Optional<Integer> getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}

	public List<Device> getDevices() {
		return devices;
	}
}
