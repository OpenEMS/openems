package io.openems.backend.browserwebsocket.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.backend.browserwebsocket.DeviceInfo;
import io.openems.common.session.SessionData;

public class BrowserSessionData extends SessionData {
	private Optional<Integer> userId = Optional.empty();
	private Optional<String> odooSessionId = Optional.empty();
	private List<DeviceInfo> deviceInfos = new ArrayList<>();

	public Optional<String> getOdooSessionId() {
		return odooSessionId;
	}

	public void setOdooSessionId(String odooSessionId) {
		this.odooSessionId = Optional.ofNullable(odooSessionId);
	}

	public void setDeviceInfos(List<DeviceInfo> deviceInfos) {
		this.deviceInfos = deviceInfos;
	}

	public void setUserId(Integer userId) {
		this.userId = Optional.of(userId);
	}

	public Optional<Integer> getUserId() {
		return userId;
	}

	public List<DeviceInfo> getDeviceInfos() {
		return deviceInfos;
	}
}
