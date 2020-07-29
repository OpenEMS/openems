package io.openems.common.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

@FunctionalInterface
public interface OnOpen {

	/**
	 * Handles OnOpen event of WebSocket.
	 * 
	 * @param ws        the WebSocket
	 * @param handshake the HTTP handshake/headers
	 * @throws OpenemsNamedException on error
	 */
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsNamedException;

	/**
	 * Get field from the 'cookie' field in the handshake.
	 *
	 * @param handshake the Handshake
	 * @param fieldname the field name
	 * @return value as optional
	 */
	public static Optional<String> getFieldFromHandshakeCookie(JsonObject handshake, String fieldname) {
		Optional<String> cookieOpt = JsonUtils.getAsOptionalString(handshake, "Cookie");
		if (!cookieOpt.isPresent()) {
			cookieOpt = JsonUtils.getAsOptionalString(handshake, "cookie");
			if (!cookieOpt.isPresent()) {
				return Optional.empty();
			}
		}
		String cookie = cookieOpt.get();
		for (String cookieVariable : cookie.split("; ")) {
			String[] keyValue = cookieVariable.split("=");
			if (keyValue.length == 2) {
				if (keyValue[0].equals(fieldname)) {
					return Optional.ofNullable(keyValue[1]);
				}
			}
		}
		return Optional.empty();
	}
}
