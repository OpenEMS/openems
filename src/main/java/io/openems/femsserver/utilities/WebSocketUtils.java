package io.openems.femsserver.utilities;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class WebSocketUtils {

	private static Logger log = LoggerFactory.getLogger(WebSocketUtils.class);

	/**
	 * Send a message to a websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public static boolean send(WebSocket websocket, JsonObject j) {
		try {
			// log.info("Send: " + j.toString());
			websocket.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			return false;
		}
	}
}
