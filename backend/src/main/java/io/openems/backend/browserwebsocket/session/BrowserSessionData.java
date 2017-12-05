package io.openems.backend.browserwebsocket.session;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonObject;

import io.openems.common.session.SessionData;
import io.openems.common.types.DeviceImpl;

public class BrowserSessionData extends SessionData {
	private String userName = "";
	private Optional<Integer> userId = Optional.empty();
	private Optional<String> odooSessionIdOpt = Optional.empty();
	private LinkedHashMultimap<String, DeviceImpl> devices = LinkedHashMultimap.create();
	private Optional<BackendCurrentDataWorker> currentDataWorkerOpt = Optional.empty();

	public Optional<String> getOdooSessionId() {
		return odooSessionIdOpt;
	}

	public void setOdooSessionId(Optional<String> odooSessionIdOpt) {
		this.odooSessionIdOpt = odooSessionIdOpt;
	}

	public void setDevices(LinkedHashMultimap<String, DeviceImpl> deviceMap) {
		this.devices = deviceMap;
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

	public Set<DeviceImpl> getDevices(String name) {
		return this.devices.get(name);
	}

	public Collection<DeviceImpl> getDevices() {
		return this.devices.values();
	}

	public void setCurrentDataWorkerOpt(BackendCurrentDataWorker currentDataWorker) {
		this.currentDataWorkerOpt = Optional.ofNullable(currentDataWorker);
	}

	public Optional<BackendCurrentDataWorker> getCurrentDataWorkerOpt() {
		return currentDataWorkerOpt;
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		// TODO Auto-generated method stub
		return j;
	}
}
