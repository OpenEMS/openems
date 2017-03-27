package io.openems.femsserver.browserwebsocket;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.OdooApiException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.femsserver.core.ConnectionManager;
import io.openems.femsserver.odoo.Odoo;
import io.openems.femsserver.odoo.fems.device.FemsDevice;
import io.openems.femsserver.utilities.JsonUtils;
import io.openems.femsserver.utilities.OpenemsException;
import io.openems.femsserver.utilities.WebSocketUtils;

/**
 * Handles connections from a browser (for FENECON Online-Monitoring).
 * Needs to be initialized before it can be used as a singleton.
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocket extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(BrowserWebsocket.class);

	private static BrowserWebsocket instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(int port) throws Exception {
		BrowserWebsocket ws = new BrowserWebsocket(port);
		ws.start();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized BrowserWebsocket getInstance() {
		return BrowserWebsocket.instance;
	}

	/**
	 * Holds a reference to the ConnectionManager singleton
	 */
	private final ConnectionManager connectionManager;

	/**
	 * Holds a reference to the Odoo singleton
	 */
	private final Odoo odoo;

	private BrowserWebsocket(int port) throws Exception {
		super(new InetSocketAddress(port));
		this.connectionManager = ConnectionManager.getInstance();
		this.odoo = Odoo.getInstance();
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
					JsonObject jOdoo = this.odoo.getFemsInfo(sessionId);
					// successfully logged in (otherwise an exception was thrown)
					JsonArray jOdooDeviceNames = JsonUtils.getAsJsonArray(jOdoo, "devices");
					List<FemsDevice> devices = this.odoo.getDevicesForNames(jOdooDeviceNames);
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

				} catch (OpenemsException | OdooApiException | XmlRpcException e) {
					// Authentication failed
					throw new OpenemsException(
							"Connection using session [" + sessionId + "] failed: " + e.getMessage());
				}
			} else {
				throw new OpenemsException("Connection failed. No session_id given.");
			}
		} catch (OpenemsException e) {
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
			JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
			if (jMessage.has("device")) {
				String deviceName = JsonUtils.getAsString(jMessage, "device");
				jMessage.remove("device");

				// Execute for every matching femsWebsocket (should be only one in general)
				this.connectionManager.getFemsWebsockets(deviceName).forEach(femsWebsocket -> {
					if (jMessage.has("subscribe")) {
						// forward subscribe message to fems websocket
						WebSocketUtils.send(femsWebsocket, jMessage);
					}
				});
			}
		} catch (OpenemsException e) {
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
}
