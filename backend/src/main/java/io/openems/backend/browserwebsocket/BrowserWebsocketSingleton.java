package io.openems.backend.browserwebsocket;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.browserwebsocket.session.BrowserSessionManager;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.backend.openemswebsocket.OpenemsWebsocketSingleton;
import io.openems.backend.timedata.Timedata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.Device;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections from a browser.
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocketSingleton extends AbstractWebsocketServer<BrowserSession, BrowserSessionManager> {

	protected BrowserWebsocketSingleton(int port) throws Exception {
		super(port, new BrowserSessionManager());
	}

	/**
	 * Open event of websocket. Parses the Odoo "session_id" and stores it in a new Session.
	 */
	@Override
	public void onOpen(WebSocket websocket, ClientHandshake handshake) {
		// Prepare session information
		String error = "";
		BrowserSession session = null;
		String sessionId = null;

		try {
			// get cookie information
			JsonObject jCookie = parseCookieFromHandshake(handshake);
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
		} catch (OpenemsException e) {
			error = e.getMessage();
		}

		// create new session if no existing one was found
		if (session == null) {
			BrowserSessionData sessionData = new BrowserSessionData();
			sessionData.setOdooSessionId(sessionId);
			session = sessionManager.createNewSession(sessionData);
		}

		// check Odoo session and refresh info from Odoo
		try {
			Metadata.instance().getInfoWithSession(session);
		} catch (OpenemsException e) {
			error = e.getMessage();
		}

		// check if the session is now valid and send reply to browser
		BrowserSessionData data = session.getData();
		if (error.isEmpty() && session.isValid()) {
			// add isOnline information
			OpenemsWebsocketSingleton openemsWebsocket = OpenemsWebsocket.instance();
			for (Device device : data.getDevices()) {
				device.setOnline(openemsWebsocket.isOpenemsWebsocketConnected(device.getName()));
			}

			// send connection successful to browser
			JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply(session.getToken(), Optional.empty(),
					data.getDevices());
			log.info("Browser connected. User [" + data.getUserId().orElse(-1) + "] Session ["
					+ data.getOdooSessionId().orElse("") + "]");
			WebSocketUtils.send(websocket, jReply);

			// add websocket to local cache
			this.websockets.forcePut(websocket, session);

		} else {
			// send connection failed to browser
			JsonObject jReply = DefaultMessages.browserConnectionFailedReply();
			WebSocketUtils.send(websocket, jReply);
			log.info("Browser connection failed. User [" + data.getUserId().orElse(-1) + "] Session ["
					+ data.getOdooSessionId().orElse("") + "] Error [" + error + "]");

			websocket.closeConnection(CloseFrame.REFUSE, error);
		}
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	protected void onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		/*
		 * With existing device name
		 */
		if (deviceNameOpt.isPresent()) {
			String deviceName = deviceNameOpt.get();
			/*
			 * Query historic data
			 */
			if (jMessage.has("historicData")) {
				historicData(jMessageIdOpt, deviceName, websocket, jMessage.get("historicData"));
			}

			/*
			 * Forward to OpenEMS Edge
			 */
			if (jMessage.has("config") || jMessage.has("currentData") || jMessage.has("log")) {
				try {
					forwardMessageToOpenems(websocket, jMessage, deviceName);
				} catch (OpenemsException e) {
					WebSocketUtils.send(websocket, DefaultMessages.notification(Notification.EDGE_UNABLE_TO_FORWARD,
							deviceName, e.getMessage()));
					log.error(deviceName + ": Unable to forward message: " + e.getMessage());
				}
			}
		}
		// TODO system command
		// /*
		// * Forward System command
		// */
		// if (jMessage.has("system")) {
		// system(deviceName, jMessage.get("system"));
		// }
	}

	/**
	 * Forward message to OpenEMS websocket.
	 *
	 * @throws OpenemsException
	 */
	private void forwardMessageToOpenems(WebSocket websocket, JsonObject jMessage, String deviceName)
			throws OpenemsException {
		// remove device from message
		if (jMessage.has("device")) {
			jMessage.remove("device");
		}

		// add session token to message id for identification
		BrowserSession session = this.websockets.get(websocket);
		JsonArray jId;
		if (jMessage.has("id")) {
			jId = JsonUtils.getAsJsonArray(jMessage, "id");
		} else {
			jId = new JsonArray();
		}
		jId.add(session.getToken());
		jMessage.add("id", jId);

		// get OpenEMS websocket and forward message
		Optional<WebSocket> openemsWebsocketOpt = OpenemsWebsocket.instance().getOpenemsWebsocket(deviceName);
		if (openemsWebsocketOpt.isPresent()) {
			WebSocket openemsWebsocket = openemsWebsocketOpt.get();
			if (WebSocketUtils.send(openemsWebsocket, jMessage)) {
				return;
			} else {
				throw new OpenemsException("Sending failed");
			}
		} else {
			throw new OpenemsException("Device is not connected.");
		}
	}

	// TODO notification handling
	// /**
	// * Generates a generic notification message
	// *
	// * @param message
	// * @return
	// */
	// private JsonObject generateNotification(String message) {
	// JsonObject j = new JsonObject();
	// JsonObject jNotification = new JsonObject();
	// jNotification.addProperty("message", message);
	// j.add("notification", jNotification);
	// return j;
	// }

	// TODO system command
	// /**
	// * System command
	// *
	// * @param j
	// */
	// private synchronized void system(String deviceName, JsonElement jSubscribeElement) {
	// JsonObject j = new JsonObject();
	// j.add("system", jSubscribeElement);
	// Optional<WebSocket> openemsWebsocketOpt = ConnectionManager.instance()
	// .getOpenemsWebsocketFromDeviceName(deviceName);
	// if (!openemsWebsocketOpt.isPresent()) {
	// log.warn("Trying to forward system call to [" + deviceName + "], but it is not online");
	// }
	// WebSocket openemsWebsocket = openemsWebsocketOpt.get();
	// log.info(deviceName + ": forward system call to OpenEMS " + StringUtils.toShortString(j, 100));
	// WebSocketUtils.send(openemsWebsocket, j);
	// }

	/**
	 * Query command
	 *
	 * @param j
	 */
	private synchronized void historicData(Optional<JsonArray> jMessageIdOpt, String deviceName, WebSocket websocket,
			JsonElement jHistoricDataElement) {
		try {
			JsonObject jHistoricData = JsonUtils.getAsJsonObject(jHistoricDataElement);
			String mode = JsonUtils.getAsString(jHistoricData, "mode");
			if (mode.equals("query")) {
				JsonArray jMessageId = jMessageIdOpt.get();
				/*
				 * Query historic data
				 */
				Matcher matcher = Pattern.compile("\\d+").matcher(deviceName); // extracts '0' from 'openems0'
				matcher.find();
				int deviceId = Integer.valueOf(matcher.group());
				int timezoneDiff = JsonUtils.getAsInt(jHistoricData, "timezone");
				ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
				ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(jHistoricData, "fromDate", timezone);
				ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(jHistoricData, "toDate", timezone).plusDays(1);
				JsonObject channels = JsonUtils.getAsJsonObject(jHistoricData, "channels");
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
				JsonArray jData = Timedata.instance().queryHistoricData(deviceId, fromDate, toDate, channels,
						resolution);
				// send reply
				JsonObject j = DefaultMessages.historicDataQueryReply(jMessageId, jData);
				WebSocketUtils.send(websocket, j);
			}
		} catch (Exception e) {
			log.error("Error", e);
			e.printStackTrace();
		}
	}

	/**
	 * OpenEMS Websocket tells us, when the connection to an OpenEMS Edge is closed
	 *
	 * @param name
	 */
	public void openemsConnectionClosed(String name) {
		for (BrowserSession session : this.sessionManager.getSessions()) {
			for (Device device : session.getData().getDevices()) {
				if (name.equals(device.getName())) {
					WebSocket ws = this.websockets.inverse().get(session);
					JsonObject j = DefaultMessages.notification(Notification.EDGE_CONNECTION_ClOSED, name);
					WebSocketUtils.send(ws, j);
				}
			}
		}
	}

	/**
	 * OpenEMS Websocket tells us, when the connection to an OpenEMS Edge is openend
	 *
	 * @param name
	 */
	public void openemsConnectionOpened(String name) {
		for (BrowserSession session : this.sessionManager.getSessions()) {
			for (Device device : session.getData().getDevices()) {
				if (name.equals(device.getName())) {
					WebSocket ws = this.websockets.inverse().get(session);
					JsonObject j = DefaultMessages.notification(Notification.EDGE_CONNECTION_OPENED, name);
					WebSocketUtils.send(ws, j);
				}
			}
		}
	}
}
