package io.openems.backend.edgewebsocket.impl;

import java.util.Optional;

import io.openems.common.access_control.RoleId;

public class WsData extends io.openems.common.websocket.WsData {

	private String apiKey;
	private String edgeId;
	private RoleId roleId;

	public WsData() {
	}

	public RoleId getRoleId() {
		return roleId;
	}

	public void setRoleId(RoleId roleId) {
		this.roleId = roleId;
	}

	public synchronized void setApikey(String apiKey) {
		this.apiKey = apiKey;
	}

	public synchronized Optional<String> getApikey() {
		return Optional.ofNullable(apiKey);
	}

	public synchronized void setEdgeId(String edgeId) {
		this.edgeId = edgeId;
	}

	public synchronized Optional<String> getEdgeId() {
		return Optional.ofNullable(edgeId);
	}

	@Override
	public String toString() {
		return "EdgeWebsocket.WsData [apikey=" + getApikey().orElse("UNKNOWN") + ", edgeId=" + getEdgeId().orElse("UNKNOWN")
				+ "]";
	}
}
