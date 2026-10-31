package io.openems.common.websocket;

import java.util.Optional;
import java.util.UUID;


public class WebsocketUtils {

	/**
	 * Gets a String value from a {@link Handshake}.
	 * 
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshake the {@link Handshake}
	 * @param fieldName     the name of the field
	 * @return the field value; or null
	 */
	public static String getAsString(Handshake handshake, String fieldName) {
		return getAsOptionalString(handshake, fieldName).orElse(null);
	}

	/**
	 * Gets a String value from a {@link Handshake}.
	 *
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshake the {@link Handshake}
	 * @param fieldName     the name of the field
	 * @return the field value as optional; empty if not found
	 */
	public static Optional<String> getAsOptionalString(Handshake handshake, String fieldName) {
		return handshake.getHeader(fieldName).map(String::trim);
	}

	/**
	 * Gets a String value from a {@link Handshake}.
	 *
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshake the {@link Handshake}
	 * @param header        the header to search for
	 * @return the field value as optional; empty if not found
	 */
	public static Optional<String> getAsOptionalString(Handshake handshake, CommonHttpHeader header) {
		return getAsOptionalString(handshake, header.asString());
	}

	/**
	 * Gets a UUID value from a {@link Handshake}.
	 *
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshake the {@link Handshake}
	 * @param header        the header to search for
	 * @return the field value as optional; empty if not found or not a valid UUID
	 */
	public static Optional<UUID> getAsOptionalUuid(Handshake handshake, CommonHttpHeader header) {
		return getAsOptionalString(handshake, header) //
				.map((raw) -> {
					try {
						return UUID.fromString(raw);
					} catch (IllegalArgumentException e) {
						return null;
					}
				});
	}

	private static final String[] REMOTE_IDENTIFICATION_HEADERS = new String[] { //
			"Forwarded", "X-Forwarded-For", "X-Real-IP" };

	/**
	 * Parses a identifier for the Remote from the {@link Handshake}.
	 * 
	 * <p>
	 * Tries to use the headers "Forwarded", "X-Forwarded-For" or "X-Real-IP". Falls
	 * back to `ws.getRemoteSocketAddress()`. See https://serverfault.com/a/920060
	 * 
	 * @param ws            the {@link WebSocket}
	 * @param handshake the {@link Handshake}
	 * @return an identifier String
	 */
	public static String parseRemoteIdentifier(WebSocket ws, Handshake handshake) {
		for (var key : REMOTE_IDENTIFICATION_HEADERS) {
			var value = getAsString(handshake, key);
			if (value != null) {
				return value;
			}
		}
		// fallback
		return String.valueOf(ws.getRemoteSocketAddress());
	}

	/**
	 * Gets the toLogString() content of the WsData attachment of the WebSocket; or
	 * empty string if not available.
	 *
	 * @param ws the WebSocket
	 * @return the {@link WsData#toLogString()} content
	 */
	public static String generateWsDataString(WebSocket ws) {
		if (ws == null) {
			return "";
		}
		WsData wsData = ws.getAttachment();
		if (wsData == null) {
			return "";
		}
		var logString = wsData.toLogString();
		if (logString == null) {
			return "";
		}
		return logString;
	}
}
