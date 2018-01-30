package io.openems.backend.browserwebsocket.api;

import java.util.Optional;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface BrowserWebsocketService {

	/**
	 * Announce browserWebsocket that this OpenEMS Edge was connected
	 * 
	 * @param deviceNames
	 */
	void openemsConnectionOpened(Set<String> deviceNames);

	Optional<WebSocket> getWebsocketByToken(String token);

}
