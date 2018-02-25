package io.openems.common.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.StringUtils;

public class WebSocketUtils {

	private static Logger log = LoggerFactory.getLogger(WebSocketUtils.class);

	public static void sendOrLogError(Optional<WebSocket> websocketOpt, JsonObject j) {
		if (!websocketOpt.isPresent()) {
			log.error("Websocket is not available. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
		} else {
			sendOrLogError(websocketOpt.get(), j);
		}
	}

	public static void sendNotificationOrLogError(Optional<WebSocket> websocketOpt, JsonObject jMessageId,
			LogBehaviour logBehaviour, Notification code, Object... params) {
		if (!websocketOpt.isPresent()) {
			log.error("Websocket is not available. Unable to send Notification ["
					+ String.format(code.getMessage(), params) + "]");
		} else {
			WebSocketUtils.sendNotificationOrLogError(websocketOpt.get(), jMessageId, logBehaviour, code, params);
		}
	}

	public static void sendNotificationOrLogError(WebSocket websocket, JsonObject jMessageId, LogBehaviour logBehaviour,
			Notification notification, Object... params) {
		if (logBehaviour.equals(LogBehaviour.WRITE_TO_LOG)) {
			// log message
			notification.writeToLog(log, params);
		}
		String message = String.format(notification.getMessage(), params);
		JsonObject j = DefaultMessages.notification(jMessageId, notification, message, params);
		WebSocketUtils.sendOrLogError(websocket, j);
	}

	public static void send(Optional<WebSocket> websocketOpt, JsonObject j) throws OpenemsException {
		if (!websocketOpt.isPresent()) {
			throw new OpenemsException(
					"Websocket is not available. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
		} else {
			send(websocketOpt.get(), j);
		}
	}

	/**
	 * Send a message to a websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public static void send(WebSocket websocket, JsonObject j) throws OpenemsException {
		try {
			websocket.send(j.toString());
		} catch (WebsocketNotConnectedException e) {
			throw new OpenemsException(
					"Websocket is not connected. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
		}
	}

	/**
	 * Send a message to a websocket. If sending fails, it is logged as an error
	 *
	 * @param j
	 */
	public static void sendOrLogError(WebSocket websocket, JsonObject j) {
		try {
			WebSocketUtils.send(websocket, j);
		} catch (OpenemsException e) {
			log.error(e.getMessage());
		}
	}
}
