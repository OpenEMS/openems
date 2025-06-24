package io.openems.edge.edge2edge.websocket.bridge;

/**
 * NOT_CONNECTED -> CONNECTING -> AUTHENTICATING -> CONNECTED.
 */
public enum ConnectionState {
	NOT_CONNECTED(ConnectionStateOption.NOT_CONNECTED), //
	CONNECTING(ConnectionStateOption.CONNECTING), //
	AUTHENTICATING(ConnectionStateOption.AUTHENTICATING), //
	CONNECTED(ConnectionStateOption.CONNECTED), //
	;

	public final ConnectionStateOption option;

	private ConnectionState(ConnectionStateOption option) {
		this.option = option;
	}

}