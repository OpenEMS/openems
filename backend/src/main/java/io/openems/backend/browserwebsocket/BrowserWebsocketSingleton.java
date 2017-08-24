package io.openems.backend.browserwebsocket;

import java.net.InetSocketAddress;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.browserwebsocket.session.BrowserSessionManager;
import io.openems.backend.core.ConnectionManager;
import io.openems.backend.influx.Influxdb;
import io.openems.backend.odoo.Odoo;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.backend.openemswebsocket.OpenemsWebsocketSingleton;
import io.openems.backend.utilities.StringUtils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.Device;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections from a browser.
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocketSingleton extends WebSocketServer {

	private final Logger log = LoggerFactory.getLogger(BrowserWebsocketSingleton.class);

	private final BiMap<WebSocket, BrowserSession> websockets = Maps.synchronizedBiMap(HashBiMap.create());
	private final BrowserSessionManager sessionManager = new BrowserSessionManager();

	protected BrowserWebsocketSingleton(int port) throws Exception {
		super(new InetSocketAddress(port));
	}

	/**
	 * Open event of websocket. Parses the Odoo "session_id" and stores it in a new Session.
	 */
	@Override
	public void onOpen(WebSocket websocket, ClientHandshake handshake) {
		try {
			// Prepare session information
			BrowserSession session = null;
			JsonObject jCookie = parseCookieFromHandshake(handshake);
			String sessionId;
			sessionId = JsonUtils.getAsString(jCookie, "session_id");

			// try to get token of an existing, valid session from cookie
			if (jCookie.has("token")) {
				String token = JsonUtils.getAsString(jCookie, "token");
				Optional<BrowserSession> existingSessionOpt = sessionManager.getSessionByToken(token);
				if (existingSessionOpt.isPresent()) {
					BrowserSession existingSession = existingSessionOpt.get();
					// test if it is the same Odoo session_id
					if (Optional.ofNullable(sessionId).equals(existingSession.getData().getOdooSessionId())) {
						session = existingSession;
					}
				}
			}

			// create new session if no existing one was found
			if (session == null) {
				BrowserSessionData sessionData = new BrowserSessionData();
				sessionData.setOdooSessionId(sessionId);
				session = sessionManager.createNewSession(sessionData);
			}

			// check Odoo session and refresh info from Odoo
			Odoo.instance().getInfoWithSession(session);

			// check if the session is now valid and send reply to browser
			BrowserSessionData data = session.getData();
			if (session.isValid()) {
				// add isOnline information
				OpenemsWebsocketSingleton openemsWebsocket = OpenemsWebsocket.instance();
				for (Device device : data.getDevices()) {
					device.setOnline(openemsWebsocket.isDeviceOnline(device.getName()));
				}

				// send connection successful to browser
				JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply(session.getToken(),
						data.getDevices());
				log.info("Browser connected. User [" + data.getUserId().orElse(0) + "] Session ["
						+ data.getOdooSessionId().orElse("") + "]");
				WebSocketUtils.send(websocket, jReply);

				// add websocket to local cache
				this.websockets.forcePut(websocket, session);

			} else {
				// send connection failed to browser
				JsonObject jReply = DefaultMessages.browserConnectionFailedReply();
				WebSocketUtils.send(websocket, jReply);
				log.info("Browser connection failed. User [" + data.getUserId() + "] Session ["
						+ data.getOdooSessionId() + "]");

				// close websocket
				websocket.close();
			}

		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Close event of websocket. Removes the websocket.
	 */
	@Override
	public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
		BrowserSession session = this.websockets.get(websocket);
		if (session != null) {
			log.info("Browser connection closed. User [" + session.getData().getUserId() + "] Session ["
					+ session.getData().getOdooSessionId() + "]" + " Code [" + code + "] Reason [" + reason + "]");
		} else {
			log.info("Browser connection closed. Code [" + code + "] Reason [" + reason + "]");
		}
		this.websockets.remove(websocket);
	}

	/**
	 * Error event of websocket. Logs the error.
	 */
	@Override
	public void onError(WebSocket websocket, Exception ex) {
		BrowserSession session = this.websockets.get(websocket);
		if (session != null) {
			log.warn("Browser connection error. User [" + session.getData().getUserId() + "] Session ["
					+ session.getData().getOdooSessionId() + "]: " + ex.getMessage());
		} else {
			log.warn("Browser connection error: " + ex.getMessage());
		}
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	public void onMessage(WebSocket websocket, String message) {
		log.info(message);
		String requestId = "";
		JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();

		/*
		 * // * Try to get a Session from authentication message
		 * //
		 */
		// if (jMessage.has("authenticate")) {
		// Optional<Session> existingSessionOpt = authenticate(jMessage.get("authenticate"));
		// if (existingSessionOpt.isPresent()) {
		// Session existingSession = existingSessionOpt.get();
		// // if the session is still connected to another websocket, close that old websocket
		// WebSocket existingWebsocket = this.websockets.inverse().get(existingSession);
		// if (existingWebsocket != null) {
		// this.websockets.remove(existingWebsocket);
		// }
		// // replace the session that was created in onOpen with the existing session
		// this.websockets.forcePut(websocket, existingSession);
		// }
		// }

		/*
		 * Check validity of Session
		 */
		//
		// if (jMessage.has("device")) {
		// String deviceName = JsonUtils.getAsString(jMessage, "device");
		// jMessage.remove("device");
		//
		// if (jMessage.has("requestId")) {
		// try {
		// requestId = JsonUtils.getAsString(jMessage, "requestId");
		// } catch (Exception e) {
		// log.warn("Invalid requestId: " + e.getMessage());
		// }
		// }
		//
		// /*
		// * Register interconnection to OpenEMS
		// */
		// if (jMessage.has("connect")) {
		// connect(websocket, deviceName, jMessage.get("connect"));
		// }
		//
		// /*
		// * Forward Subscribe to data
		// */
		// if (jMessage.has("subscribe")) {
		// subscribe(deviceName, jMessage.get("subscribe"));
		// }
		//
		// /*
		// * Forward System command
		// */
		// if (jMessage.has("system")) {
		// system(deviceName, jMessage.get("system"));
		// }
		//
		// /*
		// * Query command
		// */
		// if (jMessage.has("query")) {
		// query(requestId, deviceName, websocket, jMessage.get("query"));
		// }
		// }
	}

	/**
	 * Get cookie from handshake
	 *
	 * @param handshake
	 * @return cookie as JsonObject
	 */
	private JsonObject parseCookieFromHandshake(ClientHandshake handshake) {
		JsonObject j = new JsonObject();
		if (handshake.hasFieldValue("cookie")) {
			String cookieString = handshake.getFieldValue("cookie");
			for (String cookieVariable : cookieString.split("; ")) {
				String[] keyValue = cookieVariable.split("=");
				if (keyValue.length == 2) {
					j.addProperty(keyValue[0], keyValue[1]);
				}
			}
		}
		return j;
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
	 * Handle connect: interconnects browser and OpenEMS websocket
	 */
	private synchronized void connect(WebSocket browserWebsocket, String deviceName, JsonElement jConnectElement) {
		boolean connect;
		try {
			connect = JsonUtils.getAsBoolean(jConnectElement);
		} catch (OpenemsException e) {
			log.warn("Unable to parse connect [" + jConnectElement + "]: " + e.getMessage());
			return;
			// TODO send notification
		}
		Optional<WebSocket> openemsWebsocketOpt = ConnectionManager.instance()
				.getOpenemsWebsocketFromDeviceName(deviceName);
		if (!openemsWebsocketOpt.isPresent()) {
			log.warn("Trying to connect [" + connect + "] with [" + deviceName + "], but it is not online");
			return;
			// TODO send notification
		}
		WebSocket openemsWebsocket = openemsWebsocketOpt.get();
		if (connect) {
			ConnectionManager.instance().addWebsocketInterconnection(browserWebsocket, openemsWebsocket);
		} else {
			ConnectionManager.instance().removeWebsocketInterconnection(browserWebsocket, openemsWebsocket);
		}
	}

	/**
	 * Handle subscriptions
	 */
	private synchronized void subscribe(String deviceName, JsonElement jSubscribeElement) {
		JsonObject j = new JsonObject();
		j.add("subscribe", jSubscribeElement);
		Optional<WebSocket> openemsWebsocketOpt = ConnectionManager.instance()
				.getOpenemsWebsocketFromDeviceName(deviceName);
		if (!openemsWebsocketOpt.isPresent()) {
			log.warn("Trying to forward subscribe to [" + deviceName + "], but it is not online");
		}
		WebSocket openemsWebsocket = openemsWebsocketOpt.get();
		log.info(deviceName + ": forward subscribe to OpenEMS " + StringUtils.toShortString(j, 100));
		// TODO: subscribes should be handled here instead of at OpenEMS to avoid unsubscribing somebody else
		WebSocketUtils.send(openemsWebsocket, j);
	}

	/**
	 * System command
	 *
	 * @param j
	 */
	private synchronized void system(String deviceName, JsonElement jSubscribeElement) {
		JsonObject j = new JsonObject();
		j.add("system", jSubscribeElement);
		Optional<WebSocket> openemsWebsocketOpt = ConnectionManager.instance()
				.getOpenemsWebsocketFromDeviceName(deviceName);
		if (!openemsWebsocketOpt.isPresent()) {
			log.warn("Trying to forward system call to [" + deviceName + "], but it is not online");
		}
		WebSocket openemsWebsocket = openemsWebsocketOpt.get();
		log.info(deviceName + ": forward system call to OpenEMS " + StringUtils.toShortString(j, 100));
		WebSocketUtils.send(openemsWebsocket, j);
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
				JsonObject jQueryreply = Influxdb.instance().query(fems, fromDate, toDate, channels, resolution/*
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
