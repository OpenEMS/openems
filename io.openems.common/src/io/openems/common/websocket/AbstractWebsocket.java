package io.openems.common.websocket;

import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static io.openems.common.utils.StringUtils.toShortString;
import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.jsonrpc.base.JsonrpcMessage;

public abstract class AbstractWebsocket<T extends WsData> {

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocket.class);

	private final String name;

	/**
	 * Creates an empty WsData object that is attached to the given
	 * {@link WebSocket} as early as possible.
	 * 
	 * @param ws the {@link WebSocket}
	 * @return the typed {@link WsData}
	 */
	protected abstract T createWsData(WebSocket ws);

	/**
	 * Callback for internal error.
	 *
	 * @return the {@link OnInternalError} callback
	 */
	protected abstract OnInternalError getOnInternalError();

	/**
	 * Callback for websocket OnOpen event.
	 *
	 * @return the {@link OnOpen} callback
	 */
	protected abstract OnOpen getOnOpen();

	/**
	 * Callback for JSON-RPC request.
	 *
	 * @return the {@link OnRequest} callback
	 */
	protected abstract OnRequest getOnRequest();

	/**
	 * Callback for JSON-RPC notification.
	 *
	 * @return the {@link OnNotification} callback
	 */
	protected abstract OnNotification getOnNotification();

	/**
	 * Callback for websocket error.
	 *
	 * @return the {@link OnError} callback
	 */
	protected abstract OnError getOnError();

	/**
	 * Callback for websocket OnClose event.
	 *
	 * @return the {@link OnClose} callback
	 */
	protected abstract OnClose getOnClose();

	/**
	 * Construct this {@link AbstractWebsocket}.
	 *
	 * @param name a name that is used to identify log messages
	 */
	public AbstractWebsocket(String name) {
		this.name = name;
	}

	/**
	 * Gets the internal name of this websocket client/server.
	 *
	 * @return the internal name
	 */
	public String getName() {
		return this.name;
	}

	protected void start() {

	}

	protected void stop() {
	}

	/**
	 * Execute a {@link Runnable}.
	 *
	 * @param command the {@link Runnable}
	 */
	protected abstract void execute(Runnable command);

	/**
	 * Sends a {@link JsonrpcMessage} to the {@link WebSocket}. Returns true if
	 * sending was successful, otherwise false. Also logs a warning in that case.
	 *
	 * @param ws      the {@link WebSocket}
	 * @param message the {@link JsonrpcMessage}
	 * @return true if sending was successful
	 */
	protected final boolean sendMessage(WebSocket ws, JsonrpcMessage message) {
		if (!ws.isOpen()) {
			// Catch status before to avoid throwing an expensive
			// WebsocketNotConnectedException
			this.sendMessageFailedLog(ws, message);
			return false;
		}

		try {
			ws.send(message.toString());
			return true;

		} catch (WebsocketNotConnectedException e) {
			// Fallback for race condition if Connection was closed inbetween
			this.sendMessageFailedLog(ws, message);
			return false;
		}
	}

	private void sendMessageFailedLog(WebSocket ws, JsonrpcMessage message) {
		this.logWarn(this.log, new StringBuilder() //
				.append("[").append(generateWsDataString(ws)) //
				.append("] Unable to send message: Connection is closed. ") //
				.append(toShortString(simplifyJsonrpcMessage(message), 100)) //
				.toString());
	}

	/**
	 * Handles an internal Error synchronously.
	 *
	 * @param t            the {@link Throwable} to be handled
	 * @param wsDataString the toString() content of the WsData attachment of the
	 *                     WebSocket
	 */
	protected void handleInternalError(Throwable t, String wsDataString) {
		this.getOnInternalError().accept(t, wsDataString);
	}

	/**
	 * Log a info message.
	 *
	 * @param log     a Logger instance
	 * @param message the message
	 */
	protected abstract void logInfo(Logger log, String message);

	/**
	 * Log a warn message.
	 *
	 * @param log     a Logger instance
	 * @param message the message
	 */
	protected abstract void logWarn(Logger log, String message);

	/**
	 * Log a error message.
	 *
	 * @param log     a Logger instance
	 * @param message the message
	 */
	protected abstract void logError(Logger log, String message);

}
