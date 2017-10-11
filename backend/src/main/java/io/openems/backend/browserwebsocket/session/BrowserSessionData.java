package io.openems.backend.browserwebsocket.session;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;

import io.openems.common.session.SessionData;
import io.openems.common.types.Device;

public class BrowserSessionData extends SessionData {
	private String userName = "";
	private Optional<Integer> userId = Optional.empty();
	private Optional<String> odooSessionId = Optional.empty();
	private HashMultimap<String, Device> devices = HashMultimap.create(0, 0);

	public Optional<String> getOdooSessionId() {
		return odooSessionId;
	}

	public void setOdooSessionId(String odooSessionId) {
		this.odooSessionId = Optional.ofNullable(odooSessionId);
	}

	public void setDevices(HashMultimap<String, Device> deviceMap) {
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

	public Set<Device> getDevices(String name) {
		return this.devices.get(name);
	}

	public Collection<Device> getDevices() {
		return this.devices.values();
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		// TODO Auto-generated method stub
		return j;
	}
}
