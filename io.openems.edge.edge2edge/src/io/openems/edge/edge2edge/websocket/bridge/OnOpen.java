package io.openems.edge.edge2edge.websocket.bridge;

import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Consumer<ConnectionState> onStateChange;

	public OnOpen(Consumer<ConnectionState> onStateChange) {
		super();
		this.onStateChange = onStateChange;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		this.onStateChange.accept(ConnectionState.AUTHENTICATING);
		return null;
	}

}
