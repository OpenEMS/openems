package io.openems.backend.edgewebsocket.impl.provider;

import java.util.Optional;

import org.java_websocket.handshake.ClientHandshake;

public class Utils {

	private Utils() {
		
	}
	

	/**
	 * Parses the apikey from websocket onOpen handshake
	 *
	 * @param handshake
	 * @return
	 */
	protected static Optional<String> parseApikeyFromHandshake(ClientHandshake handshake) {
		if (handshake.hasFieldValue("apikey")) {
			String apikey = handshake.getFieldValue("apikey");
			return Optional.ofNullable(apikey);
		}
		return Optional.empty();
	}
}
