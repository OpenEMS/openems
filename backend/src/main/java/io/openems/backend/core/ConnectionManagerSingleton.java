package io.openems.backend.core;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.utilities.ManyToMany;

public class ConnectionManagerSingleton {

	protected ConnectionManagerSingleton() {};

	/**
	 * Stores active websockets to browsers
	 */
	private final BiMap<WebSocket, BrowserSession> browserWebsockets = Maps.synchronizedBiMap(HashBiMap.create());

	/**
	 * Stores active websockets to openems devices (value = deviceName, e.g. 'fems5')
	 */
	private final BiMap<WebSocket, String> openemsWebsockets = Maps.synchronizedBiMap(HashBiMap.create());

	/**
	 * Stores active interconnections between browser (key) and openems (value) websockets
	 */
	private final ManyToMany<WebSocket, WebSocket> websocketInterconnection = new ManyToMany<>();

	/*
	 * Helper methods for Browser <-> OpenEMS interconnection
	 */
	public void addWebsocketInterconnection(WebSocket browserWebsocket, WebSocket openemsWebsocket) {
		websocketInterconnection.put(browserWebsocket, openemsWebsocket);
	}

	public void removeWebsocketInterconnection(WebSocket browserWebsocket, WebSocket openemsWebsocket) {
		websocketInterconnection.remove(browserWebsocket, openemsWebsocket);
	}

	/*
	 * Helper methods for Browser websockets
	 */
	public void addBrowserWebsocket(WebSocket websocket, BrowserSession session) {
		this.browserWebsockets.forcePut(websocket, session);
	}

	public void removeBrowserWebsocket(WebSocket websocket) {
		this.browserWebsockets.remove(websocket);
		this.websocketInterconnection.removeAllKeys(websocket);
	}

	/*
	 * Helper methods for OpenEMS websockets
	 */
	public void addOpenemsWebsocket(WebSocket websocket, String deviceName) {
		this.openemsWebsockets.forcePut(websocket, deviceName);
	}

	public void removeOpenemsWebsocket(WebSocket websocket) {
		this.openemsWebsockets.remove(websocket);
		this.websocketInterconnection.removeAllValues(websocket);
	}

	public Optional<String> getDeviceNameFromOpenemsWebsocket(WebSocket websocket) {
		String deviceName = this.openemsWebsockets.get(websocket);
		return Optional.ofNullable(deviceName);
	}

	public Optional<WebSocket> getOpenemsWebsocketFromDeviceName(String deviceName) {
		WebSocket websocket = this.openemsWebsockets.inverse().get(deviceName);
		return Optional.ofNullable(websocket);
	}

	public boolean isOpenemsWebsocketConnected(String deviceName) {
		return this.openemsWebsockets.containsValue(deviceName);
	}
}
