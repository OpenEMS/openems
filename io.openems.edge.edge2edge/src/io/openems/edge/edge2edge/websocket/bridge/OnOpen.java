package io.openems.edge.edge2edge.websocket.bridge;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.websocket.Handshake;
import io.openems.common.websocket.WebSocket;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Consumer<ConnectionState> onStateChange;

	public OnOpen(Consumer<ConnectionState> onStateChange) {
		super();
		this.onStateChange = onStateChange;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshake handshakedata) {
		this.onStateChange.accept(ConnectionState.AUTHENTICATING);
		return null;
	}

}
