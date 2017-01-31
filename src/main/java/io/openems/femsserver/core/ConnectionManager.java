package io.openems.femsserver.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.femsserver.odoo.Odoo;
import io.openems.femsserver.odoo.fems.device.FemsDevice;
import io.openems.femsserver.utilities.ManyToMany;

public class ConnectionManager {

	private static Logger log = LoggerFactory.getLogger(ConnectionManager.class);

	private static ConnectionManager instance;

	public static synchronized ConnectionManager getInstance() {
		if (ConnectionManager.instance == null) {
			ConnectionManager.instance = new ConnectionManager();
		}
		return ConnectionManager.instance;
	}

	private Odoo odoo;

	private ConnectionManager() {
		this.odoo = Odoo.getInstance();
	}

	/**
	 * Stores info about FEMS devices
	 * Key: fems-name (e.g. "fems7") - Value: FemsDevice object
	 */
	private HashMap<String, FemsDevice> femsDevices = new HashMap<>();

	/**
	 * Stores all active websockets to FEMS devices
	 * Key: Websocket to FEMS - Value: fems-name (e.g. "fems7")
	 */
	private ManyToMany<WebSocket, String> femsWebsockets = new ManyToMany<>();

	/**
	 * Stores all active websockets to browsers
	 * Key: Websocket to browser - Value: fems-name (e.g. "fems7")
	 */
	private ManyToMany<WebSocket, String> browserWebsockets = new ManyToMany<>();

	/**
	 * Stores a websocket connection to FEMS together with the connected FemsDevice objects
	 *
	 * @param webSocket
	 * @param device
	 */
	public synchronized void addFemsWebsocket(WebSocket websocket, List<FemsDevice> devices) {
		devices.forEach(device -> {
			String name = device.getName();
			/*
			 * Check if femsDevice already existed in cache. If so, refresh and reuse it. Otherwise add it.
			 */
			if (this.femsDevices.containsKey(name)) {
				// refresh an existing FemsDevice object
				FemsDevice existingDevice = this.femsDevices.get(name);
				existingDevice.refreshFrom(device);
				device = existingDevice;
			} else {
				// put new object
				this.femsDevices.put(name, device);
			}
			/*
			 * Store the websocket connection
			 */
			this.femsWebsockets.put(websocket, name);
		});
	}

	/**
	 * Remove a websocket connection to FEMS
	 *
	 * @param WebSocket
	 */
	public synchronized void removeFemsWebsocket(WebSocket websocket) {
		this.femsWebsockets.removeAllKeys(websocket);
	}

	/**
	 * Returns all devices for this websocket
	 *
	 * @param websocket
	 * @return
	 */
	public synchronized List<FemsDevice> getFemsWebsocketDevices(WebSocket websocket) {
		List<FemsDevice> devices = new ArrayList<>();
		this.getFemsWebsocketDeviceNames(websocket).forEach(name -> {
			devices.add(this.femsDevices.get(name));
		});
		return Collections.unmodifiableList(devices);
	}

	/**
	 * Returns all fems websockets for a device name
	 *
	 * @param name
	 * @return
	 */
	public synchronized Set<WebSocket> getFemsWebsockets(String name) {
		return Collections.unmodifiableSet(this.femsWebsockets.getKeys(name));
	}

	/**
	 * Returns all device names for this websocket
	 *
	 * @param websocket
	 * @return
	 */
	public Set<String> getFemsWebsocketDeviceNames(WebSocket websocket) {
		return Collections.unmodifiableSet(this.femsWebsockets.getValues(websocket));
	}

	/**
	 * Stores a websocket connection to a browser together with the allowed FemsDevice objects
	 *
	 * @param webSocket
	 * @param device
	 */
	public synchronized void addBrowserWebsocket(WebSocket websocket, List<FemsDevice> devices) {
		devices.forEach(device -> {
			String name = device.getName();
			/*
			 * Check if femsDevice already existed in cache. If so, refresh and reuse it. Otherwise add it.
			 */
			if (this.femsDevices.containsKey(name)) {
				// refresh an existing FemsDevice object
				FemsDevice existingDevice = this.femsDevices.get(name);
				existingDevice.refreshFrom(device);
				device = existingDevice;
			} else {
				// put new object
				this.femsDevices.put(name, device);
			}
			/*
			 * Store the websocket connection
			 */
			this.browserWebsockets.put(websocket, name);
		});
	}

	/**
	 * Remove a websocket connection to a browser
	 *
	 * @param WebSocket
	 */
	public synchronized void removeBrowserWebsocket(WebSocket websocket) {
		this.browserWebsockets.removeAllKeys(websocket);
	}

	public synchronized boolean isFemsOnline(String name) {
		return this.femsWebsockets.getKeys(name).size() > 0;
	}

	/**
	 * Returns all browser websockets for a device name
	 *
	 * @param name
	 * @return
	 */
	public synchronized Set<WebSocket> getBrowserWebsockets(String name) {
		return Collections.unmodifiableSet(this.browserWebsockets.getKeys(name));
	}
}
