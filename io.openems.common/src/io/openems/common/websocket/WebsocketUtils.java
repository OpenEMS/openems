package io.openems.common.websocket;

import java.util.Iterator;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

import com.google.gson.JsonObject;

public class WebsocketUtils {

	/**
	 * Converts a Handshake to a JsonObject
	 * 
	 * @param handshake
	 * @return
	 */
	public static JsonObject handshakeToJsonObject(Handshakedata handshake) {
		JsonObject j = new JsonObject();
		for (Iterator<String> iter = handshake.iterateHttpFields(); iter.hasNext();) {
			String field = iter.next();
			j.addProperty(field, handshake.getFieldValue(field));
		}
		return j;
	}

	/**
	 * Gets the toString() content of the WsData attachment of the WebSocket; or
	 * empty string if not available.
	 * 
	 * @param ws the WebSocket
	 * @return the {@link WsData#toString()} content
	 */
	public static String getWsDataString(WebSocket ws) {
		if (ws == null) {
			return "";
		}
		WsData wsData = ws.getAttachment();
		if (wsData == null) {
			return "";
		}
		return wsData.toString();
	}

}
