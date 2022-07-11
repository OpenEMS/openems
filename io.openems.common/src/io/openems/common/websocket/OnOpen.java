package io.openems.common.websocket;

import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
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
	 * <p>
	 * Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * all variants of 'cookie' are accepted.
	 *
	 * @param handshake the Handshake
	 * @param fieldname the field name
	 * @return value as optional
	 */
	public static Optional<String> getFieldFromHandshakeCookie(JsonObject handshake, String fieldname) {
		for (Entry<String, JsonElement> entry : handshake.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("cookie")) {
				var cookieOpt = JsonUtils.getAsOptionalString(entry.getValue());
				if (cookieOpt.isPresent()) {
					for (String cookieVariable : cookieOpt.get().split("; ")) {
						var keyValue = cookieVariable.split("=");
						if (keyValue.length == 2) {
							if (keyValue[0].equals(fieldname)) {
								return Optional.ofNullable(keyValue[1]);
							}
						}
					}
				}
			}
		}
		return Optional.empty();
	}
}
