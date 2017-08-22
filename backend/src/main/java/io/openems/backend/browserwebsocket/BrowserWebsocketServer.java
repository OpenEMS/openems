package io.openems.backend.browserwebsocket;

import java.net.InetSocketAddress;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.influx.InfluxdbProvider;
import io.openems.backend.odoo.device.Device;
import io.openems.backend.utilities.JsonUtils;
import io.openems.backend.utilities.OpenemsException;
import io.openems.backend.utilities.StringUtils;
import io.openems.backend.utilities.WebSocketUtils;

/**
 * Handles connections from a browser (for FENECON Online-Monitoring).
 * Needs to be initialized before it can be used as a singleton.
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocketServer extends WebSocketServer {

	private Logger log = LoggerFactory.getLogger(BrowserWebsocketServer.class);

	protected BrowserWebsocketServer(int port) throws Exception {
		super(new InetSocketAddress(port));
	}

	/**
	 * Open event of websocket. Expects an open Odoo "session_id". On success tells the ConnectionManager
	 * to keep the websocket. On failure closes the websocket. Sends an initial message to the browser.
	 */
	@Override
	public void onOpen(WebSocket websocket, ClientHandshake handshake) {
		try {
			String sessionId = parseSessionId(handshake);
			if (sessionId != null) {
				try {
					log.info("Incoming browser websocket using session [" + sessionId + "].");
					JsonObject jOdooResult = null;
					// TODO this.odoo.getFemsInfo(sessionId);
					// successfully logged in (otherwise an exception was thrown)
					JsonArray jOdooDevices = JsonUtils.getAsJsonArray(jOdooResult, "devices");
					List<Device> devices = new ArrayList<>();
					// TODO this.odoo.getDevicesForNames(jOdooDevices);
					connectionManager.addBrowserWebsocket(websocket, devices);
					/**
					 * send initial message
					 *
					 * <pre>
					 {
					   authenticate: {
					     mode: "allow",
					     username: "..."
					   }, metadata: {
					     devices: [{
					       name, online,...
					     }]
					
					   }
					 }
					 * </pre>
					 */
					JsonObject j = new JsonObject();
					JsonObject jAuthenticate = new JsonObject();
					jAuthenticate.addProperty("mode", "allow");
					j.add("authenticate", jAuthenticate);
					JsonArray jDevices = new JsonArray();
					devices.forEach(device -> {
						try {
							JsonObject jDevice = device.toJsonObject();
							jDevice.addProperty("online", connectionManager.isFemsOnline(device.getName()));
							jDevices.add(jDevice);
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
					JsonObject jMetadata = new JsonObject();
					jMetadata.add("devices", jDevices);
					jMetadata.addProperty("backend", "femsserver");
					j.add("metadata", jMetadata);
					WebSocketUtils.send(websocket, j);

				} catch (OpenemsException e) {
					// Authentication failed
					JsonObject j = new JsonObject();

					JsonObject jAuthenticate = new JsonObject();
					jAuthenticate.addProperty("mode", "deny");

					j.add("authenticate", jAuthenticate);
					WebSocketUtils.send(websocket, j);
					throw new OpenemsException(
							"Connection using session [" + sessionId + "] failed: " + e.getMessage());
				}
			} else {
				throw new OpenemsException("Connection failed. No session_id given.");
			}
		} catch (OpenemsException e) {
			log.error(e.getMessage());
			JsonObject j = generateNotification(e.getMessage());
			WebSocketUtils.send(websocket, j);
			try {
				Thread.sleep(1000); // give some time to send data
			} catch (InterruptedException e1) {}
			websocket.close();
		}
	}

	/**
	 * Close event of websocket. Tells the ConnectionManager to remove the websocket.
	 */
	@Override
	public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
		log.info("Close connection to [" + websocket + "]" //
				+ " Code [" + code + "] Reason [" + reason + "]");
		this.connectionManager.removeBrowserWebsocket(websocket);
	}

	/**
	 * Error event of websocket. Logs the error.
	 */
	@Override
	public void onError(WebSocket websocket, Exception ex) {
		log.info("Error on connection to [" + websocket + "]: " + ex.getMessage());
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	public void onMessage(WebSocket websocket, String message) {
		try {
			String requestId = "";
			JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
			if (jMessage.has("device")) {
				String deviceName = JsonUtils.getAsString(jMessage, "device");
				jMessage.remove("device");

				if (jMessage.has("requestId")) {
					try {
						requestId = JsonUtils.getAsString(jMessage, "requestId");
					} catch (Exception e) {
						log.warn("Invalid requestId: " + e.getMessage());
					}
				}

				/*
				 * Forward Subscribe to data
				 */
				if (jMessage.has("subscribe")) {
					subscribe(deviceName, jMessage.get("subscribe"));
				}

				/*
				 * Forward System command
				 */
				if (jMessage.has("system")) {
					system(deviceName, jMessage.get("system"));
				}

				/*
				 * Query command
				 */
				if (jMessage.has("query")) {
					query(requestId, deviceName, websocket, jMessage.get("query"));
				}
			}
		} catch (

		OpenemsException e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Tries to find a "session_id" in the handshake
	 *
	 * @param handshake
	 * @return session_id or null
	 */
	private String parseSessionId(ClientHandshake handshake) {
		String sessionId = null;
		if (handshake.hasFieldValue("cookie")) {
			String cookieString = handshake.getFieldValue("cookie");
			for (String cookieVariable : cookieString.split("; ")) {
				String[] keyValue = cookieVariable.split("=");
				if (keyValue.length == 2 && keyValue[0].equals("session_id")) {
					sessionId = keyValue[1];
				}
			}
		}
		return sessionId;
	}

	/**
	 * Generates a generic notification message
	 *
	 * @param message
	 * @return
	 */
	private JsonObject generateNotification(String message) {
		JsonObject j = new JsonObject();
		JsonObject jNotification = new JsonObject();
		jNotification.addProperty("message", message);
		j.add("notification", jNotification);
		return j;
	}

	/**
	 * Handle subscriptions
	 *
	 * @param j
	 */
	private synchronized void subscribe(String deviceName, JsonElement jSubscribeElement) {
		JsonObject j = new JsonObject();
		j.add("subscribe", jSubscribeElement);
		Optional<WebSocket> websocketOptional = this.connectionManager.getFemsWebsocket(deviceName);
		if (websocketOptional.isPresent()) {
			WebSocket websocket = websocketOptional.get();
			log.info(deviceName + ": forward subscribe to FEMS " + StringUtils.toShortString(j, 100));
			WebSocketUtils.send(websocket, j);
		}
	}

	/**
	 * System command
	 *
	 * @param j
	 */
	private synchronized void system(String deviceName, JsonElement jSubscribeElement) {
		JsonObject j = new JsonObject();
		j.add("system", jSubscribeElement);
		Optional<WebSocket> websocketOptional = this.connectionManager.getFemsWebsocket(deviceName);
		if (websocketOptional.isPresent()) {
			WebSocket websocket = websocketOptional.get();
			log.info(deviceName + ": forward system call to FEMS " + StringUtils.toShortString(j, 100));
			WebSocketUtils.send(websocket, j);
		}
	}

	/**
	 * Query command
	 *
	 * @param j
	 */
	private synchronized void query(String requestId, String deviceName, WebSocket websocket,
			JsonElement jQueryElement) {
		try {
			JsonObject jQuery = JsonUtils.getAsJsonObject(jQueryElement);
			String mode = JsonUtils.getAsString(jQuery, "mode");
			int fems = Integer.parseInt(deviceName.substring(4));
			if (mode.equals("history")) {
				/*
				 * History query
				 */
				int timezoneDiff = JsonUtils.getAsInt(jQuery, "timezone");
				ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
				ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(jQuery, "fromDate", timezone);
				ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(jQuery, "toDate", timezone);
				JsonObject channels = JsonUtils.getAsJsonObject(jQuery, "channels");
				// JsonObject kWh = JsonUtils.getAsJsonObject(jQuery, "kWh");
				int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
				// TODO: better calculation of sensible resolution
				int resolution = 5 * 60; // 5 Minutes
				if (days > 25) {
					resolution = 24 * 60 * 60; // 1 Day
				} else if (days > 6) {
					resolution = 3 * 60 * 60; // 3 Hours
				} else if (days > 2) {
					resolution = 60 * 60; // 60 Minutes
				}
				JsonObject jQueryreply = InfluxdbProvider.getInstance().query(fems, fromDate, toDate, channels,
						resolution/*
									 * ,
									 * kWh
									 */);

				JsonObject j = bootstrapReply(requestId);
				// Send result
				if (jQueryreply != null) {
					j.add("queryreply", jQueryreply);
				} else {
					j.addProperty("error", "No Queryable persistence found!");
				}
				WebSocketUtils.sendAsDevice(websocket, j, fems);
			}
		} catch (Exception e) {
			log.error("Error", e);
			e.printStackTrace();
		}
	}

	private JsonObject bootstrapReply(String requestId) {
		JsonObject j = new JsonObject();
		j.addProperty("requestId", requestId);
		return j;
	}

	@Override
	public void onStart() {

	}
}
