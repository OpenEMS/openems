package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import com.google.gson.JsonObject;

public class WebSocketUtils {

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

	public static boolean sendAsDevice(WebSocket websocket, JsonObject j, int fems) {
		j.addProperty("device", "fems" + fems);
		return send(websocket, j);
	}
}
