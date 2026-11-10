package io.openems.common.websocket;

import java.net.InetSocketAddress;

/**
 * A WebSocket connection of an {@link AbstractWebsocketServer} or
 * {@link AbstractWebsocketClient}.
 */
public interface WebSocket {

	/**
	 * Gets the attachment of this connection, typically a {@link WsData}.
	 *
	 * @param <T> the type of the attachment
	 * @return the attachment; possibly null
	 */
	public <T> T getAttachment();

	/**
	 * Sets the attachment of this connection.
	 *
	 * @param <T>        the type of the attachment
	 * @param attachment the attachment
	 */
	public <T> void setAttachment(T attachment);

	/**
	 * Sends a text message. Messages to a closed connection are silently dropped.
	 *
	 * @param text the text message
	 */
	public void send(String text);

	/**
	 * Is the connection open?.
	 *
	 * @return true if messages can be sent
	 */
	public boolean isOpen();

	/**
	 * Closes the connection with {@link CloseCode#NORMAL}.
	 */
	public void close();

	/**
	 * Closes the connection.
	 *
	 * @param code   the close code, see {@link CloseCode}
	 * @param reason the close reason
	 */
	public void close(int code, String reason);

	/**
	 * Gets the address of the remote endpoint.
	 *
	 * @return the address; null if not connected
	 */
	public InetSocketAddress getRemoteSocketAddress();

}
