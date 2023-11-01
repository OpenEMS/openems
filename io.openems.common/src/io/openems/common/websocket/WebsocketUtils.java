package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

import com.google.gson.JsonObject;

public class WebsocketUtils {

	/**
	 * Converts a Handshake to a JsonObject.
	 * 
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive". Because of this fields are converted to
	 * lower-case.
	 *
	 * @param handshake the {@link Handshakedata}
	 * @return the converted {@link JsonObject}
	 */
	public static JsonObject handshakeToJsonObject(Handshakedata handshake) {
		var j = new JsonObject();
		for (var iter = handshake.iterateHttpFields(); iter.hasNext();) {
			var field = iter.next();
			j.addProperty(field.toLowerCase(), handshake.getFieldValue(field));
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
