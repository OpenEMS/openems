package io.openems.common.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.utils.StringUtils;

public class WebSocketUtils {

	private static Logger log = LoggerFactory.getLogger(WebSocketUtils.class);

	public static boolean send(Optional<WebSocket> websocketOpt, JsonObject j) {
		if (!websocketOpt.isPresent()) {
			log.error("Websocket is not available. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
			return false;
		} else {
			return WebSocketUtils.send(websocketOpt.get(), j);
		}
	}

	public static boolean sendNotification(Optional<WebSocket> websocketOpt, JsonArray jId, LogBehaviour logBehaviour,
			Notification code, Object... params) {
		if (!websocketOpt.isPresent()) {
			log.error("Websocket is not available. Unable to send Notification ["
					+ String.format(code.getMessage(), params) + "]");
			return false;
		} else {
			return WebSocketUtils.sendNotification(websocketOpt.get(), jId, logBehaviour, code, params);
		}
	}

	public static boolean sendNotification(WebSocket websocket, JsonArray jId, LogBehaviour logBehaviour,
			Notification notification, Object... params) {
		if (logBehaviour.equals(LogBehaviour.WRITE_TO_LOG)) {
			// log message
			notification.writeToLog(log, params);
		}
		String message = String.format(notification.getMessage(), params);
		JsonObject j = DefaultMessages.notification(jId, notification, message, params);
		return WebSocketUtils.send(websocket, j);
	}

	/**
	 * Send a message to a websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public static boolean send(WebSocket websocket, JsonObject j) {
		// System.out.println("SEND: websocket["+websocket+"]: " + j.toString());
		try {
			websocket.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			log.error("Websocket is not connected. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
			return false;
		}
	}
}
