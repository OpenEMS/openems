package io.openems.backend.openemswebsocket.session;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.common.session.SessionData;

public class OpenemsSessionData extends SessionData {
	private final MetadataDevice device;
	private final WebSocket websocket;

	public OpenemsSessionData(WebSocket websocket, MetadataDevice device) {
		this.device = device;
		this.websocket = websocket;
	}

	public WebSocket getWebsocket() {
		return websocket;
	}

	public MetadataDevice getDevice() {
		return device;
	}

	@Override
	public JsonObject toJsonObject() {
		return this.device.toJsonObject();
	}
}
