package io.openems.backend.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.backend.metadata.odoo.device.OdooDevice;
import io.openems.backend.utilities.ManyToMany;

public class ConnectionManagerALT {

	private static Logger log = LoggerFactory.getLogger(ConnectionManagerALT.class);

	private static ConnectionManagerALT instance;

	public static synchronized ConnectionManagerALT getInstance() {
		if (ConnectionManagerALT.instance == null) {
			ConnectionManagerALT.instance = new ConnectionManagerALT();
		}
		return ConnectionManagerALT.instance;
	}

	private ConnectionManagerALT() {}

	/**
	 * Stores info about FEMS devices
	 * Key: fems-name (e.g. "fems7") - Value: FemsDevice object
	 */
	private HashMap<String, OdooDevice> femsDevices = new HashMap<>();

	/**
	 * Stores all active websockets to FEMS devices
	 * Key: Websocket to FEMS - Value: fems-name (e.g. "fems7")
	 */
	private Multimap<WebSocket, String> femsWebsockets = HashMultimap.create();

	/**
	 * Stores all active websockets to browsers
	 * Key: Websocket to browser - Value: fems-name (e.g. "fems7")
	 */
	private ManyToMany<WebSocket, String> browserWebsockets = new ManyToMany<>();

	/**
	 * Stores a websocket connection to FEMS and the connected FemsDevice objects
	 *
	 * @param webSocket
	 * @param device
	 */
	public synchronized void addFemsWebsocket(WebSocket websocket, List<OdooDevice> devices) {
		devices.forEach(device -> {
			String name = device.getName();
			/*
			 * Store the FemsDevice
			 */
			// Check if femsDevice already existed in cache. If so, refresh and reuse it. Otherwise add it.
			if (this.femsDevices.containsKey(name)) {
				// refresh an existing FemsDevice object
				OdooDevice existingDevice = this.femsDevices.get(name);
				existingDevice.refreshFrom(device);
			} else {
				// put new object
				this.femsDevices.put(name, device);
			}
			/*
			 * Store the websocket connection
			 */
			// close old websocket connection(s) to this device
			for (Iterator<Entry<WebSocket, String>> it = this.femsWebsockets.entries().iterator(); it.hasNext();) {
				Entry<WebSocket, String> entry = it.next();
				if (entry.getValue().equals(name)) {
					WebSocket oldWebsocket = entry.getKey();
					oldWebsocket.close(CloseFrame.POLICY_VALIDATION, "Another websocket ["
							+ websocket.getRemoteSocketAddress().getHostString() + "] connected for [" + name + "].");
					it.remove();
				}
			}
			// add new websocket for this device
			this.femsWebsockets.put(websocket, name);
		});
	}

	/**
	 * Remove a websocket connection to FEMS
	 *
	 * @param WebSocket
	 */
	public synchronized void removeFemsWebsocket(WebSocket websocket) {
		this.femsWebsockets.removeAll(websocket);
	}

	/**
	 * Returns all devices for this websocket
	 *
	 * @param websocket
	 * @return
	 */
	public synchronized List<OdooDevice> getFemsWebsocketDevices(WebSocket websocket) {
		List<OdooDevice> devices = new ArrayList<>();
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
	public synchronized Optional<WebSocket> getFemsWebsocket(String name) {
		for (Iterator<Entry<WebSocket, String>> it = this.femsWebsockets.entries().iterator(); it.hasNext();) {
			Entry<WebSocket, String> entry = it.next();
			if (entry.getValue().equals(name)) {
				return Optional.of(entry.getKey());
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns all device names for this websocket
	 *
	 * @param websocket
	 * @return
	 */
	public synchronized Collection<String> getFemsWebsocketDeviceNames(WebSocket websocket) {
		return Collections.unmodifiableCollection(this.femsWebsockets.get(websocket));
	}

	/**
	 * Stores a websocket connection to a browser together with the allowed FemsDevice objects
	 *
	 * @param webSocket
	 * @param device
	 */
	public synchronized void addBrowserWebsocket(WebSocket websocket, List<OdooDevice> devices) {
		devices.forEach(device -> {
			String name = device.getName();
			/*
			 * Check if femsDevice already existed in cache. If so, refresh and reuse it. Otherwise add it.
			 */
			if (this.femsDevices.containsKey(name)) {
				// refresh an existing FemsDevice object
				OdooDevice existingDevice = this.femsDevices.get(name);
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
		return this.femsWebsockets.containsValue(name);
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
