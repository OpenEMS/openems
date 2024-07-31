package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

public class WebsocketUtils {

	/**
	 * Gets a String value from a {@link Handshakedata}.
	 * 
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshakedata the {@link Handshakedata}
	 * @param fieldName     the name of the field
	 * @return the field value; or null
	 */
	public static String getAsString(Handshakedata handshakedata, String fieldName) {
		for (var iter = handshakedata.iterateHttpFields(); iter.hasNext();) {
			var field = iter.next();
			if (fieldName.equalsIgnoreCase(field)) {
				return handshakedata.getFieldValue(field).trim();
			}
		}
		return null;
	}

	private static final String[] REMOTE_IDENTIFICATION_HEADERS = new String[] { //
			"Forwarded", "X-Forwarded-For", "X-Real-IP" };

	/**
	 * Parses a identifier for the Remote from the {@link Handshakedata}.
	 * 
	 * <p>
	 * Tries to use the headers "Forwarded", "X-Forwarded-For" or "X-Real-IP". Falls
	 * back to `ws.getRemoteSocketAddress()`. See https://serverfault.com/a/920060
	 * 
	 * @param ws            the {@link WebSocket}
	 * @param handshakedata the {@link Handshakedata}
	 * @return an identifier String
	 */
	public static String parseRemoteIdentifier(WebSocket ws, Handshakedata handshakedata) {
		for (var key : REMOTE_IDENTIFICATION_HEADERS) {
			var value = getAsString(handshakedata, key);
			if (value != null) {
				return value;
			}
		}
		// fallback
		return ws.getRemoteSocketAddress().toString();
	}

	/**
	 * Gets the toString() content of the WsData attachment of the WebSocket; or
	 * empty string if not available.
	 *
	 * @param ws the WebSocket
	 * @return the {@link WsData#toString()} content
	 */
	public static String generateWsDataString(WebSocket ws) {
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
